package edu.bigfilesort.radix;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import edu.bigfilesort.Util;
import edu.bigfilesort.UtilForTest;
import edu.bigfilesort.util.Checksum;
import edu.bigfilesort.util.ChecksumBuilder;

public class TestRadixArray {

  @Before
  public void before() {
    assert Util.assertionsEnabled();
  }
  
  void testImpl(final int[] arr) throws IOException {
    final Checksum sum0 = ChecksumBuilder.calculateChecksum(arr);
    System.out.println(sum0);
    
    Storage mainStorage = new ArrayStorage(arr);
    assertEquals(arr.length, mainStorage.length());
    int[] tmp = new int[arr.length];
    UtilForTest.fillArrayRandom(tmp, 0x1234567890ABCDEFL);
    Storage tmpStorage = new ArrayStorage(tmp);
    
    sortImpl(mainStorage, tmpStorage);
    
    assertEquals(arr.length, tmpStorage.length());
    final Checksum sum1 = ChecksumBuilder.calculateChecksum(arr);
    System.out.println(sum1);
    UtilForTest.assertArraySorted(arr); // verify sorting
    assertEquals(sum0, sum1); // verify checksum
  }
  
  protected void sortImpl(Storage mainStorage, Storage tmpStorage) throws IOException {
    final RadixSort radix = new RadixSort(mainStorage, tmpStorage);
    radix.sort(0);
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
