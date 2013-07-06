package edu.bigfilesort;

public class TestByteBufferChoiceSort extends AbstractTestByteBufferInplaceSort {

  @Override
  protected InplaceSort createInplaceSort(InplaceSortDataProvider provider) {
    return new ChoiceQuickHeapInplaceSort(provider);
  }

}
