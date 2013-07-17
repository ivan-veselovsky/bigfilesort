package edu.bigfilesort.util;

import java.io.IOException;

import edu.bigfilesort.radix.ReadProvider;
import edu.bigfilesort.radix.Storage;

public class Checksum {

  private final long length;
  private final int xor;
  private final int sum;
  
  private Checksum(long length0, int xor0, int sum0) {
    length = length0;
    xor = xor0;
    sum = sum0;
  }
  
  public static Checksum calculateChecksum(int[] array) {
    int xor = 0;
    int sum = 0;
    for (int v: array) {
      xor ^= v;
      sum += v;
    }
    return new Checksum(array.length, xor, sum);
  }
  
  public static Checksum calculateChecksum(Storage storage, int buf) throws IOException {
    ReadProvider rp = storage.createReadProvider(0, storage.length(), buf);
    Checksum sum = calculateChecksum(rp);
    rp.dispose();
    return sum;
  }
  
  private static Checksum calculateChecksum(ReadProvider rp) throws IOException {
    int xor = 0;
    int sum = 0;
    int v;
    final long l = rp.length();
    while (rp.hasNext()) {
      v = rp.next();
      xor ^= v;
      sum += v;
    }
    return new Checksum(l, xor, sum);
  }
  
  public boolean equals(Object o) {
    if (o instanceof Checksum) {
      Checksum other = (Checksum)o;
      return (length == other.length) 
          && (xor == other.xor) 
          && (sum == other.sum);
    }
    return false;
  }
  
  @Override
  public int hashCode() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "Length=" + Long.toHexString(length) + " Xor=" + Integer.toHexString(xor) + " Sum=" +Integer.toHexString(sum);
  }
}
