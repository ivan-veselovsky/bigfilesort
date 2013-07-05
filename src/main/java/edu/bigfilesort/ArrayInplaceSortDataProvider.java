package edu.bigfilesort;

import java.util.concurrent.atomic.AtomicLong;

public class ArrayInplaceSortDataProvider implements InplaceSortDataProvider {

  private final int[] array;
  private final AtomicLong numberOfGets = new AtomicLong(0);  
  private final AtomicLong numberOfExchanges = new AtomicLong(0);  
  
  public ArrayInplaceSortDataProvider(int[] array0) {
    array = array0;
  }
  
  protected int index(long interfaceIndex) {
    int i = (int)(--interfaceIndex);
    assert (i >=0 && i<array.length); // sanity check
    return i;
  }
  
  @Override
  public void exchange(long a, long b) {
    int ia = index(a);
    int ib = index(b);
    int t = array[ia];
    array[ia] = array[ib];
    array[ib] = t;
    numberOfExchanges.incrementAndGet();
  }

  @Override
  public int get(long a) {
    numberOfGets.incrementAndGet();
    return array[index(a)];
  }

  @Override
  public long getMaxIndex() {
    return array.length;
  }

  @Override
  public long numberOfGets() {
    return numberOfGets.get();
  }

  @Override
  public long numberOfExchanges() {
    return numberOfExchanges.get();
  }

}
