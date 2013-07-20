package edu.bigfilesort.util;

import java.io.IOException;

import edu.bigfilesort.radix.ReadProvider;
import edu.bigfilesort.radix.Storage;

/**
 * Creates {@link Checksum}-s using different sources. 
 */
public class ChecksumBuilder {
  
  public static Checksum calculateChecksum(int[] array) {
    ChecksumBuilder builder = new ChecksumBuilder();
    for (int v: array) {
      builder.next(v);
    }
    return builder.getChecksum();
  }
  
  public static Checksum calculateChecksum(Storage storage, int buf) throws IOException {
    ReadProvider rp = storage.createReadProvider(0, storage.length(), buf);
    Checksum sum = calculateChecksum(rp);
    rp.dispose();
    return sum;
  }
  
  private static Checksum calculateChecksum(ReadProvider rp) throws IOException {
    ChecksumBuilder builder = new ChecksumBuilder();
    int v;
    while (rp.hasNext()) {
      v = rp.next();
      builder.next(v);
    }
    return builder.getChecksum();
  }

  private int xor = 0;
  private int sum = 0;
  private long length = 0L; 
  
  public ChecksumBuilder() {
  }
  
  public ChecksumBuilder next(int v) {
    xor ^= v;
    sum += v;
    length++;
    return this;
  }  
  
  public Checksum getChecksum() {
    return new Checksum(length, xor, sum);
  }

}
