package edu.bigfilesort;

import static java.lang.System.out;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import edu.bigfilesort.util.Checksum;
import edu.bigfilesort.util.ChecksumBuilder;
import edu.bigfilesort.util.ChecksumReaderWriter;
import static edu.bigfilesort.WriteDataMain.bufferSizeBytes;

public class CheckSortedMain {

  public CheckSortedMain() {
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    int status = new CheckSortedMain().mainImpl(args);
    System.exit(status);
  }
  
  public int mainImpl(String... args) throws Exception {
    if (args.length < 1) {
      out.println("Paramaters: <file name>");
      return 3;
    }
    String fileName = args[0];
    
    if ((bufferSizeBytes % Main.dataLength) != 0L) {
      throw new IllegalStateException("Buffer size must be multiple of "+Main.dataLength+".");
    }
    
    out.println("File name:   ["+fileName+"]");
    
    final long t = System.currentTimeMillis();
    
    checkSortedImpl(fileName);
    
    long delta = System.currentTimeMillis() - t;
    out.println("Sorting of ["+fileName+"] is okay.");
    out.println("Read time:  "+delta+" ms");
    
    return 0;
  }
  
  private void checkSortedImpl(final String name) throws Exception {
    final RandomAccessFile raf = new RandomAccessFile(name, "r"); // read only
    final FileChannel fc = raf.getChannel();
    final ChecksumBuilder builder = new ChecksumBuilder();
    try {
    final long fileLength = raf.length();
    out.println("Length of ["+name+"] is " + fileLength);
    if (fileLength % Main.dataLength != 0) {
    	throw new IllegalArgumentException("File length ("+fileLength+") must be multiple of " + Main.dataLength);
    }
    
    long numWrites = fileLength / bufferSizeBytes;
    long remainderLength =  fileLength % bufferSizeBytes;
    if (remainderLength > 0) {
      numWrites++;
    }
    if (remainderLength > 0) {
      assert ((numWrites - 1) * bufferSizeBytes + remainderLength == fileLength);
    } else {
      assert (numWrites * bufferSizeBytes + remainderLength == fileLength);
    }
    
    fc.position(0);
    
      long position, len;
      long readOkayByteCount = 0;
      MappedByteBuffer mbb;
      int leader = Integer.MIN_VALUE;
      for (int i=0; i<numWrites; i++) {
        position = i * bufferSizeBytes;
        len = (remainderLength > 0 
            && i == numWrites - 1) ? remainderLength : bufferSizeBytes;
        
        assert (len % Main.dataLength == 0);
        
        mbb = fc.map(MapMode.READ_ONLY, position, len);
        mbb.order(Main.byteOrder);
        
        int d;
        for (long offset = 0; offset < (len / Main.dataLength); offset++) {
          d = mbb.getInt();
          builder.next(d);
          if (d < leader) {
            assert readOkayByteCount % Main.dataLength == 0;
        	  throw new IllegalStateException("Sorting violation at *number* position ["
        	      +(readOkayByteCount / Main.dataLength)+"]: " 
        	      + Integer.toHexString(d) +" < "+ Integer.toHexString(leader));
          }
          leader = d;
          readOkayByteCount += Main.dataLength;
        }
        
        Util.disposeDirectByteBuffer(mbb);
        mbb = null;
      }
      
      assert (readOkayByteCount == fileLength);
    } finally {
      fc.close();
      raf.close();
    }
    
    // verify checksum
    final File checksumFile = new File(name + ChecksumReaderWriter.checksumFileSuffix);
    if (checksumFile.exists()) {
      Checksum actualChecksum = builder.getChecksum();
      ChecksumReaderWriter crw = new ChecksumReaderWriter(name + ChecksumReaderWriter.checksumFileSuffix);
      Checksum expectedSum = crw.readChecksum();
      if (expectedSum.equals(actualChecksum)) {
        System.out.println("Checksum ok.");
      } else {
        throw new RuntimeException("Checksum verification failed. Expected ["+expectedSum+"], but found ["+actualChecksum+"].");
      }
    }
  }
  
}
