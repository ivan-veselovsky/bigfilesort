package edu.bigfilesort;

/**
 * Represents a ~universal provider of data for in-place sorting. 
 * Note that indexes (a,b) by default should be treated as 1-based [1..getMaxIndex()], 
 * but may be treated differently by the implementing classes, in which case this 
 * should be documented.
 */
public interface InplaceSortDataProvider {

  /**
   * Unconditionally exchanges 2 elements at the given indexes.
   * @param a
   * @param b
   */
  void exchange(long a, long b);
  
  /**
   * Gets the data element at the given index.
   * @param a
   * @return the element
   */
  int get(long a);
  
  /**
   * Gets max index of the data provider (inclusive).
   * Result of {@link #get(long)} or {@link #exchange(long, long)} with an 
   * arg greater than this index is undefined.
   * @return max index
   */
  long getMaxIndex();
  
  // ----------------------------
  // Diagnostic methods:
  
  long numberOfGets();
  
  long numberOfExchanges();
  
}
