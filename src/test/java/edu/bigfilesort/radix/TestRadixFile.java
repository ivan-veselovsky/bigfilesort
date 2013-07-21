package edu.bigfilesort.radix;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import edu.bigfilesort.CheckSortedMain;
import edu.bigfilesort.Main;
import edu.bigfilesort.UtilForTest;
import edu.bigfilesort.WriteDataMain;
import edu.bigfilesort.util.Checksum;
import edu.bigfilesort.util.ChecksumBuilder;

public class TestRadixFile {

  @Before
  public void before() {
    assert assertionsEnabled();
  }
  
  private boolean assertionsEnabled() {
    System.out.println("Assertions enabled.");
    return true;
  }

  protected void sortImpl(Storage mainStorage, Storage tmpStorage, long bufLen) throws IOException {
    final RadixSort radix = new RadixSort(mainStorage, tmpStorage);
    radix.sort(bufLen);
  }
  
  protected void testImpl(long numLength, long bufLen)  throws Exception {
    System.out.println("=============================== " + numLength);
    final String srcFile = UtilForTest.testDataDir + "/" + "test-merge-"+numLength+"-src.data";
    final String tmpFile = UtilForTest.testDataDir + "/" + "test-merge-"+numLength+"-src.data.tmp";
    // write:
    WriteDataMain writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(srcFile, Long.toString(Main.dataLength * numLength), 
        WriteDataMain.Mode.rand.toString()
        ));
    
    final Storage mainStorage = new FileStorage(srcFile, false);
    final Storage tmpStorage = new FileStorage(tmpFile, mainStorage.length());
    final Checksum sum0 = ChecksumBuilder.calculateChecksum(mainStorage, (int)(bufLen/2));
    System.out.println("Checksum0: " + sum0);
    final long t0 = System.currentTimeMillis();
    
    sortImpl(mainStorage, tmpStorage, bufLen);
    
    mainStorage.close();
    tmpStorage.close();
    final long t1 = System.currentTimeMillis() - t0;
    System.out.println("Sorting took "+ t1 + " ms");
    
    new File(tmpFile).delete();
    
    // check sorting:
    assertEquals(0, new CheckSortedMain().mainImpl(srcFile));
    final Checksum sum1 = ChecksumBuilder.calculateChecksum(new FileStorage(srcFile, true), (int)(bufLen/2));
    System.out.println("Checksum1: " + sum1);
    assertEquals(sum0, sum1);
  }
  
  @Test
  public void testRadixSort10() throws Exception {
    final long numLength = 10;
    final long bufLen = 1024L;
    testImpl(numLength, bufLen);
  }

  @Test
  public void testRadixSort1024() throws Exception {
    final long numLength = 1024L;
    final long bufLen = 1024L;
    testImpl(numLength, bufLen);
  }
  
  @Test
  public void testRadixSort1m() throws Exception {
    final long numLength = 1024L * 1024L;
    final long bufLen = 3 * 1024L;
    testImpl(numLength, bufLen);
  }
  
  @Test
  // -XX:MaxDirectMemorySize=1024m
  public void testRadixSortBig() throws Exception {
    final long numLength = 1024L * 1024L * 32; // 128 is ok for test
    final long bufLen    = 1024L * 1024L * 16; // 128 is default
    testImpl(numLength, bufLen);
  }
  
}
