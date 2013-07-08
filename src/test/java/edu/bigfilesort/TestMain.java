package edu.bigfilesort;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestMain {

  @Test
  public void testPieceMemorySortingMultithread() throws Exception {
    
    final long numLength = 1024L * 1024 * 64; //1027; // 256 : 1G
    final long maxAllocInts = 1024L * 1024 * 128; // default
    final int threads = 5;
    final String file = "test-sort-"+(numLength/Main.megaByte)+"x4m.data";
    
    //DataProvider provider = new MergeTestDataProvider((int)numLength, 0, 2, 1);
    WriteDataMain writeDataMain = new WriteDataMain();
    //writeDataMain.setProvider(provider);
    // write:
    assertEquals(0, writeDataMain.mainImpl(file, Long.toString(Main.dataLength * numLength), 
        WriteDataMain.Mode.rand.toString()
        //WriteDataMain.Mode.rand.toString()
        //null
        ));
    
    final long t0 = System.currentTimeMillis();
    
    new Main().mainImpl(file, Integer.toString(threads), Long.toString(maxAllocInts));
    
    final long delta0 = System.currentTimeMillis() - t0;
    System.out.println("Sorting took " + delta0 + " ms.");
    
    assertEquals(0, CheckSortedMain.mainImpl(file));
  }
}
