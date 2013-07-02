package edu.bigfilesort;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestUtil {
  @Test
  public void testLog2() {
    assertEquals(0, Util.log2(1));
    assertEquals(10, Util.log2(1024));
    for (int i=0; i<63; i++) {
      assertEquals(i, Util.log2(1L << i));
    }
  }
}
