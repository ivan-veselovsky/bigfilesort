package edu.bigfilesort;

import static java.lang.System.out;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
  
   public static final int dataLength = 4; // bytes
   
   /*
    * Used to simplify multiplication of the byte-based indexes: 
    */
   public static final int log2DataLength = 2; // Util.log2(dataLength);
   
   public static final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
   
   // ----------------------------------------------------------------
   
   private static final long megaByte = 1024L * 1024L;
   
   public static boolean debug = true;

   public static boolean countersEnabled = false;
   
   // ----------------------------------------------------------------
   
   private String fileName;
   private RandomAccessFile raf, rafZ;
   private long fileLength;
   private FileChannel fc, fcZ;
   private int threadCount;
   private long bytesPerThread;

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
      out.println("Time elapsed:  " + delta + " ms");
      return result;
   }

   private int setParameters(String[] args) throws Exception {
      if (args.length < 2) {
         out.println("2 paramaters expected: <file name> <thread count>");
         return 3;
      }

      // the input file:
      fileName = args[0];
      File f = new File(fileName);
      if (!f.exists() || !f.isFile() || !f.canRead() || !f.canWrite()) {
         out.println("File ["
               + f.getAbsolutePath()
               + "] does not exist, is not a regular file, or cannot be read/written.");
         return 4;
      }
      // NB: do not require synchronous update:
      raf = new RandomAccessFile(fileName, "rw");
      fileLength = raf.length();
      out.println("file length: " + fileLength);
      if (fileLength % 4 != 0) {
         out.println("Length of file [" + f.getAbsolutePath() + "] "
               + fileLength + " is not a multiple of 4.");
         return 4;
      }

      // thread count:
      threadCount = Integer.parseInt(args[1]);
      out.println("thread count: " + threadCount);
      if (threadCount < 1) {
         out.println("Thread count [" + threadCount + "] is less than 1.");
         return 3;
      }

      // memory.
      // check up if the xmx corresponds to the thread count conditions:
      long actualXmxBytes = Runtime.getRuntime().maxMemory();
      out.println("max memory found to be " + actualXmxBytes + " bytes.");
      long expectedXmxBytes = (8 + (threadCount - 1)) * megaByte;
      long delta = expectedXmxBytes - actualXmxBytes;
      if (delta > 500 * 1024L/* 500k */) {
         out.println("Expected (8 + 1(threadCount - 1))) megabytes = "
               + expectedXmxBytes + ", however, only " + actualXmxBytes
               + " bytes found to be available.");
         return 3;
      }

      // calculate approx memory buffer size per thread:
      bytesPerThread = (actualXmxBytes * 9) / (threadCount * 10L);
      out.println("bytes per thread = " + bytesPerThread);

      return 0;
   }

   private int runImpl() throws Exception {
      fc = raf.getChannel();

      final int numPieces = threadCount;
      if (!Util.isPowerOf2(numPieces)) {
        throw new IllegalArgumentException("Temporary limitation: number of threads must be a power of 2.");
      }

      assert (fileLength % dataLength == 0);
      final long totalNumbers = fileLength / dataLength;

      long reminder = totalNumbers % numPieces;
      long numbersInPiece = totalNumbers / numPieces;
      out.println("numbersInPiece: " + numbersInPiece);
      if (reminder > 0) {
         numbersInPiece++;
         reminder = totalNumbers % numbersInPiece;
      }
      if (reminder == 0) {
         assert (numbersInPiece * numPieces + reminder == totalNumbers);
      } else {
         assert (numbersInPiece * (numPieces - 1) + reminder == totalNumbers);
      }
      // XXX:
      assert reminder == 0;
      
      final ExecutorService executorSerice = Executors
            .newFixedThreadPool(numPieces);
      for (int i = 0; i < numPieces; i++) {
         final long position = i * numbersInPiece * dataLength;
         final long length;
         if (i == numPieces - 1 && reminder > 0) {
            length = reminder * dataLength;
         } else {
            length = numbersInPiece * dataLength;
         }
         executorSerice.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
               out.println("Started:  " + position + " +" + length);
               try {
                  MemorySorter.sort(fc, length, position);
               } catch (Exception e) {
                  e.printStackTrace();
               }
               out.println("Finished: " + position + " +" + length);
               return null;
            }
         });
      }
      executorSerice.shutdown();
      executorSerice.awaitTermination(5, TimeUnit.MINUTES);

      fc.force(false);
      
      mergeImpl(0, numPieces, numbersInPiece);
      
      fc.force(false);
      fc.close();
      raf.close();
      if (fcZ != null) {
        fcZ.force(false);
        fcZ.close();
      }
      if (rafZ != null) {
        rafZ.close();
      }
      return 0;
   }

   /*
    * Merges sorted pieces using net sorting.
    */
   void mergeImpl(final long startNumPos, final long numPieces, final long pieceNumLength) throws Exception {
     final long t = System.currentTimeMillis();
     
//     Comparator cmp = new FileComparator(fc);
//     NetSort ns = new NetSort(cmp, null);
//     ns.aggregatedMerge(startNumPos, numPieces, pieceNumLength);
     allocateZFile();
     SimpleMerger sm = new SimpleMerger(fc, fcZ);
     sm.aggregatedMerge(startNumPos, numPieces, pieceNumLength);
     
     final long delta = System.currentTimeMillis() - t;
     System.out.println("====== Z Sorting took " + delta + " ms.");
   }
   
   /*
    * Allocates temp file for merge of the same length that the input data file.
    */
   private void allocateZFile() throws IOException {
     rafZ = new RandomAccessFile(fileName + ".z" , "rw");
     rafZ.setLength(fileLength);
     fcZ = rafZ.getChannel();
   }
}
