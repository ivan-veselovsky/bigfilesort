package edu.bigfilesort;


public class TestHeapSort extends AbstractTestInplaceSort {

  @Override
  protected InplaceSort createInplaceSort(InplaceSortDataProvider provider) {
    return new HeapSort(provider);
  }
  
}
