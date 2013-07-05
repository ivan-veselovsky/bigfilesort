package edu.bigfilesort;

import java.util.Arrays;

import org.junit.Test;
import static org.junit.Assert.*;
import static java.lang.System.out;

// -Xms131m -Xmx131m -XX:NewRatio=81 -XX:MaxPermSize=1m
public class TestHeapSort {

  //NB: not necessarily power of 2:
  private static final int arrayLength = 1024 * 1024 * 16;
  
  private static int[] array = new int[arrayLength];
  private static int[] array2 = new int[arrayLength];
  
  @Test
  public void testArray10() {
    out.println("==== testArray10");
    // Example from the book Cormen, Leiserson, Rivest, page 144:
    int[] arr = new int[] { 4, 1, 3, 2, 16, 9, 10, 14, 8, 7 };
    int[] arr2 = Arrays.copyOf(arr, arr.length);
    arraySortImpl(arr, arr2);
  }

  @Test
  public void testArrayAscending() {
    out.println("==== testArrayAscending");
    UtilForTest.fillArray(array, 0, 3);
    UtilForTest.fillArray(array2, 0, 3);
    arraySortImpl(array, array2);
  }
    
  @Test
  public void testArrayDescending() {
    out.println("==== testArrayDescending");
    UtilForTest.fillArray(array, 0, -3);
    UtilForTest.fillArray(array2, 0, -3);
    arraySortImpl(array, array2);
  }

  @Test
  public void testArrayFlat() {
    out.println("==== testArrayFlat");
    UtilForTest.fillArray(array, Integer.MIN_VALUE, 0);
    UtilForTest.fillArray(array2, Integer.MIN_VALUE, 0);
    arraySortImpl(array, array2);
  }
  
  @Test
  public void testArrayRandom() {
    out.println("==== testArrayRandom");
    long seed = -System.currentTimeMillis() * 37;
    UtilForTest.fillArrayRandom(array, seed);
    UtilForTest.fillArrayRandom(array2, seed);
    arraySortImpl(array, array2);
  }
  
  private void arraySortImpl(int arr1[], int[] arr2) {
    assertEquals(arr1.length , arr2.length);
    assertNotSame(arr1, arr2);
    assertArrayEquals(arr1, arr2);

    {
    InplaceSortDataProvider provider = new ArrayInplaceSortDataProvider(arr1);
    HeapSort heapSort = new HeapSort(provider);
    long t = System.currentTimeMillis();
    heapSort.sort();
    long d = System.currentTimeMillis() - t;
    UtilForTest.assertArraySorted(arr1);
    final long nLogN = arr1.length * Util.log2plus(arr1.length);
    out.println("n * log2(n) = " + nLogN);
    out.println("Heap sort gets      = " + ((double)provider.numberOfGets()) / nLogN);
    out.println("Heap sort exchanges = " + ((double)provider.numberOfExchanges()) / nLogN);
    out.println("Heap sort time     = " + d + " ms");
    }
    
    {
    long t2 = System.currentTimeMillis();
    Arrays.sort(arr2);
    long d2 = System.currentTimeMillis() - t2;
    UtilForTest.assertArraySorted(arr2);
    out.println("Arrays.sort() time = " + d2 + " ms");
    }
    
    System.gc();
  }
}
