package edu.bigfilesort;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestMain {

  protected Main getMain() {
    return new Main();
  }
  
  protected void testImpl(final long numLength, final long maxAllocBytes, final int threads) throws Exception {
    assert Util.assertionsEnabled();
    
    final String file = UtilForTest.testDataDir + "/" + "test-sort-"+(numLength/Main.megaByte)+"x4m.data";
    
    WriteDataMain writeDataMain = new WriteDataMain();
    // write:
    assertEquals(0, writeDataMain.mainImpl(file, Long.toString(Main.dataLength * numLength), 
        WriteDataMain.Mode.rand.toString()
        ));
    
    final long t0 = System.currentTimeMillis();
    
    int result = getMain().mainImpl(file, Integer.toString(threads), Long.toString(maxAllocBytes >> 20));
    assertEquals(0, result);
    
    final long delta0 = System.currentTimeMillis() - t0;
    System.out.println("=================================== Sorting took " + delta0 + " ms.");
    
    assertEquals(0, new CheckSortedMain().mainImpl(file));
  }
  
  private static final long numLength = 1024L * 1024L * 32;//256; //64; //1027; // 256 : 1G
  private static final long maxAllocBytes = 1024L * 1024 * 11 * 4;//128;//128; // 128m = default
  
  @Test
  public void test_1thread() throws Exception {
    final int threads = 1;
    testImpl(numLength, maxAllocBytes, threads);
  }
  
  @Test
  public void test_5threads() throws Exception {
    final int threads = 5;
    testImpl(numLength, maxAllocBytes, threads);
  }

  @Test
  public void testExcessiveAlloc1() throws Exception {
    final int threads = 1;
    // case without merge (memory is enough, 1 thread):
    testImpl(1024L * 1024L * 3, 1024L * 1024 * 4 * 4, threads);
  }
  
  @Test
  public void testExcessiveAlloc5() throws Exception {
    final int threads = 5;
    // case with merge (memory is enough, but there are 5 threads):
    testImpl(1024L * 1024L * 3, 1024L * 1024 * 4 * 4, threads);
  }
  
  @Test
  public void test3SortingTasks() throws Exception {
    final int threads = 1;
    // case of uncoupled merge at the end (3 sorting tasks):
    testImpl(1024L * 1024L * 3, 1024L * 1024 * 1 * 4, threads);
  }
  
  @Test
  public void test2SortingTasks() throws Exception {
    final int threads = 1;
    // case of 2 sorting tasks (1 merge):
    testImpl(1024L * 1024L * 3, 1024L * 1024 * 2 * 4, threads);
  }
}
