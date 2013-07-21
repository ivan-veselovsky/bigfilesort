package edu.bigfilesort.util;

/**
 * Repeats range sequence from the supplied Iterator
 * until it covers the specified length.
 */
public class CoveringIterator implements ResettableIterator<Range> {
  private final long lengthToCover;
  private final ResettableIterator<Range> loopingIterator;
  
  public CoveringIterator(long lengthToCover0, ResettableIterator<Range> it0) {
    lengthToCover = lengthToCover0;
    loopingIterator = new LoopingIterator(it0);
  }

  @Override
  public Range next() {
    Range r = loopingIterator.next();
    // r must not be null.
    if (r.start + r.length <= lengthToCover) {
      return r;
    } else if (lengthToCover > r.start) {
      return new Range(r.start, lengthToCover - r.start);
    } else {
      return null;
    }
  }
  
  @Override
  public void reset() {
    loopingIterator.reset();
  }
}
