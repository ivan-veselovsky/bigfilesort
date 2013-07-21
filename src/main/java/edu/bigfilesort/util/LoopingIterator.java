package edu.bigfilesort.util;

/**
 * Iterator that repeats Ranges from the supplied iterator infinitely. 
 */
public class LoopingIterator implements ResettableIterator<Range> {
  
  private final ResettableIterator<Range> iterator;
  private Range lastRange; // last returned range
  
  public LoopingIterator(ResettableIterator<Range> it0) {
    iterator = it0;
    lastRange = new Range(0, 0);
  }
  
  @Override
  public Range next() {
    Range r = iterator.next();
    if (r == null) {
      iterator.reset();
      r = iterator.next();
    } 
    if (r != null) {
      r = r.putAfter(lastRange);
    }
    lastRange = r;
    return r;
  }
  
  @Override
  public void reset() {
    iterator.reset();
    lastRange = new Range(0, 0);
  }
}