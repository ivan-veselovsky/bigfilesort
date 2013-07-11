package edu.bigfilesort.kmerge;

public interface OutStream {
  
  /**
   * puts next int.
   * Unless otherwise specified, assumed that numbers are put from lower indexes to higher.  
   * @param val
   */
  void putNext(int val);
}
