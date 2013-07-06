package edu.bigfilesort;

public class TestChoiceSort extends AbstractTestInplaceSort {

  @Override
  protected InplaceSort createInplaceSort(InplaceSortDataProvider provider) {
    return new ChoiceQuickHeapInplaceSort(provider);
  }
  
}
