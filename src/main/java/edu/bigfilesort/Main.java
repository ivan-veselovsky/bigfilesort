package edu.bigfilesort;

import static java.lang.System.out;

import java.io.File;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

import edu.bigfilesort.parallel.Exec;
import edu.bigfilesort.parallel.TaskPlanner;
import edu.bigfilesort.parallel.TaskPlannerImpl;

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
   
   public static boolean debug = false;

   public static boolean countersEnabled = false; // counters in sort algorithms
   
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
      if (debug) { out.println("Sorting took: " + delta + " ms"); }
      return result;
   }

   private long intsToMegabytes(long ints/*4-byte numbers*/) {
     return (ints << log2DataLength) / megaByte;
   }
   
   private int setParameters(String[] args) throws Exception {
      if (args.length < 2) {
         out.println("Parameters: <file name> <thread count> [max allowed native memory in mega bytes]");
         out.println("(Default for the last parameter is " + intsToMegabytes(maxAllocNumbers) + " Mb.)");
         out.println("Example:");
         out.println(" ./bigfilesort.sh test-putina-naxuy.data 7 1024");
         out.println("sorts the file using 7 threads with 1024 megabytes of total allowed native memory (both direct + mapped).");
         out.println("(Note that JVM parameter -XX:MaxDirectMemorySize=... may need to be adjusted accordingly.)");
         return 3;
      }

      // the input file:
      fileName = args[0];
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
      threadCount = Integer.parseInt(args[1]);
      out.println("Threads: " + threadCount);
      if (threadCount < 1) {
         out.println("Thread count [" + threadCount + "] is less than 1.");
         return 3;
      }

      // max alloc native
      if (args.length > 2) {
        long mb = Long.parseLong(args[2]);
        if (mb <= 0) {
          out.println("Max native alloc must be positive. (" + mb + " specified.)");
          return 3;
        }
        maxAllocNumbers = (megaByte * mb) >> log2DataLength;
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
      // =====================================
      // new parallel impl:
      
      final long numLength = (fileLength >> log2DataLength);
      TaskPlanner planner = new TaskPlannerImpl(threadCount, numLength, maxAllocNumbers, fileName);
      Exec exec = new Exec();
      exec.executeWithPalnner(threadCount, 120, TimeUnit.MINUTES, planner);
      planner.finish();
      
      // =====================================
      return 0;
   }

}
