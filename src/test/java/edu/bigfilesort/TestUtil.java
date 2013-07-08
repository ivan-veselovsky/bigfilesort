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
  
  @Test
  public void testDivide() {
    {
    Couple c = divideByNumberOfPieces(7, 3);
    assertEquals(3, c.fraction);
    assertEquals(1, c.remainder);
    }

    {
    Couple c = divideByNumberOfPieces(10, 5);
    assertEquals(2, c.fraction);
    assertEquals(0, c.remainder);
    }
    
    {
    Couple c = divideByPiecesOfLength(7, 3);
    assertEquals(3, c.fraction);
    assertEquals(1, c.remainder);
    }
    
    {
    Couple c = divideByPiecesOfLength(10, 5);
    assertEquals(2, c.fraction);
    assertEquals(0, c.remainder);
    }
    
    {
    Couple c = divideByNumberOfPieces(101, 4);
    // 4 pieces in total: 3 of length 26 and 1 of length 23.
    // 101 = 3 * 26 + 23:
    assertEquals(26, c.fraction);
    assertEquals(23, c.remainder);
    }
    {
    Couple c = divideByPiecesOfLength(101, 4);
    // 26 pieces in total, 25 of length 4 and 1 of length 1. 
    // 101 = (26 - 1) * 4 + 1:
    assertEquals(26, c.fraction);
    assertEquals(1, c.remainder);
    }
  }
}
