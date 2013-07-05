package edu.bigfilesort;

import java.io.IOException;

/**
 * Used in Net Sorting, where each comparator 
 * compares and swaps (if needed) 2 values. 
 */
public interface Comparator {

  /**
   * Compares and optionally swaps 2 elements at data positions a and b.
   * Note that data position is not the same as byte position.
   * @param numPosA
   * @param numPosB
   */
  void compareAndSwap(long numPosA, long numPosB) throws IOException;
  
  long getCmpCount();
}
