package edu.bigfilesort.kmerge;

/**
 * Wrapper around mutable numeric value with a constant reference to 
 * the input stream where next value should be taken.   
 */
public class NumberContainer {

  final InStream inStream;
  int number = 0;
  boolean hasNext = true; 
  
  public NumberContainer(InStream is) {
    if (is == null) { throw new NullPointerException(); }
    inStream = is;
  }
  
  /**
   * loads next element from the stream to the 'value' field, 
   * if the one is available; If not, does nothing.  
   */
  public final boolean loadNext() {
    if (hasNext) {
      hasNext = inStream.hasNext();
      if (hasNext) {
        number = inStream.getNext();
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

}
