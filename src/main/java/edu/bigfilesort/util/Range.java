package edu.bigfilesort.util;

public final class Range {
  
  public final long start; // inclusive
  public final long length; // length of the range
  
  public Range(long start0, long length0) {
    if (length0 < 0) {
      throw new IllegalArgumentException("Negative Range length: " + length0);
    }
    start = start0;
    length = length0;
  }
  
  public boolean contains(long x) {
    return (x >= start) && (x < (start + length));
  } 
  
  public Range add(Range r) {
    if (r.start != (start + length)) {
      throw new IllegalArgumentException();
    }
    return new Range(start, length + r.length);
  }
  
  public Range putAfter(Range base) {
    return new Range(base.start + base.length, length);
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Range) {
      Range other = (Range)obj;
      return (start == other.start) && (length == other.length);
    }
    return false; 
  }
  
  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public String toString() {
    return "["+start+" +"+length+"]";
  }
}
