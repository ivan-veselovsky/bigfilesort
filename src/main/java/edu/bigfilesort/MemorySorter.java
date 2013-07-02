package edu.bigfilesort;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;

/**
 * Performs memory sorting of a piece of the file
 * assuming that the required memory is available. 
 */
public class MemorySorter {
  /**
   * reads 'length' bytes from the file starting from position 'pos'.
   * Sorts the ints and writes them back.
   */
  public static void sort(FileChannel fc, long length, long pos) throws IOException {
    assert length % Main.dataLength == 0;
    MappedByteBuffer mbb = fc.map(MapMode.READ_WRITE, pos, length);
    mbb.order(Main.byteOrder);
    IntBuffer intBuffer = mbb.asIntBuffer();
    int[] intArray = new int[(int)length / Main.dataLength];
    intBuffer.get(intArray);
    
    // XXX now using not the most efficient sorting algorithm.
    Arrays.sort(intArray);
    
    intBuffer.clear();
    assert intBuffer.position() == 0;
    assert intBuffer.limit() == intBuffer.capacity();
    intBuffer.put(intArray);
    intArray = null; // gc it asap!
    mbb.force();
  }
}
