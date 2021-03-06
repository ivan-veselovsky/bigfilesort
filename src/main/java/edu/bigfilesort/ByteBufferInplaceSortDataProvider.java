package edu.bigfilesort;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

import static edu.bigfilesort.Util.*;

/**
 * physical indexes: 
 * min: buffer.position
 * max: buffer.limit - 1
 *
 */
public class ByteBufferInplaceSortDataProvider implements InplaceSortDataProvider {

  private ByteBuffer buf;
  
  private final int indexShift; // numbers, not bytes.
  
  private final AtomicLong numberOfReads = new AtomicLong(0);
  private final AtomicLong numberOfWrites = new AtomicLong(0);
  
  public ByteBufferInplaceSortDataProvider(ByteBuffer bb, int is) {
    buf = bb;
    indexShift = is;
  }
  
  @Override
  public int get(int a) {
    if (Main.countersEnabled) { numberOfReads.incrementAndGet(); }
    assert ((a + indexShift) << Main.log2DataLength) < buf.capacity();
    assert ((a + indexShift) << Main.log2DataLength) >= 0;
    return buf.getInt((a + indexShift) << Main.log2DataLength);
  }
  
  @Override
  public void put(int a, int v) {
    if (Main.countersEnabled) { numberOfWrites.incrementAndGet(); }
    assert ((a + indexShift) << Main.log2DataLength) < buf.capacity();
    assert ((a + indexShift) << Main.log2DataLength) >= 0;
    buf.putInt((a + indexShift) << Main.log2DataLength, v);
  }
  
  @Override
  public int getMaxIndex() {
    // XXX do we need to use limit?
    // buf.capacity() ?
    return (((buf.capacity() - Main.dataLength) >> Main.log2DataLength) - indexShift);
  }

  @Override
  public int getMinIndex() {
    // XXX do we need to use position?
    return (-indexShift);
  }
  
  @Override
  public long numberOfReads() {
    return numberOfReads.get();
  }

  @Override
  public long numberOfWrites() {
    return numberOfWrites.get();
  }

  @Override
  public void force() {
    // XXX: experimental:
//    if (buf instanceof MappedByteBuffer) {
//      ((MappedByteBuffer)buf).force();
//    }
  }

  @Override
  public void dispose() {
    disposeDirectByteBuffer(buf);
    buf = null; // allow GC to eat the Java object.
  }
  
}
