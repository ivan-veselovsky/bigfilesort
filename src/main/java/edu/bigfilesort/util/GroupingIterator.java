package edu.bigfilesort.util;

public class GroupingIterator implements ResettableIterator<Range> {

  private final ResettableIterator<Range> it;
  private int groupSize;
  
  public GroupingIterator(ResettableIterator<Range> it0, int groupSize0) {
    it = it0;
    setGroupSize(groupSize0);
  }

  @Override
  public Range next() {
    Range r = it.next();
    // optimization for r == 1:
    if (groupSize == 1) {
      return r;
    }
    if (r == null) {
      return null; // it's over
    }
    int groupCount = 1;
    final long start0 = r.start;
    long length = r.length;
    while (groupCount < groupSize) {
      r = it.next();
      if (r == null) {
        break;
      } else {
        length += r.length;
        groupCount++;
      }
    }
    return new Range(start0, length);
  }

  @Override
  public void reset() {
    it.reset();
  }

  public void setGroupSize(int groupSize0) {
    if (groupSize0 < 1) {
      throw new IllegalArgumentException("Group size < 1: " + groupSize0);
    }
    groupSize = groupSize0;
  }
}
