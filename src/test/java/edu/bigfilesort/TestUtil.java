package edu.bigfilesort;

import org.junit.Before;
import org.junit.Test;

import edu.bigfilesort.util.CoveringIterator;
import edu.bigfilesort.util.GroupingIterator;
import edu.bigfilesort.util.LargeFirstDivisionResultIterator;
import edu.bigfilesort.util.LoopingIterator;
import edu.bigfilesort.util.Range;
import edu.bigfilesort.util.ResettableIterator;
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
  
  @Test
  public void testLoopingIterator() {
    DivisionResult dr = Util.divideByApproximatelyEqualParts(38, 11);
    ResettableIterator<Range> it0 = new LargeFirstDivisionResultIterator(dr);
    ResettableIterator<Range> loop = new LoopingIterator(it0);
    
    Range r;
    r = loop.next();
    assertEquals(new Range(0, 4), r);
    r = loop.next();
    assertEquals(new Range(4, 4), r);
    r = loop.next();
    assertEquals(new Range(8, 4), r);
    r = loop.next();
    assertEquals(new Range(12, 4), r);
    r = loop.next();
    assertEquals(new Range(16, 4), r);
    r = loop.next();
    assertEquals(new Range(20, 3), r);
    r = loop.next();
    assertEquals(new Range(23, 3), r);
    r = loop.next();
    assertEquals(new Range(26, 3), r);
    r = loop.next();
    assertEquals(new Range(29, 3), r);
    r = loop.next();
    assertEquals(new Range(32, 3), r);
    r = loop.next();
    assertEquals(new Range(35, 3), r);
    
    r = loop.next();
    assertEquals(new Range(38+0, 4), r);
    r = loop.next();
    assertEquals(new Range(38+4, 4), r);
    r = loop.next();
    assertEquals(new Range(38+8, 4), r);
    r = loop.next();
    assertEquals(new Range(38+12, 4), r);
    r = loop.next();
    assertEquals(new Range(38+16, 4), r);
    r = loop.next();
    assertEquals(new Range(38+20, 3), r);
    r = loop.next();
    assertEquals(new Range(38+23, 3), r);
    r = loop.next();
    assertEquals(new Range(38+26, 3), r);
    r = loop.next();
    assertEquals(new Range(38+29, 3), r);
    r = loop.next();
    assertEquals(new Range(38+32, 3), r);
    r = loop.next();
    assertEquals(new Range(38+35, 3), r);
    
    r = loop.next();
    assertEquals(new Range(38+38+0, 4), r);
  }
  
  @Test
  public void testCoveringDivResultIterator() {
    DivisionResult dr = Util.divideByApproximatelyEqualParts(38, 11);
    ResettableIterator<Range> it0 = new LargeFirstDivisionResultIterator(dr);
    CoveringIterator cov = new CoveringIterator(77, it0);
    int count = 0;
    Range last = null;
    Range r;
    while (true) {
      r = cov.next();
      if (r == null) {
        break;
      } else {
        count++;
        System.out.println(count + ": " + r);
        last = r;
      }
    } 
    assertEquals(23, count);
    assertEquals(new Range(76,1), last);
  }
  
  @Test
  public void testCoveringDivResultIterator2() {
    DivisionResult dr = Util.divideByApproximatelyEqualParts(38, 11);
    ResettableIterator<Range> it0 = new LargeFirstDivisionResultIterator(dr);
    CoveringIterator cov = new CoveringIterator(76, it0);
    int count = 0;
    Range last = null;
    Range r;
    while (true) {
      r = cov.next();
      if (r == null) {
        break;
      } else {
        count++;
        System.out.println(count + ": " + r);
        last = r;
      }
    } 
    assertEquals(22, count);
    assertEquals(new Range(73,3), last);
  }
  
  @Test
  public void testGroupingIterator() {
    DivisionResult dr = Util.divideByApproximatelyEqualParts(3, 3);
    ResettableIterator<Range> it0 = new LargeFirstDivisionResultIterator(dr);
    
    GroupingIterator it = new GroupingIterator(it0, 1);
    Range r ;
    r = it.next();
    assertEquals(new Range(0,1), r);
    r = it.next();
    assertEquals(new Range(1,1), r);
    r = it.next();
    assertEquals(new Range(2,1), r);
    r = it.next();
    assertNull(r);
    
    it.reset();
    it.setGroupSize(2);
    
    r = it.next();
    assertEquals(new Range(0,2), r);
    r = it.next();
    assertEquals(new Range(2,1), r);
    r = it.next();
    assertNull(r);
    
    it.reset();
    it.setGroupSize(3);
    
    r = it.next();
    assertEquals(new Range(0,3), r);
    r = it.next();
    assertNull(r);
    
    it.reset();
    it.setGroupSize(4);
    
    r = it.next();
    assertEquals(new Range(0,3), r);
    r = it.next();
    assertNull(r);
  }
  
  @Test
  public void testGroupingIteratorSetGroupInTheMiddle() {
    DivisionResult dr = Util.divideByApproximatelyEqualParts(4, 4);
    ResettableIterator<Range> it0 = new LargeFirstDivisionResultIterator(dr);
    GroupingIterator it = new GroupingIterator(it0, 1);
    Range r ;
    r = it.next();
    assertEquals(new Range(0,1), r);
    it.setGroupSize(2);
    r = it.next();
    assertEquals(new Range(1,2), r);
    r = it.next();
    assertEquals(new Range(3,1), r);
    r = it.next();
    assertNull(r);
    
    it.reset();
    it.setGroupSize(2);
    
    r = it.next();
    assertEquals(new Range(0,2), r);
    it.setGroupSize(1);
    r = it.next();
    assertEquals(new Range(2,1), r);
    r = it.next();
    assertEquals(new Range(3,1), r);
    r = it.next();
    assertNull(r);
  }
}
