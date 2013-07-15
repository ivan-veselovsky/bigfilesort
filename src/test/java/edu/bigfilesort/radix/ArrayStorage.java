package edu.bigfilesort.radix;

import java.io.IOException;

public class ArrayStorage implements Storage {

  private int[] arr;
  
  public ArrayStorage(int[] arr0) {
    arr = arr0;
  }

  @Override
  public long length() {
    return arr.length;
  }

  @Override
  public ReadProvider createReadProvider(long start, long len, int bufSize) {
    return new ArrayReadProvider(arr, (int)start, (int)len);
  }

  @Override
  public WriteProvider createWriteProvider(long start, long len, int bufSize) {
    return new ArrayWriteProvider(arr, (int)start, (int)len);
  }
  
  @Override
  public void flush() throws IOException {
  }
  
  @Override
  public void close() {
    arr = null;
  }

}
