package edu.bigfilesort;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.junit.Test;

import edu.bigfilesort.WriteDataMain.DataProvider;
import edu.bigfilesort.WriteDataMain.MergeTestDataProvider;
import static org.junit.Assert.*;

public class TestNetSort {

  @Test
  public void testComparator() throws Exception {
    String file = "test-comparator.data";
    
    // write:
    WriteDataMain writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(file, "" + (Main.dataLength * 2), 
        WriteDataMain.Mode.desc.toString()));
    
    // compare and swap:
    RandomAccessFile raf = new RandomAccessFile(file, "rw");
    FileChannel fc = raf.getChannel();
    try {
      Comparator cmp = new FileComparator(fc);
      cmp.compareAndSwap(0, 1);
    } finally {
      fc.close();
      raf.close();
    }
    
    // check sorting:
    assertEquals(0, CheckSortedMain.mainImpl(file));
  }
  
  @Test
  public void testNetSortPrint() throws Exception {
    final long numLength = 32;
    
    DiagnosticComparator cmp = new DiagnosticComparator();
    NetSort ns = new NetSort(cmp, null);
    ns.merge(0, numLength);
    
    System.out.println("cmp count = " + cmp.getCmpCount());
  }

  @Test
  public void testMerge2() throws Exception {
    final long numLength = 2;
    final String file = "test-merge-2.data";
    
    // write:
    WriteDataMain writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(file, "" + (Main.dataLength * numLength), 
        WriteDataMain.Mode.desc.toString()
        //WriteDataMain.Mode.rand.toString()
        ));
    
    // compare and swap:
    RandomAccessFile raf = new RandomAccessFile(file, "rw");
    FileChannel fc = raf.getChannel();
    Comparator cmp = new FileComparator(fc);
    NetSort ns = new NetSort(cmp, null);
    try {
      ns.merge(0, numLength);
    } finally {
      fc.close();
      raf.close();
    }
    System.out.println("cmp count = " + cmp.getCmpCount());
    // check sorting:
    assertEquals(0, CheckSortedMain.mainImpl(file));
  }
  
  @Test
  public void testMerge32() throws Exception {
    final long numLength = 32;
    final String file = "test-merge.data";
    
    DataProvider provider = new MergeTestDataProvider((int)numLength, 0, 2, 1);
    WriteDataMain writeDataMain = new WriteDataMain();
    writeDataMain.setProvider(provider);
    // write:
    assertEquals(0, writeDataMain.mainImpl(file, "" + (Main.dataLength * numLength), 
        //WriteDataMain.Mode.desc.toString()
        //WriteDataMain.Mode.rand.toString()
        null
        ));
    
    // compare and swap:
    RandomAccessFile raf = new RandomAccessFile(file, "rw");
    FileChannel fc = raf.getChannel();
    Comparator cmp = new FileComparator(fc);
    NetSort ns = new NetSort(cmp, null);
    try {
      ns.merge(0, numLength);
    } finally {
      fc.close();
      raf.close();
    }
    
    // check sorting:
    assertEquals(0, CheckSortedMain.mainImpl(file));
  }
  
  /**
   * Main test that checks entire NetSort correctness.
   * @throws Exception
   */
  @Test
  public void testSortingByRecursiveMerge() throws Exception {
    final long numLength = 1024 * 16;
    final String file = "test-sort.data";
    
    //DataProvider provider = new MergeTestDataProvider((int)numLength, 0, 2, 1);
    WriteDataMain writeDataMain = new WriteDataMain();
    //writeDataMain.setProvider(provider);
    // write:
    assertEquals(0, writeDataMain.mainImpl(file, "" + (Main.dataLength * numLength), 
        WriteDataMain.Mode.desc.toString()
        //WriteDataMain.Mode.rand.toString()
        //null
        ));
    
    final long t = System.currentTimeMillis();
    RandomAccessFile raf = new RandomAccessFile(file, "rw");
    FileChannel fc = raf.getChannel();
    Comparator cmp = new FileComparator(fc);
    NetSort ns = new NetSort(cmp, null);
    try {
      ns.aggregatedMerge(0, numLength, 1);
    } finally {
      fc.close();
      raf.close();
    }
    final long delta = System.currentTimeMillis() - t;
    System.out.println("====== Net Sorting took " + delta + " ms.");
    
    final long expectedCmp = Util.expectedNetSortCmp(numLength); 
    System.out.println("Expected numper of comparisons: " + expectedCmp);
    final long actualCmp = cmp.getCmpCount();
    System.out.println("Actual numper of comparisons: " + actualCmp);
    assertEquals(expectedCmp, actualCmp);
    
    // check sorting:
    assertEquals(0, CheckSortedMain.mainImpl(file));
  }

  /**
   * Check aggregSort() with non-zero starting offset.
   */
  @Test
  public void testSortingBy2HalfMerge() throws Exception {
    final long numLength = 64;
    final String file = "test-sort64.data";
    
    WriteDataMain writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(file, "" + (Main.dataLength * numLength), 
        WriteDataMain.Mode.desc.toString()
        //WriteDataMain.Mode.rand.toString()
        ));
    
    // compare and swap:
    RandomAccessFile raf = new RandomAccessFile(file, "rw");
    FileChannel fc = raf.getChannel();
    Comparator cmp = new FileComparator(fc);
    NetSort ns = new NetSort(cmp, null);
    try {
      ns.aggregatedMerge(0, numLength/2, 1);
      ns.aggregatedMerge(numLength/2, numLength/2, 1);
      ns.merge(0, numLength);
    } finally {
      fc.close();
      raf.close();
    }
    
    // check sorting:
    assertEquals(0, CheckSortedMain.mainImpl(file));
  } 
  
}
