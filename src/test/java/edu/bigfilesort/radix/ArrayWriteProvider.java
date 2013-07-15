package edu.bigfilesort.radix;

import java.io.IOException;

public class ArrayWriteProvider implements WriteProvider {

  private int[] arr;
  private int start;
  private int len;
  private int index;
  
  public ArrayWriteProvider(int[] arr0, int start0, int len0) {
    if (arr0 == null) {
      throw new NullPointerException();
    }
    if (start0 < 0 || len0 < 0) {
      throw new IllegalArgumentException("negative.");
    }
    arr = arr0;
    start = start0;
    len = len0;
    index = start;
    if ((start + len) > arr.length) {
      throw new IllegalArgumentException("last pos behind the end of array.");
    }
  }

  @Override
  public void put(int nextValue) throws IOException {
    if (index >= (start + len)) {
      throw new IllegalStateException("Cannot write to a position behing the end of the array.");
    }
    arr[index] = nextValue;
    index++;
  }

  @Override
  public void flush() {
  }
  
  @Override
  public void dispose() {
    arr = null;
  }
  
}
