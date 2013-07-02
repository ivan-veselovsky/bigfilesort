package edu.bigfilesort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;

public class FileComparator implements Comparator {
  
  private final AtomicLong cmpCount = new AtomicLong(0); 
  private final FileChannel fc;
  /*
   * Thread-local buffers for 2 numbers each.
   * The byte order is forcibly set to the configured value.
   */
  private final ThreadLocal<ByteBuffer> byteBufferThreadLocal = new ThreadLocal<ByteBuffer>() {
    @Override
    protected ByteBuffer initialValue() {
      ByteBuffer bb = ByteBuffer.allocateDirect(Main.dataLength * 2).order(Main.byteOrder);
      return bb;
    }
  };
  
  public FileComparator(FileChannel fc0) {
    fc = fc0;
  }
  
  @Override
  public void compareAndSwap(long numPosA, long numPosB) throws IOException {
    comparatorImpl(numPosA * Main.dataLength, numPosB * Main.dataLength);
  }
  
  /*
   * NB: min element to the lower address (ascending ordering).
   */
  void comparatorImpl(long a, long b) throws IOException {
    assert a < b; // a assumed to be the lower addr.
    final ByteBuffer buf = getBuffer();
    
    buf.position(0);
    buf.limit(Main.dataLength);
    int read = fc.read(buf, a);
      assert read == Main.dataLength;
    int x = buf.getInt(0);
    
    buf.limit(Main.dataLength * 2);
    read = fc.read(buf, b);
      assert read == Main.dataLength;
    int y = buf.getInt(Main.dataLength);
    
    if (x > y) {
      // swap: write 0-4th to b, and 4-8th to a:
      buf.position(0);
      buf.limit(Main.dataLength);
      int w = fc.write(buf, b);
        assert w == Main.dataLength;
      buf.limit(Main.dataLength * 2);
      w = fc.write(buf, a);
        assert w == Main.dataLength;
    }
    cmpCount.incrementAndGet();
  }
  
  ByteBuffer getBuffer() {
    return byteBufferThreadLocal.get();
  }
  
  @Override
  public long getCmpCount() {
    return cmpCount.get();
  }
}
