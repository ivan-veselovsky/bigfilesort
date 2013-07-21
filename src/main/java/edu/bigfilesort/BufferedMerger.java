package edu.bigfilesort;

import java.io.IOException;
import java.nio.channels.FileChannel;

import edu.bigfilesort.radix.BufferedReadProvider;
import edu.bigfilesort.radix.BufferedWriteProvider;
import edu.bigfilesort.radix.MappedReadProvider;
import edu.bigfilesort.radix.MappedWriteProvider;
import edu.bigfilesort.radix.ReadProvider;
import edu.bigfilesort.radix.WriteProvider;

/*
 * Implementation with optionally chooseable buffers: mapped or direct.
 * See edu.bigfilesort.Main.readWriteProvidersMapped. 
 */
public class BufferedMerger {

  private final long totalAllowedBuffersNumberSize;
  
  private FileChannel leftReadFc, rightReadFc, writeFc;
  
  private long leftReadStartNumPos;
  private long rightReadStartNumPos;
  
  private long leftNumLength;
  private long rightNumLength;
  
  private long writeStartNumPos;
  
  private ReadProvider leftReadRegion, rightReadRegion;
  private WriteProvider writeRegion;

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
  
  public void init() throws IOException {
    calculateBufferSizes(totalAllowedBuffersNumberSize);
  }
  
  private void calculateBufferSizes(long totalBufNumSize/*in numbers*/) throws IOException {
    if (totalBufNumSize < 4) {
      throw new IllegalArgumentException("Buffer too small for merging: " + totalBufNumSize);
    }
    long quater = totalBufNumSize / 4;
    int remainder = (int)(totalBufNumSize % 4);
    int quaterInt = (int)quater;
    int halfInt = (int)(2 * quater + remainder); // use remainder for the Half.

    if (Main.debug) { 
      System.out.println("left/right buffer = " + quaterInt);
      System.out.println("     write buffer = " + halfInt);
    }
    
    // <= here because numbers  are truncated to ints:
    assert ( 2L * quaterInt + halfInt <= totalBufNumSize );
    
    int leftBuffer = quaterInt;
    int rightBuffer = quaterInt;
    // optimize the case of copying:
    if (rightNumLength == 0) {
      leftBuffer <<= 1;
      rightBuffer = 0;
    }

    // truncate buffers: 
    leftBuffer = truncateBuffer(leftNumLength, leftBuffer);
    rightBuffer = truncateBuffer(leftNumLength, rightBuffer);
    int writeBuffer = truncateBuffer(leftNumLength + rightNumLength, halfInt);
    // create providers:
    if (Main.readWriteProvidersMapped) {
      leftReadRegion = new MappedReadProvider(leftReadFc, leftReadStartNumPos, leftNumLength, leftBuffer);
      rightReadRegion = new MappedReadProvider(rightReadFc, rightReadStartNumPos, rightNumLength, rightBuffer);
      writeRegion = new MappedWriteProvider(writeFc, writeStartNumPos, leftNumLength + rightNumLength, writeBuffer);
    } else {
      leftReadRegion = new BufferedReadProvider(leftReadFc, leftReadStartNumPos, leftNumLength, leftBuffer);
      rightReadRegion = new BufferedReadProvider(rightReadFc, rightReadStartNumPos, rightNumLength, rightBuffer);
      writeRegion = new BufferedWriteProvider(writeFc, writeStartNumPos, leftNumLength + rightNumLength, writeBuffer);
    }
  }
  
  private int truncateBuffer(long length, int buf) {
    if (buf > length) {
      return Util.toIntNoTruncation(length);
    }
    return buf;
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
