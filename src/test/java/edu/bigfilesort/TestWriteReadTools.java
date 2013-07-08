package edu.bigfilesort;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import static java.lang.System.out; 

public class TestWriteReadTools {

  private static final int arrayLength = 1024 * 1024 * 64; 
  
  protected long byteLength = ((long)arrayLength) * Main.dataLength;
  
  @Test
  public void testWriteRead() throws Exception {
    String file = "near1g.data";

    {
    out.println("===== writing...");
    long t = System.currentTimeMillis();
    WriteDataMain writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(file, "" + byteLength, 
        WriteDataMain.Mode.asc.toString()));
    long delta = System.currentTimeMillis() - t;
    out.println("===== writing took " + delta + " ms");
    }
    
    {
    out.println("===== reading...");
    long t = System.currentTimeMillis();
    assertEquals(0, CheckSortedMain.mainImpl(file));
    long delta = System.currentTimeMillis() - t;
    out.println("===== Reading took " + delta + " ms");
    }
  }
}
