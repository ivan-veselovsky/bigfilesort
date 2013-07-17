package edu.bigfilesort.radix;

import java.io.IOException;

public class TestConcurrentImplBasedRadixSortArray extends TestRadixArray {

  @Override
  protected void sortImpl(Storage mainStorage, Storage tmpStorage)
      throws IOException {
    final ConcurrentImplBasedRadixSort radix = new ConcurrentImplBasedRadixSort(mainStorage, tmpStorage, 5);
    radix.sort(0);
  }
  
}
