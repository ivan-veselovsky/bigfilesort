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
  void exchange(int a, int b);
  
  /**
   * Gets the data element at the given index.
   * @param a
   * @return the element
   */
  int get(int a);
  
  /**
   * Unconditionally puts given value at given index. 
   * @param a
   * @param v
   */
  void put(int a, int v);
  
  /**
   * Gets max index of the data provider (inclusive).
   * Result of {@link #get(long)} or {@link #exchange(long, long)} with an 
   * arg greater than this index is undefined.
   * @return max index
   */
  int getMaxIndex();

  int getMinIndex();
  
  // ----------------------------
  // Diagnostic methods:
  
  long numberOfReads();
  
  long numberOfWrites();
  
}
