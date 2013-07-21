package edu.bigfilesort.radix;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import edu.bigfilesort.Main;
import edu.bigfilesort.Util;

public class MappedReadProvider extends BufferedReadProvider implements ReadProvider {

  public MappedReadProvider(FileChannel fc0, long regNumStart, long regNumLength, int bufNumLen) {
    super(fc0, regNumStart, regNumLength, bufNumLen);
  }

  @Override
  void initBuffer() {
    if (bufferNumLength > regionNumLength) {
      throw new IllegalStateException("Unecessary buffer length: buf="+bufferNumLength+" > regionNumLength="+regionNumLength);
    }
  }
  
  @Override
  protected void placeBufferImpl() throws IOException {
    if (buf != null) {
      Util.disposeDirectByteBuffer(buf);
      buf = null;
    }
    long numsToBuf = regionNumLength - buffered;
    if (numsToBuf > bufferNumLength) {
      numsToBuf = bufferNumLength;
    } 
    int byteCapacity = (int)(numsToBuf << Main.log2DataLength);
    long byteStartBufPos = (regionStartNumPosition + buffered) << Main.log2DataLength;  
    buf = fc.map(MapMode.READ_ONLY, byteStartBufPos, byteCapacity).order(Main.byteOrder);
    buf.clear();
    buffered += numsToBuf;
  }

}
