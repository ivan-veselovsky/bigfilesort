package edu.bigfilesort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class SimpleMerger {

  private FileChannel fcXY, fcZ;
  
  private final ThreadLocal<ByteBuffer> byteBufferThreadLocal = new ThreadLocal<ByteBuffer>() {
    @Override
    protected ByteBuffer initialValue() {
      ByteBuffer bb = ByteBuffer.allocateDirect(Main.dataLength * 2).order(Main.byteOrder);
      return bb;
    }
  };
  
  public SimpleMerger(FileChannel fcXY0, FileChannel fcZ0) {
    fcXY = fcXY0;
    fcZ = fcZ0;
  }
  
  /**
   * Merges 2 neighbor pieces of data assuming that each of 
   * the 2 pieces of length/2 is already sorted.
   * This is done using main step of "merge sort" algorithm.
   * The signature is the same as for method {@link NetSort#merge(long, long)} 
   * @param startPos -- number (data) position, inclusive.
   * @param length -- number length, currently may only be a power of 2. 
   */
  public void merge(final long startPos, final long length) throws Exception {
    final ByteBuffer buf = getBuffer();
    
    long xPosition = startPos * Main.dataLength;
    long yPosition = (startPos  + length/2) * Main.dataLength;
    
    final long maxX = yPosition;
    final long maxY = (startPos  + length) * Main.dataLength;
    
    long zPosition = xPosition; // target pos
    
    // TODO: consider SimpleMerger a thread-local instance and store the 
    // positions and buffers as the class fields to avoid this ugly mess
    // with the local variables.
    boolean endX = false, endY = false;
    int x = readX(buf, xPosition); xPosition += Main.dataLength;
    int y = readY(buf, yPosition); yPosition += Main.dataLength;   
    while (true) {
      if (endX) {
        if (endY) {
          break; // both ended: finish
        } else {
          writeY(buf, zPosition); zPosition += Main.dataLength;
          endY = (yPosition == maxY);
          if (!endY) { y = readY(buf, yPosition); yPosition += Main.dataLength; }
        }
      } else {
        if (endY) {
          writeX(buf, zPosition); zPosition += Main.dataLength;
          endX = (xPosition == maxX);
          if (!endX) { x = readX(buf, xPosition); xPosition += Main.dataLength; }
        } else {
          if (x < y) {
            writeX(buf, zPosition); zPosition += Main.dataLength;
            endX = (xPosition == maxX);
            if (!endX) { x = readX(buf, xPosition); xPosition += Main.dataLength;}
          } else {
            writeY(buf, zPosition); zPosition += Main.dataLength;
            endY = (yPosition == maxY);
            if (!endY) { y = readY(buf, yPosition); yPosition += Main.dataLength;}
          }
        }
      }
    }
    
    assert xPosition == maxX;
    assert yPosition == maxY;
    assert zPosition == (startPos + length) * Main.dataLength;
  }
  
  ByteBuffer getBuffer() {
    return byteBufferThreadLocal.get();
  }
  
  int readX(ByteBuffer buf, long pos) throws IOException {
    buf.position(0);
    buf.limit(Main.dataLength);
    fcXY.read(buf, pos);
    return buf.getInt(0);
  }
  
  int readY(ByteBuffer buf, long pos) throws IOException {
    buf.position(Main.dataLength);
    buf.limit(Main.dataLength * 2);
    fcXY.read(buf, pos);
    return buf.getInt(Main.dataLength);
  }
  
  void writeX(ByteBuffer buf, long zPos) throws IOException {
    buf.position(0);
    buf.limit(Main.dataLength);
    fcZ.write(buf, zPos);
  }
  
  void writeY(ByteBuffer buf, long zPos) throws IOException {
    buf.position(Main.dataLength);
    buf.limit(Main.dataLength * 2);
    fcZ.write(buf, zPos);
  }

  // XXX BUG: write from fc -> fcZ, then fcZ -> fc.
  public void aggregatedMerge(final long startNumPos, final long numPieces, final long pieceNumLength) throws Exception {
    assert Util.isPowerOf2(numPieces); // now only power of 2
    assert Util.isPowerOf2(pieceNumLength); // now only power of 2

    long len = 2 * pieceNumLength;
    for (long q=numPieces; q >= 1; q >>= 1) {
      // q/2 because we merge pairs of pieces:
      for (long i=0; i<q/2; i++) {
        // -----------
        long start = startNumPos + i * len;
        merge(start, len);
        // -----------
      }
      len <<= 1;
      // swap fc <--> fcZ:
      FileChannel tmp = fcXY;
      fcXY = fcZ;
      fcZ = tmp;
    }
  }
  
}
