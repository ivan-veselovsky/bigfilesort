package edu.bigfilesort.util;

import org.junit.Before;
import org.junit.Test;

import edu.bigfilesort.Util;
import edu.bigfilesort.Util.DivisionResult;
import static org.junit.Assert.*;

public class TestSmallerFirstDivisionResultIterator {

  @Before
  public void before() {
    assert Util.assertionsEnabled();
  }
  
  @Test
  public void testByZero() {
    try {
      DivisionResult dr = Util.divideByApproximatelyEqualParts(2, 0);
      fail("ae expected, but obtained " + dr);
    } catch (ArithmeticException ae) {
      // okay
    }
  }
  
  @Test
  public void testBy1() {
    DivisionResult dr = Util.divideByApproximatelyEqualParts(6, 1);
    SmallFirstDivisionResultIterator it = new SmallFirstDivisionResultIterator(dr);
    Range r;
    r = it.next();
    assertEquals(0, r.start);
    assertEquals(6, r.length);
    r = it.next();
    assertNull(r);
  }
  
  @Test
  public void testNoRemainder() {
    DivisionResult dr = Util.divideByApproximatelyEqualParts(6, 2);
    SmallFirstDivisionResultIterator it = new SmallFirstDivisionResultIterator(dr);
    Range r;
    r = it.next();
    assertEquals(0, r.start);
    assertEquals(3, r.length);
    r = it.next();
    assertEquals(3, r.start);
    assertEquals(3, r.length);
    r = it.next();
    assertNull(r);
  }

  @Test
  public void testByNumerator() {
    DivisionResult dr = Util.divideByApproximatelyEqualParts(2, 2);
    SmallFirstDivisionResultIterator it = new SmallFirstDivisionResultIterator(dr);
    Range r;
    r = it.next();
    assertEquals(0, r.start);
    assertEquals(1, r.length);
    r = it.next();
    assertEquals(1, r.start);
    assertEquals(1, r.length);
    r = it.next();
    assertNull(r);
  }

  @Test
  public void testZeroNumerator() {
    DivisionResult dr = Util.divideByApproximatelyEqualParts(0, 2);
    SmallFirstDivisionResultIterator it = new SmallFirstDivisionResultIterator(dr);
    Range r;
    r = it.next();
    assertNull(r);
  }
  
  @Test
  public void testByLargerThanNumerator() {
    DivisionResult dr = Util.divideByApproximatelyEqualParts(2, 5);
    SmallFirstDivisionResultIterator it = new SmallFirstDivisionResultIterator(dr);
    Range r;
    r = it.next();
    assertEquals(0, r.start);
    assertEquals(1, r.length);
    r = it.next();
    assertEquals(1, r.start);
    assertEquals(1, r.length);
    r = it.next();
    assertNull(r);
    
    it.reset();
    
    r = it.next();
    assertEquals(0, r.start);
    assertEquals(1, r.length);
    r = it.next();
    assertEquals(1, r.start);
    assertEquals(1, r.length);
    r = it.next();
    assertNull(r);
  }
  
  @Test
  public void test38() {
    DivisionResult dr = Util.divideByApproximatelyEqualParts(38, 11);
    SmallFirstDivisionResultIterator it = new SmallFirstDivisionResultIterator(dr);
    testImpl38(it);
    it.reset();
    testImpl38(it);
  }
  
  private void testImpl38(SmallFirstDivisionResultIterator it) {
    Range r;
    long expectedStart = 0;
    long expectedLen = 3;
    for (int i=0; i<11; i++) {
      if (i == 6) {
        expectedLen = 4;
      }
      r = it.next();
      assertEquals(expectedStart, r.start);
      assertEquals(expectedLen, r.length);
      expectedStart += expectedLen;
    }
    assertNull(it.next());
  }
  
  @Test
  public void test3() {
    DivisionResult dr = Util.divideByApproximatelyEqualParts(3, 2);
    SmallFirstDivisionResultIterator it = new SmallFirstDivisionResultIterator(dr);
    Range r;
    
    r = it.next();
    assertEquals(0, r.start);
    assertEquals(1, r.length);
    
    r = it.next();
    assertEquals(1, r.start);
    assertEquals(2, r.length);
    
    r = it.next();
    assertNull(r);
    
    it.reset();
    
    r = it.next();
    assertEquals(0, r.start);
    assertEquals(1, r.length);
  }
}
