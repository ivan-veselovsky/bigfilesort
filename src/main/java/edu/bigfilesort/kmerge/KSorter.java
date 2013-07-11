package edu.bigfilesort.kmerge;

/**
 * Special sorter that is always able to get min element of <= K available elements.
 *  
 *
 */
public interface KSorter {
  
  void putValues(NumberContainer[] containers);
  
  /**
   * Re-sort to be able to get min element.
   */
  void reSort();
  
  /**
   * 1) Returns the current minimal element.
   * 2) reloads the min data container from the associated stream;
   * 3) if the min element got empty (no next value), then
   * removes it from the sorting data structure. 
   * @return the min element, or 'null' if no elements left (sorter is empty)
   */
  NumberContainer removeMinAndReload();
  
}
