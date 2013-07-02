package edu.bigfilesort;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * TODO: looks like larger ByteBuffer is needed in merging. 
 * Just 1-2 int buffers seem to perform very slowly. 
 */
public class TestMain {

  @Test
  public void testPieceMemorySortingMultithread() throws Exception {
    final long numLength = 1024 * 1024 * 128; // 256 : 1G
    final int threads = 8;
    final String file = "test-sort.data";
    
    //DataProvider provider = new MergeTestDataProvider((int)numLength, 0, 2, 1);
    WriteDataMain writeDataMain = new WriteDataMain();
    //writeDataMain.setProvider(provider);
    // write:
    assertEquals(0, writeDataMain.mainImpl(file, Long.toString(Main.dataLength * numLength), 
        WriteDataMain.Mode.desc.toString()
        //WriteDataMain.Mode.rand.toString()
        //null
        ));
    
    final long t0 = System.currentTimeMillis();
    
    new Main().mainImpl(file, Integer.toString(threads));
    
    final long delta0 = System.currentTimeMillis() - t0;
    System.out.println("====== All Sorting took " + delta0 + " ms.");
    
    // check sorting (now temporarily check the temp "z" file):
    assertEquals(0, CheckSortedMain.mainImpl(file + ".z"));
  }
}
