package edu.bigfilesort;

public class TestByteBufferHeapSort extends AbstractTestByteBufferInplaceSort {

  @Override
  protected InplaceSort createInplaceSort(InplaceSortDataProvider provider) {
    return new HeapSort(provider);
  }

}
