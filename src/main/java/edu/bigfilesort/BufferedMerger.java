package edu.bigfilesort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/*
 * Implementation with no direct mapping, just native byte buffers. 
 */
public class BufferedMerger {

  private final long totalAllowedBuffersNumberSize;
  
  private FileChannel leftReadFc, rightReadFc, writeFc;
  
  private long leftReadStartNumPos;
  private long rightReadStartNumPos;
  
  private long leftNumLength;
  private long rightNumLength;
  
  private long writeStartNumPos;
  
  private BufferedReadRegion leftReadRegion, rightReadRegion;
  private BufferedWriteRegion writeRegion;

  public BufferedMerger(long totalAllowedBufNumSize) {
    totalAllowedBuffersNumberSize = totalAllowedBufNumSize;
  }
 
  public void setLeft(FileChannel fc, long numStart, long numLength) {
    leftReadFc = fc;
    leftReadStartNumPos = numStart;
    leftNumLength = numLength;
  }
  
  public void setRight(FileChannel fc, long numStart, long numLength) {
    rightReadFc = fc;
    rightReadStartNumPos = numStart;
    rightNumLength = numLength;
  }

  public void setTarget(FileChannel fc, long numStart) {
    writeFc = fc;
    writeStartNumPos = numStart;
  }
  
  public void init() {
    calculateBufferSizes(totalAllowedBuffersNumberSize);
  }
  
  /*
   * regStart                                   regStart + regLength - 1
   * |------------------------------------------|
   *      |===========|
   *      buffer
   * |------------>
   *               read count      
   */
  static class BufferedReadRegion {
    private final long regionStartNumPosition; // inclusive
    private final long regionNumLength; // how many numbers are in region
    private final FileChannel fc;
    
    private ByteBuffer buf; // not final since it can be re-allocated in principle
    private final int bufferNumLength;
    
    private long readCount;
    private long buffered;
    
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
      // NB: buffer may be re-allocated
      if (buf != null) {
        Util.disposeDirectByteBuffer(buf);
      }
      if (bufferNumLength > regionNumLength) {
        throw new IllegalStateException("Unecessary buffer length");
      }
      int size = bufferNumLength << Main.log2DataLength;
      if (Main.debug) { System.out.println("Allocating direct buffer of "+bufferNumLength+" ints."); }
      buf = ByteBuffer.allocateDirect(size).order(Main.byteOrder);
    }
    
    /**
     * 1) If old buffer exists, flushes it to underlying storage and disposes the buffer.
     * 2) Places buffer to a new position 
     * 3) Pre-fetches the data into it (if needed)
     * 4) Sets positions inside the buffer, so that it is ready for reading.   
     */
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
    
    final boolean hasNext() throws IOException {
      if (readCount == regionNumLength) {
        return false; // region finished.
      }
      if (readCount == buffered) {
        // shift the buffer:
        placeBufferImpl();
      } 
      return true;
    }
    
    /**
     * Note that it does not check if the number is available
     * @return
     */
    final int next() {
      assert (readCount < regionNumLength);
      assert (readCount < buffered);
      int v = buf.getInt();
      readCount++;
      return v;
    }
    
    void dispose() {
      if (buf != null) {
        Util.disposeDirectByteBuffer(buf);
        buf = null;
      }
    }
  }

  /*
   * regStart                                   regStart + regLength - 1
   * |------------------------------------------|
   *      |===========|
   *      buffer
   * |<---------->| write count
   * |<-->| flushCount 
   */
  static class BufferedWriteRegion {
    
    private final long regionStartNumPosition; // inclusive
    private final long regionNumLength; // how many numbers are in region
    private final FileChannel fc;
    
    private ByteBuffer buf;
    private final int bufferNumLength;
    
    private long writeCount; // how many numbers written to buffer or flushed.
    private long flushCount; // how many numbers are flushed (written to underlying storage).
    
    public BufferedWriteRegion(FileChannel fc0, long regNumStart, long regNumLength, int bufNumLen) {
      fc = fc0;
      regionStartNumPosition = regNumStart;
      regionNumLength = regNumLength;
      bufferNumLength = bufNumLen;
      writeCount = 0;
      flushCount = 0;
      initBuffer();
    }
    
    void initBuffer() {
      // NB: buffer may be re-allocated
      if (buf != null) {
        Util.disposeDirectByteBuffer(buf);
      }
      if (bufferNumLength > regionNumLength) {
        throw new IllegalStateException("Unecessary buffer length");
      }
      int size = bufferNumLength << Main.log2DataLength;
      if (Main.debug) { System.out.println("Allocating direct buffer of "+bufferNumLength+" ints."); }
      buf = ByteBuffer.allocateDirect(size).order(Main.byteOrder);
      buf.clear();
    }
    
    protected void flush() throws IOException {
      final long numsToFlush = writeCount - flushCount;
      if (numsToFlush > 0) {
        buf.position(0);
        buf.limit((int)(numsToFlush << Main.log2DataLength));
        long byteStartBufPos = (regionStartNumPosition + flushCount) << Main.log2DataLength;  
        fc.write(buf, byteStartBufPos);
        buf.clear();
        flushCount += numsToFlush;
        //if (Main.debug) { System.out.println("flushCount = " + flushCount+", advanced by " + numsToFlush); }
      }
    } 
    
    void dispose() {
      if (Main.debug) { 
        System.out.println("writeCount = " + writeCount);
        System.out.println("flushCount = " + flushCount); 
      }
      assert (writeCount == flushCount);
      if (buf != null) {
        Util.disposeDirectByteBuffer(buf);
        buf = null;
      }
    }
    
    final void put(int value) throws IOException {
      assert (writeCount < regionNumLength);
      assert (flushCount <= writeCount);
      buf.putInt(value);
      writeCount++;
      if (writeCount == flushCount + bufferNumLength) {
        flush(); // flush the buffer
      } 
    }
  }
  
  
  private void calculateBufferSizes(long totalBufNumSize/*in numbers*/) {
    if (totalBufNumSize < 4) {
      throw new IllegalArgumentException("Buffer too small for merging: " + totalBufNumSize);
    }
    long quater = totalBufNumSize / 4;
    int reminder = (int)(totalBufNumSize % 4);
    int quaterInt = (int)quater;
    int halfInt = (int)(2 * quater + reminder); // use reminder for the Half.

    if (Main.debug) { 
      System.out.println("left/right buffer = " + quaterInt);
      System.out.println("     write buffer = " + halfInt);
    }
    
    // <= here because numbers  are truncated to ints:
    assert ( 2L * quaterInt + halfInt <= totalBufNumSize );
    
    leftReadRegion = new BufferedReadRegion(leftReadFc, leftReadStartNumPos, leftNumLength, quaterInt);
    rightReadRegion = new BufferedReadRegion(rightReadFc, rightReadStartNumPos, rightNumLength, quaterInt);
    
    writeRegion = new BufferedWriteRegion(writeFc, writeStartNumPos, leftNumLength + rightNumLength, halfInt); 
  }
  
  public void merge() throws IOException {
    boolean leftHasNext = leftReadRegion.hasNext();
    boolean rightHasNext = rightReadRegion.hasNext();
    
    int left = leftHasNext ? leftReadRegion.next() : 0;
    int right = rightHasNext ? rightReadRegion.next() : 0;
    
    int w; // what to write
    long writeCount = 0;
    while (true) {
      if (leftHasNext) {
        if (rightHasNext) {
          // compare:
          if (left < right) {
            w = left;
            leftHasNext = leftReadRegion.hasNext();
            if (leftHasNext) { left = leftReadRegion.next(); }
          } else {
            w = right;
            rightHasNext = rightReadRegion.hasNext();
            if (rightHasNext) { right = rightReadRegion.next(); }
          }
        } else {
          w = left;
          leftHasNext = leftReadRegion.hasNext();
          if (leftHasNext) { left = leftReadRegion.next(); }
        }
      } else {
        // no left:
        if (rightHasNext) {
          w = right;
          rightHasNext = rightReadRegion.hasNext();
          if (rightHasNext) { right = rightReadRegion.next(); }
        } else {
          break; // finish, do not write anything
        }
      }
      writeRegion.put(w);
      writeCount++;
    } // loop
    
    // NB: this is needed in case if last written value was not on the buffer end,
    // e.g. buffer size == 3, while 4 numbers were written:
    writeRegion.flush();
    
    if (Main.debug) { System.out.println("total written: " + writeCount); }
    assert (writeCount == leftNumLength + rightNumLength);
  }
  
  public void dispose() {
    leftReadRegion.dispose();
    rightReadRegion.dispose();
    writeRegion.dispose();
  }
}
