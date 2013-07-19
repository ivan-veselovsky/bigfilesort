package edu.bigfilesort;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestMain {

  protected void testImpl(final long numLength, final long maxAllocBytes, final int threads) throws Exception {
    assert Util.assertionsEnabled();
    
    final String file = "test-sort-"+(numLength/Main.megaByte)+"x4m.data";
    
    WriteDataMain writeDataMain = new WriteDataMain();
    // write:
    assertEquals(0, writeDataMain.mainImpl(file, Long.toString(Main.dataLength * numLength), 
        WriteDataMain.Mode.rand.toString()
        ));
    
    final long t0 = System.currentTimeMillis();
    
    int result = new Main().mainImpl(file, Integer.toString(threads), Long.toString(maxAllocBytes >> 20));
    assertEquals(0, result);
    
    final long delta0 = System.currentTimeMillis() - t0;
    System.out.println("=================================== Sorting took " + delta0 + " ms.");
    
    assertEquals(0, CheckSortedMain.mainImpl(file));
  }

  
  private static final long numLength = 1024L * 1024L * 64;//256; //64; //1027; // 256 : 1G
  private static final long maxAllocBytes = 1024L * 1024 * 16;//128;//128; // 128m = default
  
  @Test
  public void test_1thread() throws Exception {
    // non-radix (old) impl:
    // XXX does not work with 1 thread . Sorting violation.
    // XXX does not work for 3 threads if the file is small (~3Mb)
    final int threads = 1;
    testImpl(numLength, maxAllocBytes, threads);
  }
  
  @Test
  public void test_5threads() throws Exception {
    // non-radix (old) impl:
    // XXX does not work with 1 thread . Sorting violation.
    // XXX does not work for 3 threads if the file is small (~3Mb)
    final int threads = 5;
    testImpl(numLength, maxAllocBytes, threads);
  }
}
