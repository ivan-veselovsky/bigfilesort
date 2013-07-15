package edu.bigfilesort.radix;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import edu.bigfilesort.CheckSortedMain;
import edu.bigfilesort.Main;
import edu.bigfilesort.WriteDataMain;

public class TestRadixFile {

  @Before
  public void before() {
    assert assertionsEnabled();
  }
  
  private boolean assertionsEnabled() {
    System.out.println("Assertions enabled.");
    return true;
  }

  
  @Test
  public void testRadixSort10() throws Exception {
    final long numLength = 10;
    final long bufLen = 1024L;
    
    System.out.println("=============================== " + numLength);
    final String srcFile = "test-merge-"+numLength+"-src.data";
    final String tmpFile = "test-merge-"+numLength+"-src.data.tmp";
    // write:
    WriteDataMain writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(srcFile, Long.toString(Main.dataLength * numLength), 
        //WriteDataMain.Mode.desc.toString()
        WriteDataMain.Mode.rand.toString()
        ));
    writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(tmpFile, Long.toString(Main.dataLength * numLength), 
        //WriteDataMain.Mode.desc.toString()
        WriteDataMain.Mode.desc.toString()
        ));
    
    final Storage mainStorage = new FileStorage(srcFile);
    final Storage tmpStorage = new FileStorage(tmpFile);
    final RadixSort radix = new RadixSort(mainStorage, tmpStorage, bufLen);
    radix.sort();
    mainStorage.close();
    tmpStorage.close();
    
    new File(tmpFile).delete();
    
    // check sorting:
    assertEquals(0, CheckSortedMain.mainImpl(srcFile));
  }

  @Test
  public void testRadixSort1024() throws Exception {
    final long numLength = 1024L;
    final long bufLen = 1024L;
    
    System.out.println("=============================== " + numLength);
    final String srcFile = "test-merge-"+numLength+"-src.data";
    final String tmpFile = "test-merge-"+numLength+"-src.data.tmp";
    // write:
    WriteDataMain writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(srcFile, Long.toString(Main.dataLength * numLength), 
        //WriteDataMain.Mode.desc.toString()
        WriteDataMain.Mode.rand.toString()
        ));
    writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(tmpFile, Long.toString(Main.dataLength * numLength), 
        //WriteDataMain.Mode.desc.toString()
        WriteDataMain.Mode.desc.toString()
        ));
    
    final Storage mainStorage = new FileStorage(srcFile);
    final Storage tmpStorage = new FileStorage(tmpFile);
    final RadixSort radix = new RadixSort(mainStorage, tmpStorage, bufLen);
    radix.sort();
    mainStorage.close();
    tmpStorage.close();
    
    new File(tmpFile).delete();
    
    // check sorting:
    assertEquals(0, CheckSortedMain.mainImpl(srcFile));
  }
  
  @Test
  public void testRadixSort1m() throws Exception {
    final long numLength = 1024L * 1024L;
    final long bufLen = 3 * 1024L;
    
    System.out.println("=============================== " + numLength);
    final String srcFile = "test-merge-"+numLength+"-src.data";
    final String tmpFile = "test-merge-"+numLength+"-src.data.tmp";
    // write:
    WriteDataMain writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(srcFile, Long.toString(Main.dataLength * numLength), 
        //WriteDataMain.Mode.desc.toString()
        WriteDataMain.Mode.rand.toString()
        ));
    writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(tmpFile, Long.toString(Main.dataLength * numLength), 
        //WriteDataMain.Mode.desc.toString()
        WriteDataMain.Mode.desc.toString()
        ));
    
    final Storage mainStorage = new FileStorage(srcFile);
    final Storage tmpStorage = new FileStorage(tmpFile);
    final RadixSort radix = new RadixSort(mainStorage, tmpStorage, bufLen);
    radix.sort();
    mainStorage.close();
    tmpStorage.close();
    
    new File(tmpFile).delete();
    
    // check sorting:
    assertEquals(0, CheckSortedMain.mainImpl(srcFile));
  }
  
  @Test
  // -XX:MaxDirectMemorySize=1024m
  public void testRadixSort1G() throws Exception {
    final long numLength = 1024L * 1024L * 379; // 256; // 1.48G, movie
    final long bufNumLen = 1024L * 1024L * 192; // 800m // 200: ok with MDMS=1024m
    
    System.out.println("=============================== " + numLength);
    final String srcFile = "test-radix-"+numLength+"-src.data";
    final String tmpFile = "test-radix-"+numLength+"-src.data.tmp";
    
//    // write:
    WriteDataMain writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(srcFile, Long.toString(Main.dataLength * numLength), 
        //WriteDataMain.Mode.desc.toString()
        WriteDataMain.Mode.rand.toString()
        ));
    writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(tmpFile, Long.toString(Main.dataLength * numLength), 
        //WriteDataMain.Mode.desc.toString()
        WriteDataMain.Mode.desc.toString()
        ));
    
    long t = System.currentTimeMillis();
    final Storage mainStorage = new FileStorage(srcFile);
    final Storage tmpStorage = new FileStorage(tmpFile);
    final RadixSort radix = new RadixSort(mainStorage, tmpStorage, bufNumLen);
    radix.sort();
    mainStorage.close();
    tmpStorage.close();
    long delta = System.currentTimeMillis() - t;
    System.out.println("===== Radix sorting took: " + delta/1000 + " s");
    
    new File(tmpFile).delete();
    
    // check sorting:
    assertEquals(0, CheckSortedMain.mainImpl(srcFile));
  }
  
}
