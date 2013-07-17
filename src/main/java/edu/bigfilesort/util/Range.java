package edu.bigfilesort.util;

public final class Range {
  
  public final long start; // inclusive
  public final long length; // length of the range
  
  public Range(long start0, long length0) {
    start = start0;
    length = length0;
  }
  
  public boolean contains(long x) {
    return (x >= start) && (x < (start + length));
  } 
  
  @Override
  public String toString() {
    return "["+start+" +"+length+"]";
  }
}
