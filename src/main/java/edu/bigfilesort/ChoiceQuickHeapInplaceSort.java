package edu.bigfilesort;

import java.util.concurrent.atomic.AtomicLong;

import static edu.bigfilesort.Util.*;

/*
 * We had to create own implementation because of 2 reasons:
 * 1) Arrays.sort() uses fallback to merge sort which requires additional arrays;
 * 2) We need to use byte buffers instead of arrays. 
 *  
 * The code of quicksort is slightly modified copy of JDK7 class java.util.DualPivotQuicksort.
 * -------------------------------------------------------------------
 * This class implements the Dual-Pivot Quicksort algorithm by
 * Vladimir Yaroslavskiy, Jon Bentley, and Josh Bloch. The algorithm
 * offers O(n log(n)) performance on many data sets that cause other
 * quicksorts to degrade to quadratic performance, and is typically
 * faster than traditional (one-pivot) Quicksort implementations.
 *
 * @author Vladimir Yaroslavskiy
 * @author Jon Bentley
 * @author Josh Bloch
 *
 * @version 2011.02.11 m765.827.12i:5\7pm
 * @since 1.7
 * --------------------------------------------------------------------
 */
public class ChoiceQuickHeapInplaceSort implements InplaceSort {
  
  /**
   * The maximum number of runs in merge sort.
   */
  private static final int MAX_RUN_COUNT = 67;

  /**
   * The maximum length of run in merge sort.
   */
  private static final int MAX_RUN_LENGTH = 33;

  /**
   * If the length of an array to be sorted is less than this
   * constant, Quicksort is used in preference to merge sort.
   */
  private static final int QUICKSORT_THRESHOLD = 286;

  
  private final InplaceSortDataProvider provider;
  private final AtomicLong comparisonCount = new AtomicLong(0); // TODO: prisobachit'
  private InplaceSort inplaceSortImpl; 
  
  public ChoiceQuickHeapInplaceSort(InplaceSortDataProvider p0) {
    provider = p0;
  }

  @Override
  public String getName() {
    return "Choice Heap/Quick";
  }
  
  @Override
  public long numberOfComparisons() {
    long count = comparisonCount.get();
    if (inplaceSortImpl != null) {
      count += inplaceSortImpl.numberOfComparisons(); 
    }
    return count;
  }
  
  /*
   * Makes heuristic analysis and chooses one of the sort providers: 
   * double-pivot quick or heap. 
   */
  @Override
  public void sort() {
    int min = provider.getMinIndex();
    int max= provider.getMaxIndex();
    Boolean q = useQuick(min, max);
    if (q == null) {
      assert printTrue("choice: already sorted.");
      return; // the array is already sorted, nothing to do.
    }
    if (q.booleanValue()) {
      assert printTrue("choice: use quick.");
      inplaceSortImpl = new DualPivotQuickSort(provider);
    } else {
      assert printTrue("choice: use heap.");
      inplaceSortImpl = new HeapSort(provider);
    }
    inplaceSortImpl.sort();
  }

  /**
   * Sorts the specified range of the array.
   *
   * @param a the array to be sorted
   * @param left the index of the first element, inclusive, to be sorted
   * @param right the index of the last element, inclusive, to be sorted
   */
  private Boolean useQuick(int left, int right) {
      // Use Quicksort on small arrays
      if (right - left < QUICKSORT_THRESHOLD) {
        //sort(a, left, right, true); 
        //return; 
        return Boolean.TRUE; // use quick
      }

      /*
       * Index run[i] is the start of i-th run
       * (ascending or descending sequence).
       */
      int[] run = new int[MAX_RUN_COUNT + 1];
      int count = 0; run[0] = left;

      // Check if the array is nearly sorted
      for (int k = left; k < right; run[count] = k) {
          if (provider.get(k) < provider.get(k + 1)) { // ascending
              while (++k <= right && provider.get(k - 1) <= provider.get(k));
          } else if (provider.get(k) > provider.get(k + 1)) { // descending
              while (++k <= right && provider.get(k - 1) >= provider.get(k));
              int t;
              for (int lo = run[count] - 1, hi = k; ++lo < --hi; ) {
                  t = provider.get(lo); provider.put(lo, provider.get(hi)); provider.put(hi, t);
              }
          } else { // equal
              for (int m = MAX_RUN_LENGTH; ++k <= right && provider.get(k - 1) == provider.get(k); ) {
                  if (--m == 0) {
//                    sort(a, left, right, true);
//                    return;
                      return Boolean.TRUE; // also quick
                  }
              }
          }

          /*
           * The array is not highly structured,
           * use Quicksort instead of merge sort.
           */
          if (++count == MAX_RUN_COUNT) {
//            sort(a, left, right, true);
//            return;
              return Boolean.TRUE; // also quick
          }
      }

      // Check special cases
      if (run[count] == right++) { // The last run contains one element
          run[++count] = right;
      } else if (count == 1) { // The array is already sorted
          return null; // *** NB
      }

      return Boolean.FALSE;
      
      // =================================================================
      // fallback to merge sort follows
//      /*
//       * Create temporary array, which is used for merging.
//       * Implementation note: variable "right" is increased by 1.
//       */
//      int[] b; byte odd = 0;
//      for (int n = 1; (n <<= 1) < count; odd ^= 1);
//
//      if (odd == 0) {
//          b = a; a = new int[b.length];
//          for (int i = left - 1; ++i < right; a[i] = b[i]);
//      } else {
//          b = new int[a.length];
//      }
//
//      // Merging
//      for (int last; count > 1; count = last) {
//          for (int k = (last = 0) + 2; k <= count; k += 2) {
//              int hi = run[k], mi = run[k - 1];
//              for (int i = run[k - 2], p = i, q = mi; i < hi; ++i) {
//                  if (q >= hi || p < mi && a[p] <= a[q]) {
//                      b[i] = a[p++];
//                  } else {
//                      b[i] = a[q++];
//                  }
//              }
//              run[++last] = hi;
//          }
//          if ((count & 1) != 0) {
//              for (int i = right, lo = run[count - 1]; --i >= lo;
//                  b[i] = a[i]
//              );
//              run[++last] = right;
//          }
//          int[] t = a; a = b; b = t;
//      }
  }
  
}
