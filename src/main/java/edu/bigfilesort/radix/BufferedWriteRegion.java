package edu.bigfilesort.radix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import edu.bigfilesort.Main;
import edu.bigfilesort.Util;

//TODO: refactor. Copied from edu.bigfilesort.BufferedMerger
/*
 * regStart                                   regStart + regLength - 1
 * |------------------------------------------|
 *      |===========|
 *      buffer
 * |<---------->| write count
 * |<-->| flushCount 
 */
public class BufferedWriteRegion implements WriteProvider {
  
  protected final long regionStartNumPosition; // inclusive
  protected final long regionNumLength; // how many numbers are in region
  protected final FileChannel fc;
  protected final int bufferNumLength; 
  
  protected ByteBuffer buf;
  
  protected long writeCount; // how many numbers written to buffer or flushed.
  protected long flushCount; // how many numbers are flushed (written to underlying storage).
  
  public BufferedWriteRegion(FileChannel fc0, long regNumStart, long regNumLength, int bufNumLen) throws IOException {
    fc = fc0;
    regionStartNumPosition = regNumStart;
    regionNumLength = regNumLength;
    bufferNumLength = bufNumLen;
    if (bufferNumLength > regionNumLength) {
      throw new IllegalStateException("Unnecessary buffer length");
    }
    writeCount = 0;
    flushCount = 0;
    initBuffer();
  }
  
  protected void initBuffer() throws IOException {
    // NB: buffer may be re-allocated
    if (buf != null) {
      Util.disposeDirectByteBuffer(buf);
    }
    // this check is overestimation:
    int size = bufferNumLength << Main.log2DataLength;
    assert (size > 0);
    if (Main.debug) { System.out.println("Allocating direct buffer of "+bufferNumLength+" ints."); }
    buf = ByteBuffer.allocateDirect(size).order(Main.byteOrder);
    buf.clear();
  }
  
  @Override
  public void flush() throws IOException {
    final long numsToFlush = writeCount - flushCount;
    if (numsToFlush > 0) {
      buf.position(0);
      buf.limit(Util.toIntNoTruncation(numsToFlush << Main.log2DataLength));
      long byteStartBufPos = (regionStartNumPosition + flushCount) << Main.log2DataLength;  
      fc.write(buf, byteStartBufPos);
      buf.clear();
      flushCount += numsToFlush;
      //if (Main.debug) { System.out.println("flushCount = " + flushCount+", advanced by " + numsToFlush); }
    }
  } 
  
  @Override
  public void dispose() {
    if (Main.debug) { 
      System.out.println("writeCount = " + writeCount);
      System.out.println("flushCount = " + flushCount); 
      System.out.println("buffer len = " + bufferNumLength); 
    }
    assert (writeCount == flushCount);
    if (buf != null) {
      Util.disposeDirectByteBuffer(buf);
      buf = null;
    }
  }
  
  @Override
  public void put(int value) throws IOException {
    assert (writeCount < regionNumLength);
    assert (flushCount <= writeCount);
    buf.putInt(value);
    writeCount++;
    // NB: note that auto-flush does not work
    // if writing ends not on the edge of the buffer.
    // So, for this implementation #flush() after write finish is necessary:
    if (writeCount == flushCount + bufferNumLength) {
      flush(); // flush the buffer
    } 
  }
}

