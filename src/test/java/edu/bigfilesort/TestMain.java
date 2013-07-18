package edu.bigfilesort;

import org.junit.Test;

import edu.bigfilesort.radix.FileStorage;
import edu.bigfilesort.radix.Storage;
import edu.bigfilesort.util.Checksum;

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
    Storage srcStorage = new FileStorage(file, true);
    final Checksum sum0 = Checksum.calculateChecksum(srcStorage, (int)((maxAllocBytes >> Main.log2DataLength)/2));
    srcStorage.close();
    System.out.println("Checksum0: " + sum0);
    
    final long t0 = System.currentTimeMillis();
    
    int result = new Main().mainImpl(file, Integer.toString(threads), Long.toString(maxAllocBytes >> 20));
    assertEquals(0, result);
    
    final long delta0 = System.currentTimeMillis() - t0;
    System.out.println("=================================== Sorting took " + delta0 + " ms.");
    
    assertEquals(0, CheckSortedMain.mainImpl(file));
    
    srcStorage = new FileStorage(file, true);
    final Checksum sum1 = Checksum.calculateChecksum(srcStorage, (int)((maxAllocBytes >> Main.log2DataLength)/2));
    srcStorage.close();
    System.out.println("Checksum1: " + sum1);
    assertEquals(sum0, sum1);
  }
  
  @Test
  public void testPieceMemorySortingMultithread() throws Exception {
    final long numLength = 1024L * 1024L * 256; //64; //1027; // 256 : 1G
    final long maxAllocBytes = 1024L * 1024 * 256;//128; // 128m = default
    // non-radix impl:
    // XXX does not work with 1 thread . Sorting violation.
    // XXX does not work for 3 threads if the file is small (~3Mb)
    final int threads = 2; // 5
    testImpl(numLength, maxAllocBytes, threads);
  }
}
