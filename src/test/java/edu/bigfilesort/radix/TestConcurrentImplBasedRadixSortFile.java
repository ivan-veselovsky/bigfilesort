package edu.bigfilesort.radix;

import java.io.IOException;

public class TestConcurrentImplBasedRadixSortFile extends TestRadixFile {

  private final int threads = 5;
  
  @Override
  protected void sortImpl(Storage mainStorage, Storage tmpStorage, long bufLen) throws IOException {
    final ConcurrentImplBasedRadixSort radix = new ConcurrentImplBasedRadixSort(mainStorage, tmpStorage, threads);
    radix.sort(bufLen);
  }
  
}
