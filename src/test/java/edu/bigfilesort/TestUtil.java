package edu.bigfilesort;

import org.junit.Test;
import static org.junit.Assert.*;
import static edu.bigfilesort.Util.*;

public class TestUtil {
  @Test
  public void testLog2() {
    assertEquals(0, log2(1));
    assertEquals(10, log2(1024));
    for (int i=0; i<63; i++) {
      assertEquals(i, log2(1L << i));
    }
    
    assertEquals(2, log2(4));
    assertEquals(2, log2plus(4));
    
    assertEquals(1, log2(3));
    assertEquals(2, log2plus(3));
    
    assertEquals(10, log2(1025));
    assertEquals(11, log2plus(1025));
  }
}
