package edu.bigfilesort;

import java.util.concurrent.atomic.AtomicLong;


public class ArrayInplaceSortDataProvider implements InplaceSortDataProvider {

  private int[] array;
  private final int indexShift;
  private final AtomicLong numberOfReads = new AtomicLong(0);
  private final AtomicLong numberOfWrites = new AtomicLong(0);
  
  /*
   * All index args in this class == "virtual" indexes.
   * Rule:
   * "physical" index (index in the array passed in) == "virtual" index + indexShift.
   */
  public ArrayInplaceSortDataProvider(int[] array0, int indexShift0) {
    array = array0;
    indexShift = indexShift0;
  }

  @Override
  public int get(int a) {
    if (Main.countersEnabled) { numberOfReads.incrementAndGet(); }
    return array[a + indexShift];
  }
  
  @Override
  public void put(int a, int v) {
    if (Main.countersEnabled) { numberOfWrites.incrementAndGet(); }
    array[a + indexShift] = v;
  }

  @Override
  public int getMaxIndex() {
    return (array.length - 1 - indexShift);
  }

  @Override
  public int getMinIndex() {
    return (-indexShift);
  }
  
  @Override
  public long numberOfReads() {
    return numberOfReads.get();
  }

  @Override
  public long numberOfWrites() {
    return numberOfWrites.get();
  }

  @Override
  public void force() {
  }
  
  @Override
  public void dispose() {
    // just free the array to be GC-ed:
    array = null;
  }
}
