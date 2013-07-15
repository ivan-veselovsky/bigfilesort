package edu.bigfilesort.radix;

import java.io.IOException;

public class ArrayReadProvider implements ReadProvider {

  int[] arr;
  final int start;
  final int len;
  int index;
  int lastPosPlusOne; 
  
  public ArrayReadProvider(int[] arr0, int start0, int len0) {
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
    lastPosPlusOne = start + len;
    if (lastPosPlusOne > arr.length) {
      throw new IllegalArgumentException("last pos behind the end of array.");
    }
  }
  
  @Override
  public boolean hasNext() throws IOException {
    return (index < lastPosPlusOne);
  }
  
  @Override
  public int next() {
    return arr[index++];
  }
  
  @Override
  public long length() {
    return len;
  }
  
//  @Override
//  public void reset() throws IOException {
//    index = start;
//  }
  
  @Override
  public void dispose() {
    arr = null;
  }

}
