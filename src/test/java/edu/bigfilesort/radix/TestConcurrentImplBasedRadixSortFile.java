package edu.bigfilesort.radix;

import java.io.IOException;

public class TestConcurrentImplBasedRadixSortFile extends TestRadixFile {

  protected void sortImpl(Storage mainStorage, Storage tmpStorage, long bufLen) throws IOException {
    final ConcurrentImplBasedRadixSort radix = new ConcurrentImplBasedRadixSort(mainStorage, tmpStorage, 2);
    radix.sort(bufLen);
  }
  
}
