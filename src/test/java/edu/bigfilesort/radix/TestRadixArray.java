package edu.bigfilesort.radix;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import edu.bigfilesort.Util;
import edu.bigfilesort.UtilForTest;

public class TestRadixArray {

  @Before
  public void before() {
    assert Util.assertionsEnabled();
  }
  
  void testImpl(int[] arr) throws IOException {
    Storage mainStorage = new ArrayStorage(arr);
    assertEquals(arr.length, mainStorage.length());
    int[] tmp = new int[arr.length];
    UtilForTest.fillArrayRandom(tmp, 0x1234567890ABCDEFL);
    Storage tmpStorage = new ArrayStorage(tmp);
    assertEquals(arr.length, tmpStorage.length());
    final RadixSort radix = new RadixSort(mainStorage, tmpStorage);
    radix.sort(0);
    UtilForTest.assertArraySorted(arr);
  }
  
  @Test
  public void testRadixSortSimpleForward() throws Exception {
    final int[] array = new int[] { 
        Integer.MAX_VALUE,  
        Integer.MAX_VALUE >> 24,  
        Integer.MAX_VALUE >> 16,  
        Integer.MAX_VALUE >> 8,  
        0, 
        Integer.MIN_VALUE >> 8,
        Integer.MIN_VALUE >> 16,
        Integer.MIN_VALUE >> 24,
        Integer.MIN_VALUE,
        };
    testImpl(array);
  }

  static final int bigArrLen = 1024 * 1024 * 28; // 7
  
  @Test
  public void testRadixSortBigForward() throws Exception {
    final int[] array = new int[bigArrLen]; 
    UtilForTest.fillArrayRandom(array, 0x1234567890ABCDEFL);
    testImpl(array);
  }
  
}
