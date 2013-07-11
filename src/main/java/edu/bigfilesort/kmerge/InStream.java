package edu.bigfilesort.kmerge;

public interface InStream {

  /**
   * if next number is available
   * @return
   */
  boolean hasNext();

  /**
   * gets next int number
   * Unless otherwise specified, assumed that numbers are taken from lower indexes to higher.
   * @return
   */
  int getNext();
  
}
