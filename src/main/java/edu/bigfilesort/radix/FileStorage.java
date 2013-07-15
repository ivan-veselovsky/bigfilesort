package edu.bigfilesort.radix;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import edu.bigfilesort.Main;
import edu.bigfilesort.Util;

public class FileStorage implements Storage {

  private final String fileName;
  private RandomAccessFile raf;
  private FileChannel fc; 
  private final long numLength;
  
  /**
   * Creates storage on an existing file, whose length is taken as 
   * the storage length. No data lost in file unless we write to the storage.   
   * @param fileName0
   * @throws IOException
   */
  public FileStorage(String fileName0) throws IOException {
    fileName = fileName0;
    raf = new RandomAccessFile(fileName, "rw");
    long byteLen = raf.length(); 
    if (byteLen % Main.dataLength != 0) {
      throw new IllegalArgumentException();
    }
    numLength = byteLen >> Main.log2DataLength;
    fc = raf.getChannel();
  }
  
  /**
   * Creates new storage forcibly setting the new length.
   * Note that the data in existing file, if any, may be lost. 
   * @param fileName0
   * @param numLen0
   * @throws IOException
   */
  public FileStorage(String fileName0, long numLen0) throws IOException {
    fileName = fileName0;
    raf = new RandomAccessFile(fileName, "rw");
    long byteLen = numLen0 << Main.log2DataLength;
    raf.setLength(byteLen);
    numLength = numLen0;
    fc = raf.getChannel();
  }

  @Override
  public long length() {
    return numLength;
  }

  @Override
  public ReadProvider createReadProvider(long start, long len, int bufSize) {
    assert start >= 0;
    assert len > 0;
    assert bufSize > 0;
    // don't allocate unnecessary buffer space:
    int buf = Util.toIntNoTruncation(Math.min(bufSize, len));
    if (Main.readWriteProvidersMapped) {
      return new MappedReadProvider(fc, start, len, buf);
    } else {
      return new BufferedReadRegion(fc, start, len, buf);
    }
  }

  @Override
  public WriteProvider createWriteProvider(long start, long len, int bufSize) throws IOException {
    assert start >= 0;
    assert len > 0;
    assert bufSize > 0;
    // don't allocate unnecessary buffer space:
    int buf = Util.toIntNoTruncation(Math.min(bufSize, len));
    if (Main.readWriteProvidersMapped) {
      return new MappedWriteProvider(fc, start, len, buf);
    } else {
      return new BufferedWriteRegion(fc, start, len, buf);
    }
  }
  
  @Override
  public void flush() throws IOException {
    // XXX: experimental: disabled.
//    if (fc != null) {
//      fc.force(false);
//    }
  }
  
  @Override
  public void close() throws IOException {
    if (fc != null) {
      fc.close();
      fc = null;
    }
    if (raf != null) {
      raf.close();
      raf = null;
    }
  }

}
