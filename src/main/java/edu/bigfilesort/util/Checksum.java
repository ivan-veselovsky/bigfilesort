package edu.bigfilesort.util;

/**
 * Checksum that is a sorting invariant.
 * Now it contains length, xor and arithmetic sums of all the 
 * numbers in the file.
 */
public class Checksum {

  public final long length;
  public final int xor;
  public final int sum;
  
  Checksum(long length0, int xor0, int sum0) {
    length = length0;
    xor = xor0;
    sum = sum0;
  }
  
  @Override
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
