package edu.bigfilesort.util;

import org.junit.Test;

import edu.bigfilesort.Util;
import edu.bigfilesort.Util.DivisionResult;
import static org.junit.Assert.*;

public class TestDivisionResultIterator {

  @Test
  public void test38() {
    DivisionResult dr = Util.divideByApproximatelyEqualParts(38, 11);
    LargeFirstDivisionResultIterator it = new LargeFirstDivisionResultIterator(dr);
    testImpl38(it);
    it.reset();
    testImpl38(it);
  }
  
  private void testImpl38(LargeFirstDivisionResultIterator it) {
    Range r;
    long expectedStart = 0;
    long expectedLen = 4;
    for (int i=0; i<11; i++) {
      if (i == 5) {
        expectedLen = 3;
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
    LargeFirstDivisionResultIterator it = new LargeFirstDivisionResultIterator(dr);
    Range r;
    
    r = it.next();
    assertEquals(0, r.start);
    assertEquals(2, r.length);
    
    r = it.next();
    assertEquals(2, r.start);
    assertEquals(1, r.length);
    
    r = it.next();
    assertNull(r);
    
    it.reset();
    
    r = it.next();
    assertEquals(0, r.start);
    assertEquals(2, r.length);
  }
}
