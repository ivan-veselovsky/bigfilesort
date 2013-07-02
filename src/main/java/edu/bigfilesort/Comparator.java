package edu.bigfilesort;

import java.io.IOException;

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
