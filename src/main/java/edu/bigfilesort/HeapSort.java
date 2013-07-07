package edu.bigfilesort;

import java.util.concurrent.atomic.AtomicLong;

/*
 * The code is slightly optimized code from book
 * T.H.Cormen C.E.Leiserson, R.L.Rivest, page 135.
 * 
 * NB: the indexes in the algorithm implementation are 1-based.
 */
public class HeapSort implements InplaceSort {

   private final InplaceSortDataProvider dataProvider;
   private final int length; // full length of the array to be sorted.
   
   private final AtomicLong cmpCount = new AtomicLong(0); // diagnostic only
   
   private int heapSize; // the size of the heap (variable), 1 <= heapSize <= length
   
   public HeapSort(InplaceSortDataProvider provider0) {
     dataProvider = provider0;
     length = dataProvider.getMaxIndex();
     if (dataProvider.getMinIndex() != 1) {
       throw new IllegalArgumentException("Min index required to be 1: " + dataProvider.getMinIndex());
     }
     heapSize = length;
   }
   
   @Override
  public String getName() {
    return "HEAP";
  }
   
   /*
    * NB: heapSize field must be set correctly prio to this call.
    */
   private void heapify(final int parentIndex) {
      int leftIndex = parentIndex << 1;
      int rightIndex = leftIndex + 1;
      // invariant: heapSize >= right >= left
      if (leftIndex <= heapSize) {
        int maxValue = dataProvider.get(leftIndex);   // read #1
        int maxIndex = leftIndex;
        if (rightIndex <= heapSize) {
          int rightValue  = dataProvider.get(rightIndex); // read #2
          if (Main.countersEnabled) { cmpCount.incrementAndGet(); }
          // NB: in case of value equality prefer right choice:
          if (rightValue >= maxValue) { // cmp #1
            maxIndex = rightIndex;
            maxValue = rightValue;  
          }
        }
        // now maxValue is the max of the 2 kids.
        int parentValue;
        if (Main.countersEnabled) {  cmpCount.incrementAndGet(); }  
        if (maxValue > (parentValue = dataProvider.get(parentIndex))) { // read #3, cmp #2
          // exchange: parentIndex <-> maxIndex 
          dataProvider.put(parentIndex, maxValue); // write #1
          dataProvider.put(maxIndex, parentValue); // write #2
          heapify(maxIndex); // *** recursive call: restore the heap property where it might be broken.
        }
      }
   }

   @Override
   public void sort() {
     buildHeap();
     int valueOne, valueI;
     for (int i=length; i>=2; i--) {
       heapSize--;
       valueOne = dataProvider.get(1);
       valueI = dataProvider.get(i);
       // optimization: do not exchange and re-heapify if the values are equal:
       if (valueOne != valueI) {
         dataProvider.put(1, valueI);
         dataProvider.put(i, valueOne);
         heapify(1); // NB: heapify() does not modify heapSize
       }
     }
   }
   
   private void buildHeap() {
     heapSize = length;
     for (int i=length/2; i>=1; i--) {
       heapify(i);
     }
   }
   
   @Override
   public long numberOfComparisons() {
     return cmpCount.get();
   }
}
