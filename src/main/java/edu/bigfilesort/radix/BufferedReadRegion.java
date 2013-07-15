package edu.bigfilesort.radix;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import edu.bigfilesort.Main;
import edu.bigfilesort.Util;

// TODO: refactor. Copied from edu.bigfilesort.BufferedMerger
/*
 * regStart                                   regStart + regLength - 1
 * |------------------------------------------|
 *      |===========|
 *      buffer
 * |------------>
 *               read count      
 */
public class BufferedReadRegion implements ReadProvider {
  protected final long regionStartNumPosition; // inclusive
  protected final long regionNumLength; // how many numbers are in region
  protected final FileChannel fc;
  
  protected ByteBuffer buf; // not final since it can be re-allocated in principle
  protected final int bufferNumLength; 
  
  protected long readCount;
  protected long buffered;
  
  public BufferedReadRegion(FileChannel fc0, long regNumStart, long regNumLength, int bufNumLen) {
    fc = fc0;
    regionStartNumPosition = regNumStart;
    regionNumLength = regNumLength;
    bufferNumLength = bufNumLen;
    readCount = 0;
    buffered = 0;
    initBuffer();
  }
  
  void initBuffer() {
    // NB: buffer may be re-allocated in principle
    if (buf != null) {
      Util.disposeDirectByteBuffer(buf);
    }
    // NB: this check is too strict: current algorithm allows small regions to be merged.
    if (bufferNumLength > regionNumLength) {
      throw new IllegalStateException("Unecessary buffer length: buf="+bufferNumLength+" > regionNumLength="+regionNumLength);
    }
    int size = bufferNumLength << Main.log2DataLength;
    //if (Main.debug) { System.out.println("Allocating direct buffer of "+bufferNumLength+" ints."); }
    buf = ByteBuffer.allocateDirect(size).order(Main.byteOrder);
  }
  
  protected void placeBufferImpl() throws IOException {
    long numsToBuf = regionNumLength - buffered;
    // TODO: may truncate the buffer when needed. 
    if (numsToBuf > bufferNumLength) {
      numsToBuf = bufferNumLength;
    }
    buf.position(0);
    buf.limit((int)(numsToBuf << Main.log2DataLength));
    long byteStartBufPos = (regionStartNumPosition + buffered) << Main.log2DataLength;  
    fc.read(buf, byteStartBufPos);
    buf.flip();
    buffered += numsToBuf;
  } 
  
  public final boolean hasNext() throws IOException {
    if (readCount == regionNumLength) {
      dispose(); // fast close the buffer
      return false; // region finished.
    }
    if (readCount == buffered) {
      // shift the buffer forward:
      placeBufferImpl();
    } 
    return true;
  }
  
  /**
   * Note that it does not check if the number is available
   * @return
   */
  @Override
  public final int next() {
    assert (readCount < regionNumLength);
    assert (readCount < buffered);
    int v = buf.getInt();
    readCount++;
    return v;
  }
  
  @Override
  public final void dispose() {
    if (buf != null) {
      Util.disposeDirectByteBuffer(buf);
      buf = null;
    }
  }
  
  @Override
  public final long length() {
    return regionNumLength;
  }
  
//  @Override
//  public void reset() throws IOException {
//    readCount = 0;
//    buffered = 0;
//    placeBufferImpl();
//  }
}
