package edu.bigfilesort.radix;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import edu.bigfilesort.Main;
import edu.bigfilesort.Util;

public class MappedWriteProvider extends BufferedWriteRegion implements
    WriteProvider {
  
  private int currentBufferLength;

  public MappedWriteProvider(FileChannel fc0, long regNumStart,
      long regNumLength, int bufNumLen) throws IOException {
    super(fc0, regNumStart, regNumLength, bufNumLen);
  }

  // NB: called from the constructor
  @Override
  protected void initBuffer() throws IOException {
    long numsToBuf = regionNumLength - flushCount;
    if (numsToBuf == 0) {
      currentBufferLength = 0;
      return;
    }
    if (numsToBuf > bufferNumLength) {
      numsToBuf = bufferNumLength;
    } 
    currentBufferLength = Util.toIntNoTruncation(numsToBuf);
    int byteCapacity = Util.toIntNoTruncation(currentBufferLength << Main.log2DataLength);
    long byteStartBufPos = (regionStartNumPosition + flushCount) << Main.log2DataLength;
    buf = fc.map(MapMode.READ_WRITE, byteStartBufPos, byteCapacity).order(Main.byteOrder);
    buf.clear();
  }
  
  @Override
  public final void put(int value) throws IOException {
    assert (writeCount < regionNumLength);
    assert (flushCount <= writeCount);
    buf.putInt(value);
    writeCount++;
    // NB: here auto-flush always works because we use special field 'currentBufferLength':
    if (writeCount == flushCount + currentBufferLength) {
      flushImpl(); // flush the buffer
    } 
  }
  
  @Override
  public void flush() throws IOException {
    final long numsToFlush = writeCount - flushCount;
    if (numsToFlush != currentBufferLength) {
      throw new IllegalStateException("Flush of an unfinished direct buffer is not allowed.");
    }
    flushImpl();
  }
  
  void flushImpl() throws IOException {
    final long numsToFlush = writeCount - flushCount;
    assert (numsToFlush == currentBufferLength);
    if (numsToFlush > 0) {
      if (buf != null) {
        Util.disposeDirectByteBuffer(buf);
        buf = null;
      }
      flushCount = writeCount; // advance flushCount
      initBuffer();
    }
  }
}
