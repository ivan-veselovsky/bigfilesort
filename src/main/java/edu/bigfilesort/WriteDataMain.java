package edu.bigfilesort;

import static java.lang.System.out;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Random;

public class WriteDataMain {

  static final long bufferSize = 1024L * 1024L; // 1m
  
  enum Mode {
    asc, // ascending 
    desc, // descending
    flat, // equal numbers
    rand; // random
  }
  
  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    int status = new WriteDataMain().mainImpl(args);
    System.exit(status);
  }
  
  private DataProvider dataProvider; 
  
  void setProvider(DataProvider provider) {
    dataProvider = provider;
  }
  
  int mainImpl(String... args) throws Exception {
    if (args.length < 3) {
      out.println("Paramaters: <file name> <length> <asc|desc|rand>");
      return 3;
    }
    String fileName = args[0];
    long length = Long.parseLong(args[1]);
    if (length % Main.dataLength != 0) {
      throw new IllegalArgumentException("Length must be multiple of "+Main.dataLength+".");
    }
    String modeStr = args[2];
    
    if (bufferSize % Main.dataLength != 0) {
      throw new IllegalStateException("Buffer size must be multiple of "+Main.dataLength+".");
    }
    
    out.println("File name:   ["+fileName+"]");
    out.println("Length:      ["+length+"]");
    out.println("Write mode:  ["+modeStr+"]");
    
    final long t = System.currentTimeMillis();
    
    writeImpl(fileName, length, modeStr);
    
    long delta = System.currentTimeMillis() - t;
    out.println("Time elapsed:  "+delta+" ms");
    return 0;
  }
  
  void writeImpl(final String name, final long fileLength, final String modeStr) throws Exception {
    long numWrites = fileLength / bufferSize;
    long reminderLength =  fileLength % bufferSize;
    if (reminderLength > 0) {
      numWrites++;
    }
    if (reminderLength > 0) {
      assert ((numWrites - 1) * bufferSize + reminderLength == fileLength);
    } else {
      assert (numWrites * bufferSize + reminderLength == fileLength);
    }
    
    final RandomAccessFile raf = new RandomAccessFile(name, "rw");
    raf.setLength(fileLength);
    final FileChannel fc = raf.getChannel();
    fc.position(0);
    
    final DataProvider provider = getDataProvider(modeStr);
    try {
      long position, len;
      long writtenCount = 0;
      for (int i=0; i<numWrites; i++) {
        position = i * bufferSize;
        len = (reminderLength > 0 
                  && i == numWrites - 1) ? reminderLength : bufferSize;
        
        assert (len % Main.dataLength == 0);
        //out.println("Position = " + position + ", length = " + len);
        
        MappedByteBuffer mbb = fc.map(MapMode.READ_WRITE, position, len);
        mbb.order(Main.byteOrder);
        
        int data;
        for (long offset = 0; offset < (len / Main.dataLength); offset++) {
          data = provider.next();
          mbb.putInt(data);
          writtenCount += Main.dataLength;
        }
        
        mbb.force();
        Util.disposeDirectByteBuffer(mbb);
        mbb = null;
      }
      
      System.out.println("file length =   " + fileLength);
      System.out.println("written count = " + writtenCount);
      assert (writtenCount == fileLength);
    } finally {
      fc.force(true);
      fc.close();
      raf.close();
    }
  }
  
  private DataProvider getDataProvider(String modeStr) {
    if (dataProvider != null) {
      return dataProvider; 
    }
    Mode mode = Mode.valueOf(modeStr);
    switch (mode) {
      case asc: return new AscendingDataProvider();
      case desc: return new DescendingDataProvider();
      case flat: return new FlatDataProvider(-7);
      case rand: return new RamdomDataProvider();
      default: throw new IllegalArgumentException("Unknown mode " + mode);
    }
  }
  
  static interface DataProvider {
    int next();
  }

  /**
   * Used to test Mergers
   */
  static class MergeTestDataProvider implements DataProvider {
    private final int total;
    private final int startValue, increment, secondHalfShift;
    
    int count = 0;
    
    public MergeTestDataProvider(int totalNumbers, int startValue0, int increment0, int secondHalfShift0) {
      startValue = startValue0;
      increment = increment0;
      secondHalfShift = secondHalfShift0;
      total = totalNumbers;
    }
    
    @Override
    public int next() {
      final int result;
      if (count < total/2) {
        // 1st half:
        result = startValue + count * increment;
      } else {
        // 2nd half:
        result = startValue + secondHalfShift + (count - total/2) * increment; 
      }
      //System.out.println(count + ": " + result); // XXX
      count++;
      return result;
    }
  }
  
  static class AscendingDataProvider implements DataProvider {
    protected int data;
    public AscendingDataProvider() {
      data = Integer.MIN_VALUE;
    }
    @Override
    public int next() {
      return data++;
    }
  }
  
  static class DescendingDataProvider extends AscendingDataProvider implements DataProvider {
    public DescendingDataProvider() {
      data = Integer.MAX_VALUE;
    }
    @Override
    public int next() {
      return data--;
    }
  }

  static class FlatDataProvider implements DataProvider {
    private final int data;
    public FlatDataProvider(int x0) {
      data = x0;
    }
    @Override
    public int next() {
      return data;
    }
  }
  
  static class RamdomDataProvider implements DataProvider {
    private final Random random;
    public RamdomDataProvider() {
      final long seed = System.currentTimeMillis();
      out.println("Random seed used: " + Long.toHexString(seed));
      random = new Random(seed);
    }
    @Override
    public int next() {
      return random.nextInt();
    }
  }
  
}
