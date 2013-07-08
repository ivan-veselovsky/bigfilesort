package edu.bigfilesort;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestNetSortSerialPerformance {

  /**
   * Check aggregSort() with non-zero starting offset.
   */
  @Test
  public void testSorting1M() throws Exception {
    final long numLength = 1 * 1024 * 1024 / Main.dataLength;
    final String file = "test-sort-1m.data";
    
    WriteDataMain writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(file, "" + (Main.dataLength * numLength), 
        WriteDataMain.Mode.desc.toString()
        //WriteDataMain.Mode.rand.toString()
        ));
    
    
    long t = System.currentTimeMillis();
    // sort:
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
    
    long delta = System.currentTimeMillis() - t;
    System.out.println("10m sorting took " + delta + " ms.");
    
    // check sorting:
    assertEquals(0, CheckSortedMain.mainImpl(file));
  } 
  
}
