package edu.bigfilesort;

public class TestDualPivotQuickSort extends AbstractTestInplaceSort {

  @Override
  protected InplaceSort createInplaceSort(InplaceSortDataProvider provider) {
    return new DualPivotQuickSort(provider);
  }
  
}
