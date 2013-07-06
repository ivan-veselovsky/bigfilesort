package edu.bigfilesort;

import java.util.concurrent.atomic.AtomicLong;

/*
 * The code is slightly modified copy of JDK class java.util.DualPivotQuicksort.
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
public class DualPivotQuickSort implements InplaceSort {

  private final InplaceSortDataProvider provider;
  private final AtomicLong comparisonCount = new AtomicLong(0); // TODO: prisobachit'
  
  public DualPivotQuickSort(InplaceSortDataProvider p0) {
    provider = p0;
  }
  
  @Override
  public String getName() {
    return "Dual-Pivot Quicksort";
  }
  
  @Override
  public long numberOfComparisons() {
    return comparisonCount.get();
  }
  
  @Override
  public void sort() {
    int min = provider.getMinIndex(); 
    int max = provider.getMaxIndex(); 
    sort(min, max, true/*leftmost*/);
  }

  // -------------------------------------------------------------
  
  /**
   * If the length of an array to be sorted is less than this
   * constant, insertion sort is used in preference to Quicksort.
   */
  private static final int INSERTION_SORT_THRESHOLD = 47;
  
  /**
   * Sorts the specified range of the array by Dual-Pivot Quicksort.
   *
   * @param a the array to be sorted
   * @param left the index of the first element, inclusive, to be sorted
   * @param right the index of the last element, inclusive, to be sorted
   * @param leftmost indicates if this part is the leftmost in the range
   */
  private void sort(/*int[] a, */int left, int right, boolean leftmost) {
      int length = right - left + 1;

      // Use insertion sort on tiny arrays
      if (length < INSERTION_SORT_THRESHOLD) {
          if (leftmost) {
              /*
               * Traditional (without sentinel) insertion sort,
               * optimized for server VM, is used in case of
               * the leftmost part.
               */
              for (int i = left, j = i; i < right; j = ++i) {
                  int ai = /*a[i + 1]*/provider.get(i + 1);
                  while (ai < /*a[j]*/provider.get(j)) {
                      provider.put(j + 1, provider.get(j));
                      if (j-- == left) {
                          break;
                      }
                  }
                  /*a[j + 1] = ai;*/provider.put(j + 1, ai);
              }
          } else {
              /*
               * Skip the longest ascending sequence.
               */
              do {
                  if (left >= right) {
                      return;
                  }
              } while (provider.get(++left) >= provider.get(left - 1));

              /*
               * Every element from adjoining part plays the role
               * of sentinel, therefore this allows us to avoid the
               * left range check on each iteration. Moreover, we use
               * the more optimized algorithm, so called pair insertion
               * sort, which is faster (in the context of Quicksort)
               * than traditional implementation of insertion sort.
               */
              for (int k = left; ++left <= right; k = ++left) {
                  int a1 = provider.get(k), a2 = provider.get(left);

                  if (a1 < a2) {
                      a2 = a1; a1 = provider.get(left);
                  }
                  while (a1 < provider.get(--k)) {
                      provider.put(k + 2, provider.get(k));
                  }
                  provider.put(++k + 1, a1);

                  while (a2 < provider.get(--k)) {
                      provider.put(k + 1, provider.get(k));
                  }
                  provider.put(k + 1, a2);
              }
              int last = provider.get(right);

              while (last < provider.get(--right)) {
                  provider.put(right + 1, provider.get(right));
              }
              provider.put(right + 1, last);
          }
          return;
      }

      // Inexpensive approximation of length / 7
      int seventh = (length >> 3) + (length >> 6) + 1;

      /*
       * Sort five evenly spaced elements around (and including) the
       * center element in the range. These elements will be used for
       * pivot selection as described below. The choice for spacing
       * these elements was empirically determined to work well on
       * a wide variety of inputs.
       */
      int e3 = (left + right) >>> 1; // The midpoint
      int e2 = e3 - seventh;
      int e1 = e2 - seventh;
      int e4 = e3 + seventh;
      int e5 = e4 + seventh;

      // Sort these elements using insertion sort
      if (provider.get(e2) < provider.get(e1)) { int t = provider.get(e2); provider.put(e2, provider.get(e1)); provider.put(e1, t); }

      if (provider.get(e3) < provider.get(e2)) { int t = provider.get(e3); provider.put(e3, provider.get(e2)); provider.put(e2, t);
          if (t < provider.get(e1)) { provider.put(e2, provider.get(e1)); provider.put(e1, t); }
      }
      if (provider.get(e4) < provider.get(e3)) { int t = provider.get(e4); provider.put(e4, provider.get(e3)); provider.put(e3, t);
          if (t < provider.get(e2)) { provider.put(e3, provider.get(e2)); provider.put(e2, t);
              if (t < provider.get(e1)) { provider.put(e2, provider.get(e1)); provider.put(e1, t); }
          }
      }
      if (provider.get(e5) < provider.get(e4)) { int t = provider.get(e5); provider.put(e5, provider.get(e4)); provider.put(e4, t);
          if (t < provider.get(e3)) { provider.put(e4, provider.get(e3)); provider.put(e3, t);
              if (t < provider.get(e2)) { provider.put(e3, provider.get(e2)); provider.put(e2, t);
                  if (t < provider.get(e1)) { provider.put(e2, provider.get(e1)); provider.put(e1, t); }
              }
          }
      }

      // Pointers
      int less  = left;  // The index of the first element of center part
      int great = right; // The index before the first element of right part

      if (provider.get(e1) != provider.get(e2) && provider.get(e2) != provider.get(e3) && provider.get(e3) != provider.get(e4) && provider.get(e4) != provider.get(e5)) {
          /*
           * Use the second and fourth of the five sorted elements as pivots.
           * These values are inexpensive approximations of the first and
           * second terciles of the array. Note that pivot1 <= pivot2.
           */
          int pivot1 = provider.get(e2);
          int pivot2 = provider.get(e4);

          /*
           * The first and the last elements to be sorted are moved to the
           * locations formerly occupied by the pivots. When partitioning
           * is complete, the pivots are swapped back into their final
           * positions, and excluded from subsequent sorting.
           */
          provider.put(e2, provider.get(left));
          provider.put(e4, provider.get(right));

          /*
           * Skip elements, which are less or greater than pivot values.
           */
          while (provider.get(++less) < pivot1);
          while (provider.get(--great) > pivot2);

          /*
           * Partitioning:
           *
           *   left part           center part                   right part
           * +--------------------------------------------------------------+
           * |  < pivot1  |  pivot1 <= && <= pivot2  |    ?    |  > pivot2  |
           * +--------------------------------------------------------------+
           *               ^                          ^       ^
           *               |                          |       |
           *              less                        k     great
           *
           * Invariants:
           *
           *              all in (left, less)   < pivot1
           *    pivot1 <= all in [less, k)     <= pivot2
           *              all in (great, right) > pivot2
           *
           * Pointer k is the first index of ?-part.
           */
          outer:
          for (int k = less - 1; ++k <= great; ) {
              int ak = provider.get(k);
              if (ak < pivot1) { // Move a[k] to left part
                  provider.put(k, provider.get(less));
                  /*
                   * Here and below we use "a[i] = b; i++;" instead
                   * of "a[i++] = b;" due to performance issue.
                   */
                  provider.put(less, ak);
                  ++less;
              } else if (ak > pivot2) { // Move a[k] to right part
                  while (provider.get(great) > pivot2) {
                      if (great-- == k) {
                          break outer;
                      }
                  }
                  if (provider.get(great) < pivot1) { // a[great] <= pivot2
                      provider.put(k, provider.get(less));
                      provider.put(less, provider.get(great));
                      ++less;
                  } else { // pivot1 <= a[great] <= pivot2
                      provider.put(k, provider.get(great));
                  }
                  /*
                   * Here and below we use "a[i] = b; i--;" instead
                   * of "a[i--] = b;" due to performance issue.
                   */
                  provider.put(great, ak);
                  --great;
              }
          }

          // Swap pivots into their final positions
          provider.put(left, provider.get(less  - 1)); provider.put(less  - 1, pivot1);
          provider.put(right, provider.get(great + 1)); provider.put(great + 1, pivot2);

          // Sort left and right parts recursively, excluding known pivots
          sort(/*a,*/ left, less - 2, leftmost);
          sort(/*a,*/ great + 2, right, false);

          /*
           * If center part is too large (comprises > 4/7 of the array),
           * swap internal pivot values to ends.
           */
          if (less < e1 && e5 < great) {
              /*
               * Skip elements, which are equal to pivot values.
               */
              while (provider.get(less) == pivot1) {
                  ++less;
              }

              while (provider.get(great) == pivot2) {
                  --great;
              }

              /*
               * Partitioning:
               *
               *   left part         center part                  right part
               * +----------------------------------------------------------+
               * | == pivot1 |  pivot1 < && < pivot2  |    ?    | == pivot2 |
               * +----------------------------------------------------------+
               *              ^                        ^       ^
               *              |                        |       |
               *             less                      k     great
               *
               * Invariants:
               *
               *              all in (*,  less) == pivot1
               *     pivot1 < all in [less,  k)  < pivot2
               *              all in (great, *) == pivot2
               *
               * Pointer k is the first index of ?-part.
               */
              outer:
              for (int k = less - 1; ++k <= great; ) {
                  int ak = provider.get(k);
                  if (ak == pivot1) { // Move a[k] to left part
                      provider.put(k, provider.get(less));
                      provider.put(less, ak);
                      ++less;
                  } else if (ak == pivot2) { // Move a[k] to right part
                      while (provider.get(great) == pivot2) {
                          if (great-- == k) {
                              break outer;
                          }
                      }
                      if (provider.get(great) == pivot1) { // a[great] < pivot2
                          provider.put(k, provider.get(less));
                          /*
                           * Even though a[great] equals to pivot1, the
                           * assignment a[less] = pivot1 may be incorrect,
                           * if a[great] and pivot1 are floating-point zeros
                           * of different signs. Therefore in float and
                           * double sorting methods we have to use more
                           * accurate assignment a[less] = a[great].
                           */
                          provider.put(less, pivot1);
                          ++less;
                      } else { // pivot1 < a[great] < pivot2
                          provider.put(k, provider.get(great));
                      }
                      provider.put(great, ak);
                      --great;
                  }
              }
          }

          // Sort center part recursively
          sort(/*a,*/ less, great, false);

      } else { // Partitioning with one pivot
          /*
           * Use the third of the five sorted elements as pivot.
           * This value is inexpensive approximation of the median.
           */
          int pivot = provider.get(e3);

          /*
           * Partitioning degenerates to the traditional 3-way
           * (or "Dutch National Flag") schema:
           *
           *   left part    center part              right part
           * +-------------------------------------------------+
           * |  < pivot  |   == pivot   |     ?    |  > pivot  |
           * +-------------------------------------------------+
           *              ^              ^        ^
           *              |              |        |
           *             less            k      great
           *
           * Invariants:
           *
           *   all in (left, less)   < pivot
           *   all in [less, k)     == pivot
           *   all in (great, right) > pivot
           *
           * Pointer k is the first index of ?-part.
           */
          for (int k = less; k <= great; ++k) {
              if (provider.get(k) == pivot) {
                  continue;
              }
              int ak = provider.get(k);
              if (ak < pivot) { // Move a[k] to left part
                  provider.put(k, provider.get(less));
                  provider.put(less, ak);
                  ++less;
              } else { // a[k] > pivot - Move a[k] to right part
                  while (provider.get(great) > pivot) {
                      --great;
                  }
                  if (provider.get(great) < pivot) { // a[great] <= pivot
                      provider.put(k, provider.get(less));
                      provider.put(less, provider.get(great));
                      ++less;
                  } else { // a[great] == pivot
                      /*
                       * Even though a[great] equals to pivot, the
                       * assignment a[k] = pivot may be incorrect,
                       * if a[great] and pivot are floating-point
                       * zeros of different signs. Therefore in float
                       * and double sorting methods we have to use
                       * more accurate assignment a[k] = a[great].
                       */
                      provider.put(k, pivot);
                  }
                  provider.put(great, ak);
                  --great;
              }
          }

          /*
           * Sort left and right parts recursively.
           * All elements from center part are equal
           * and, therefore, already sorted.
           */
          sort(/*a,*/ left, less - 1, leftmost);
          sort(/*a,*/ great + 1, right, false);
      }
  }

}
