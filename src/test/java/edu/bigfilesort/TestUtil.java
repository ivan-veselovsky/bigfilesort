package edu.bigfilesort;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static edu.bigfilesort.Util.*;

public class TestUtil {
  
  @Before
  public void before() {
    assert Util.assertionsEnabled();
  }
  
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
  public void testDivideByApproximatelyEqualParts() {
    {
    DivisionResult c = divideByApproximatelyEqualParts(7, 3);
    assertEquals(new DivisionResult(2, 2, 1, 3), c);
    }
    {
    DivisionResult c = divideByApproximatelyEqualParts(149, 75);
    assertEquals(new DivisionResult(1, 1, 74, 2), c);
    }
    {
    DivisionResult c = divideByApproximatelyEqualParts(77, 75);
    assertEquals(new DivisionResult(73, 1, 2, 2), c);
    }
    {
    DivisionResult c = divideByApproximatelyEqualParts(20, 3);
    assertEquals(new DivisionResult(1, 6, 2, 7), c);
    }
    {
    DivisionResult c = divideByApproximatelyEqualParts(9, 4);
    assertEquals(new DivisionResult(3, 2, 1, 3), c);
    }
    {
    DivisionResult c = divideByApproximatelyEqualParts(10, 5);
    assertEquals(new DivisionResult(0, 0, 5, 2), c);
    }
    {
    DivisionResult c = divideByApproximatelyEqualParts(101, 4);
    assertEquals(new DivisionResult(3, 25, 1, 26), c);
    }
    {
    DivisionResult c = divideByApproximatelyEqualParts(10, 3);
    assertEquals(new DivisionResult(2, 3, 1, 4), c);
    }
  }  
  
  @Test
  public void testDivideNotLonger() {
    {
    DivisionResult c = divideNotLonger(7, 3);
    assertEquals(new DivisionResult(1, 1, 2, 3), c);
    }
    {
    DivisionResult c = divideNotLonger(10, 5);
    assertEquals(new DivisionResult(0, 0, 2, 5), c);
    }
    {
    DivisionResult c = divideNotLonger(10, 3);
    assertEquals(new DivisionResult(1, 1, 3, 3), c);
    }
    {
    DivisionResult c = divideNotLonger(101, 4);
    assertEquals(new DivisionResult(1, 1, 25, 4), c);
    }
    {
    DivisionResult c = divideNotLonger(149, 75);
    assertEquals(new DivisionResult(1, 74, 1, 75), c);
    }
    {
    DivisionResult c = divideNotLonger(77, 75);
    assertEquals(new DivisionResult(1, 2, 1, 75), c);
    }
  }
}
