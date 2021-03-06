package edu.bigfilesort;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.junit.Test;

public class TestBufferedMerger {
  
  @Test
  public void testMerge2() throws Exception {
    final long numLength = 2;
    System.out.println("=============================== " + numLength);
    final String srcFile = UtilForTest.testDataDir + "/" + "test-merge-"+numLength+"-src.data";
    final String dstFile = UtilForTest.testDataDir + "/" + "test-merge-"+numLength+"-dst.data";
    
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
    moveSrcChecksumToDst(srcFile, dstFile);
    assertEquals(0, new CheckSortedMain().mainImpl(dstFile));
  }

  private void moveSrcChecksumToDst(String srcFile, String dstFile) {
    File dstF = new File(dstFile+".checksum");
    dstF.delete();
    new File(srcFile+".checksum").renameTo(dstF);
  } 
  
  @Test
  public void testMerge32() throws Exception {
    final long numLength = 32;
    System.out.println("=============================== " + numLength);
    final String srcFile = UtilForTest.testDataDir + "/" + "test-merge-"+numLength+"-src.data";
    final String dstFile = UtilForTest.testDataDir + "/" + "test-merge-"+numLength+"-dst.data";
    
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
     moveSrcChecksumToDst(srcFile, dstFile);
     assertEquals(0, new CheckSortedMain().mainImpl(dstFile));
  }

  @Test
  public void testMerge128() throws Exception {
    final long numLength = 128;
    System.out.println("=============================== " + numLength);
    final String srcFile = UtilForTest.testDataDir + "/" + "test-merge-"+numLength+"-src.data";
    final String dstFile = UtilForTest.testDataDir + "/" + "test-merge-"+numLength+"-dst.data";
    
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

    mergeImpl(17, srcFile, dstFile, 0, numLength/2, numLength/2);
    
    // check sorting:
    moveSrcChecksumToDst(srcFile, dstFile);
    assertEquals(0, new CheckSortedMain().mainImpl(dstFile));
  }
  
  //@Test disables since it is huge
  public void testMerge1G() throws Exception {
    final long numLength = 1024L * 1024 * 2048;
    final long sumBuffersSize = 1024L * 1024 * 50;
    System.out.println("=============================== " + numLength);
    final String srcFile = UtilForTest.testDataDir + "/" + "test-merge-"+numLength+"-src.data";
    final String dstFile = UtilForTest.testDataDir + "/" + "test-merge-"+numLength+"-dst.data";

    mergeImpl(sumBuffersSize, srcFile, dstFile, 0, numLength/2, numLength/2);
    
    // check sorting:
    moveSrcChecksumToDst(srcFile, dstFile);
    assertEquals(0, new CheckSortedMain().mainImpl(dstFile));
  }
  
  
  private void mergeImpl(long allowedSummaryNumBuffersLength, 
      String srcFile, String dstFile, 
      long numOffset, 
      long leftNumLen, long rightNumLen) throws Exception {
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
