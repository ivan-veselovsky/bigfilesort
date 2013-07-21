package edu.bigfilesort;

import static java.lang.System.out;

import java.io.File;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.bigfilesort.parallel.Exec;
import edu.bigfilesort.parallel.TaskPlanner;
import edu.bigfilesort.parallel.TaskPlannerImpl;
import edu.bigfilesort.radix.RadixPlannerImpl;

public class Main {

  /** 
   * Length of single data segment, in bytes.
   */
   public static final int dataLength = 4; // bytes
   /**
    * Used to simplify multiplication of the byte-based indexes: 
    */
   public static final int log2DataLength = 2; // Util.log2(dataLength); let it be compile-time const
   
   public static final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
   
   // how many ints may be allocated in native buffers (no matter mapped or direct)
   // note that -XX:MaxDirectMemorySize=... might be needed to use direct buffers in desired extent.  
   private long maxAllocNumbers = 1024L * 1024 * 128; // 128 may be okay by default (512m). 32 also tested.  
   
   // ----------------------------------------------------------------
   
   public static final long megaByte = 1024L * 1024L; // auxiliary
   
   public static final boolean debug = false; // diagnostic output

   public static final boolean countersEnabled = false; // counters in sort algorithms
   
   boolean radix = true; // use radix parallel implementation (default is true)  

   /*
    * On CentOS-64 the performance is nearly the same, though, Direct are faster by ~8%.
    * So, to achieve best performance its recommended to use Direct mode.
    * 
    * !! Note that in-place comparison sorting engine uses *mapped* memory independently on this option value.
    * This value affects merging and radix rewriting routines.  
    */
   public static boolean readWriteProvidersMapped = false; // 'true' to use "Mapped", 'false' to use "Direct".   
   
   // ----------------------------------------------------------------
   
   private String fileName;
   private long fileLength;
   private int threadCount;

   /**
    * Program entry point.
    * 
    * @param args
    */
   public static void main(String[] args) throws Exception {
      int result = new Main().mainImpl(args);
      System.exit(result);
   }

   Main() {
   }

   int mainImpl(String... args) throws Exception {
      int status = setParameters(args);
      if (status != 0) {
         return status;
      }

      final long t = System.currentTimeMillis();
      int result = runImpl();
      long delta = System.currentTimeMillis() - t;
      //if (debug) { 
      out.println("============= Sorting took: " + delta + " ms"); 
      //}
      return result;
   }

   public static long megabytesToInts(long mb) {
     return mb << (20 - log2DataLength);
   }
   
   public static long intsToMegabytes(long ints/*4-byte numbers*/) {
     return ints >> (20 - log2DataLength);
   }
   
   private int setParameters(final String[] args0) throws Exception {
     assert Util.assertionsEnabled(); // print out if the assertions are enabled. This is important for performance measurements.
     final List<String> argList = new ArrayList<String>(Arrays.asList(args0)); 
      if (argList.size() < 2) {
         out.println("Parameters: [-m] [-c] <file name> <thread count> [max allowed native memory in mega bytes]");
         out.println("(Default for the last parameter is " + intsToMegabytes(maxAllocNumbers) + " Mb.)");
         out.println(" \"-m\" option instructs to use mapped buffers instead of direct in merging and radix rewriting tasks.");
         out.println(" \"-c\" option instructs to use in-place comparison sorting with subsequent merging instead of radix sorting.");
         out.println("Example:");
         out.println(" ./bigfilesort.sh test-putina-naxuy.data 7 1024");
         out.println("sorts the file using 7 threads with 1024 megabytes of total allowed native memory (both direct + mapped).");
         out.println("(Note that JVM parameter -XX:MaxDirectMemorySize=... may need to be adjusted accordingly.)");
         return 3;
      }

      // options:
      String opt;
      while (argList.size() > 0) {
        opt = argList.get(0);
        if ("-m".equals(opt)) {
          readWriteProvidersMapped = true;
        } else if ("-c".equals(opt)) {
          radix = false;
        } else {
          break;
        }
        argList.remove(0);
      }
      out.println((radix ? "Radix" : "In-place copmarison") + " sorting.");
      out.println((readWriteProvidersMapped ? "Mapped" : "Direct") + " buffers.");
      
      // the input file:
      fileName = argList.remove(0);
      final File f = new File(fileName);
      if (!f.exists() || !f.isFile() || !f.canRead() || !f.canWrite()) {
         out.println("File ["
               + f.getAbsolutePath()
               + "] does not exist, is not a regular file, or cannot be read/written.");
         return 4;
      }
      
      fileLength = f.length();
      if (debug) { out.println("file length: " + fileLength); }
      if (fileLength % dataLength != 0) {
         out.println("Length of file [" + f.getAbsolutePath() + "] "
               + fileLength + " is not a multiple of "+dataLength+".");
         return 4;
      }

      // thread count:
      String threads = argList.remove(0);
      threadCount = Integer.parseInt(threads);
      out.println("Threads: " + threadCount);
      if (threadCount < 1) {
         out.println("Thread count [" + threadCount + "] is less than 1.");
         return 3;
      }

      // max alloc native (optional)
      if (argList.size() > 0) {
        long mb = Long.parseLong(argList.remove(0));
        if (mb <= 0) {
          out.println("Max native alloc must be positive. (" + mb + " specified.)");
          return 3;
        }
        maxAllocNumbers = megabytesToInts(mb);
        out.println("Max alloc native (Mb): " + intsToMegabytes(maxAllocNumbers) );
      } else {
        out.println("Max alloc native (Mb): " + intsToMegabytes(maxAllocNumbers) + " (default).");
      }
      
      // check memory.
      // check up if the xmx corresponds to the thread count conditions:
      long actualXmxBytes = Runtime.getRuntime().maxMemory();
      if (debug) { out.println("max memory found to be " + actualXmxBytes + " bytes."); }
      long expectedXmxBytes = (8 + (threadCount - 1)) * megaByte;
      long delta = expectedXmxBytes - actualXmxBytes;
      if (delta > 1024L * 1024L/* 1m */) {
         out.println("Expected (8 + 1(threadCount - 1))) megabytes = "
               + expectedXmxBytes + ", however, only " + actualXmxBytes
               + " bytes found to be available.");
         return 3;
      }

      return 0;
   }

   private int runImpl() throws Exception {
      final long numLength = (fileLength >> log2DataLength);
      final TaskPlanner planner;
      if (radix) {
        planner = new RadixPlannerImpl(threadCount, numLength, maxAllocNumbers, fileName);
      } else {
        planner = new TaskPlannerImpl(threadCount, numLength, maxAllocNumbers, fileName);
      }
      Exec exec = new Exec();
      exec.executeWithPalnner(threadCount, 120, TimeUnit.MINUTES, planner);
      planner.finish();
      return 0;
   }

}
