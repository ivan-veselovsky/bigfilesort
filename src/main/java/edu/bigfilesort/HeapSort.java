package edu.bigfilesort;

import static edu.bigfilesort.Util.*;

/**
 * NB: the indexes in the algorithm implementation are 1-based.
 */
public class HeapSort {

   private final InplaceSortDataProvider cmp;
   private final long length; // full length of the array to be sorted.
   //private final int height; // height of the heap
   private long heapSize; // the size of the heap (variable), 1 <= heapSize <= length 
   
   public HeapSort(InplaceSortDataProvider provider0) {
     cmp = provider0;
     length = cmp.getMaxIndex();
     heapSize = length;
     //height = log2plus(length);
   }
   
   /*
    * NB: heapSize field must be set correctly!
    */
   private void heapify(final long i) {
      long left = i << 1;
      long right = left + 1;
      
      // TODO: reduce get() invocations: 4 are superfluous there, 3 is enough.
      // TODO: cmp kids between each other, after that cmp with the parent. If the kids are equal, prefer the rightmost kid.
      // TODO: 3rd if is not necessary also. Reduce it.
      long largest;
      if (left <= heapSize && (cmp.get(left) > cmp.get(i))) {
        largest = left;
      } else {
        largest = i;
      }
      if (right <= heapSize && (cmp.get(right) > cmp.get(largest))) {
        largest = right;
      }
      if (largest != i) {
        cmp.exchange(i, largest);
        heapify(largest); // *** recursive
      }
   }

   /**
    * Entry point
    */
   public void sort() {
     buildHeap();
     for (long i=length; i>=2; i--) {
       cmp.exchange(1, i);
       heapSize--;
       heapify(1);
     }
   }
   
   private void buildHeap() {
     heapSize = length;
     for (long i=length/2; i>=1; i--) {
       heapify(i);
     }
   }
   
//   private long left(long i) {
//      return i << 1;
//   }
//   
//   private long right(long i) {
//      return (i << 1) + 1;
//   }

}
