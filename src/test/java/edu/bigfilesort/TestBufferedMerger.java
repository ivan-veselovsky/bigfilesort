package edu.bigfilesort;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.junit.Test;

import edu.bigfilesort.WriteDataMain.DataProvider;
import edu.bigfilesort.WriteDataMain.MergeTestDataProvider;

public class TestBufferedMerger {
  
  @Test
  public void testMerge2() throws Exception {
    final long numLength = 2;
    System.out.println("=============================== " + numLength);
    final String srcFile = "test-merge-"+numLength+"-src.data";
    final String dstFile = "test-merge-"+numLength+"-dst.data";
    
    // write:
    WriteDataMain writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(srcFile, Long.toString(Main.dataLength * numLength), 
        WriteDataMain.Mode.desc.toString()
        //WriteDataMain.Mode.rand.toString()
        ));
    writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(dstFile, Long.toString(Main.dataLength * numLength), 
        //WriteDataMain.Mode.desc.toString()
        WriteDataMain.Mode.asc.toString()
        ));

    mergeImpl(4, srcFile, dstFile, 0, numLength/2, numLength/2);
    
    // check sorting:
    assertEquals(0, CheckSortedMain.mainImpl(dstFile));
  }

  @Test
  public void testMerge32() throws Exception {
    final long numLength = 32;
    System.out.println("=============================== " + numLength);
    final String srcFile = "test-merge-"+numLength+"-src.data";
    final String dstFile = "test-merge-"+numLength+"-dst.data";
    
    // write:
    WriteDataMain writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(srcFile, Long.toString(Main.dataLength * numLength), 
        WriteDataMain.Mode.asc.toString()
        //WriteDataMain.Mode.rand.toString()
        ));
    writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(dstFile, Long.toString(Main.dataLength * numLength), 
        //WriteDataMain.Mode.desc.toString()
        WriteDataMain.Mode.desc.toString()
        ));

   mergeImpl(5, srcFile, dstFile, 0, numLength/2, numLength/2);
    
    // check sorting:
    assertEquals(0, CheckSortedMain.mainImpl(dstFile));
  }

  @Test
  public void testMerge128() throws Exception {
    final long numLength = 128;
    System.out.println("=============================== " + numLength);
    final String srcFile = "test-merge-"+numLength+"-src.data";
    final String dstFile = "test-merge-"+numLength+"-dst.data";
    
    // write:
    WriteDataMain writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(srcFile, Long.toString(Main.dataLength * numLength), 
        WriteDataMain.Mode.asc.toString()
        //WriteDataMain.Mode.rand.toString()
        ));
    writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(dstFile, Long.toString(Main.dataLength * numLength), 
        //WriteDataMain.Mode.desc.toString()
        WriteDataMain.Mode.desc.toString()
        ));

    // XXX: does not work when numLength % sumBufSize != 0
   mergeImpl(17, srcFile, dstFile, 0, numLength/2, numLength/2);
    
    // check sorting:
    assertEquals(0, CheckSortedMain.mainImpl(dstFile));
  }
  
  @Test
  public void testMerge1G() throws Exception {
    final long numLength = 1024L * 1024 * 2048;
    final long sumBuffersSize = 1024L * 1024 * 50;
    System.out.println("=============================== " + numLength);
    final String srcFile = "test-merge-"+numLength+"-src.data";
    final String dstFile = "test-merge-"+numLength+"-dst.data";

    // XXX temp
//    // write:
//    DataProvider provider = new MergeTestDataProvider((int)numLength, Integer.MIN_VALUE, 2, 1);
//    WriteDataMain writeDataMain = new WriteDataMain();
//    writeDataMain.setProvider(provider);
//    long flen = numLength * Main.dataLength;
//    assertEquals(0, writeDataMain.mainImpl(srcFile, Long.toString(flen), 
//        null //WriteDataMain.Mode.asc.toString()
//        //WriteDataMain.Mode.rand.toString()
//        ));
//    writeDataMain = new WriteDataMain();
//    assertEquals(0, writeDataMain.mainImpl(dstFile, Long.toString(Main.dataLength * numLength), 
//        //WriteDataMain.Mode.desc.toString()
//        WriteDataMain.Mode.desc.toString()
//        ));

   mergeImpl(sumBuffersSize, srcFile, dstFile, 0, numLength/2, numLength/2);
    
    // check sorting:
    assertEquals(0, CheckSortedMain.mainImpl(dstFile));
  }
  
  
  private void mergeImpl(long allowedSummaryNumBuffersLength, String srcFile, String dstFile, long numOffset, long leftNumLen, long rightNumLen) throws Exception {
    // compare and swap:
    RandomAccessFile srcRaf = new RandomAccessFile(srcFile, "r");
    FileChannel srcFc = srcRaf.getChannel();
    
    RandomAccessFile dstRaf = new RandomAccessFile(dstFile, "rw");
    FileChannel dstFc = dstRaf.getChannel();
    
    final BufferedMerger merger = new BufferedMerger(allowedSummaryNumBuffersLength);
    merger.setLeft(srcFc,  numOffset, leftNumLen);
    merger.setRight(srcFc, numOffset + leftNumLen, rightNumLen);
    merger.setTarget(dstFc, numOffset);
    merger.init();
    long t = System.currentTimeMillis();
    try {
      merger.merge();
    } finally {
      merger.dispose();
      
      srcFc.close();
      srcRaf.close();
      
      dstFc.close();
      dstRaf.close();
    }
    long d = System.currentTimeMillis() - t;
    System.out.println("Buffered Merge took " +d+ " ms");
  }

}
