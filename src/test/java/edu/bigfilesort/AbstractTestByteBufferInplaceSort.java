package edu.bigfilesort;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import org.junit.Test;

import edu.bigfilesort.WriteDataMain.Mode;
import static org.junit.Assert.*;
import static java.lang.System.out;

// -Xms131m -Xmx131m -XX:NewRatio=81 -XX:MaxPermSize=1m
public abstract class AbstractTestByteBufferInplaceSort {

  // Number of data values (integers)
  // NB: not necessarily power of 2.
  // 1G file <-> 1024 * 1024 * 256
  // Max possible buffer (~2G): 1024 * 1024 * 512 - 1
  private static final int arrayLength = 1024 * 1024 * 512 - 1; 
  
  protected long bytePos = 0;
  protected long byteLength = ((long)arrayLength) * Main.dataLength;
  
  private RandomAccessFile raf;
  private FileChannel fc;
  
  private String file;

  
  protected void checkSorting() throws Exception {
    assertEquals(0, CheckSortedMain.mainImpl(file));
  }
  
  protected void closeFile() throws Exception {
    if (fc != null) {
      fc.close();
    } 
    if (raf != null) {
      raf.close();
    }
  }

  protected final void defineFile(String suffix) {
    file = getClass().getSimpleName() + "-" + suffix;
  }  
  
  @Test
  public void testArrayAscending() throws Exception {
    out.println("==== testArrayAscending");
    defineFile("asc.data");
    writeData(Mode.asc);
    arraySortImpl();
  }
    
  @Test
  public void testArrayDescending() throws Exception {
    out.println("==== testArrayDescending");
    defineFile("desc.data");
    writeData(Mode.desc);
    arraySortImpl();
  }

  @Test
  public void testArrayFlat() throws Exception {
    out.println("==== testArrayFlat");
    defineFile("flat.data");
    writeData(Mode.flat);
    arraySortImpl();
  }
  
  @Test
  public void testArrayRandom() throws Exception {
    out.println("==== testArrayRandom");
    defineFile("rand.data");
    writeData(Mode.rand);
    arraySortImpl();
  }
  
  protected abstract InplaceSort createInplaceSort(InplaceSortDataProvider provider);
  
  protected InplaceSortDataProvider createProvider() throws Exception {
    raf = new RandomAccessFile(file, "rw");
    fc = raf.getChannel();
    
    // otherwise we cannot create the byte buffer, whose length is int: 
    assertTrue(byteLength <= Integer.MAX_VALUE);
    
    MappedByteBuffer mbb = fc.map(MapMode.READ_WRITE, bytePos, byteLength);
    mbb.order(Main.byteOrder);
    
    assertEquals(byteLength, mbb.capacity());
    
    //mbb.position(0);
    //mbb.limit((int)byteLength);
    return new ByteBufferInplaceSortDataProvider(mbb, -1);
  }
  
  private void writeData(WriteDataMain.Mode mode) throws Exception {
    WriteDataMain writeDataMain = new WriteDataMain();
    assertEquals(0, writeDataMain.mainImpl(file, "" + byteLength, 
        mode.toString()));
  }  
  
  private void arraySortImpl() throws Exception {
    InplaceSortDataProvider provider = createProvider();
    InplaceSort inplaceSort = createInplaceSort(provider);
    
    long t = System.currentTimeMillis();
    inplaceSort.sort(); // ** sort
    provider.force();   // flush
    provider.dispose(); // cleanup and dispose the underlying buffer.
    closeFile();        // close underlying file
    long d = System.currentTimeMillis() - t;
    
    checkSorting();
    
    final long nLogN = ((long)arrayLength) * Util.log2plus(arrayLength);
    out.println("n * log2(n) = " + nLogN);
    printCounters(provider, inplaceSort, nLogN);
    out.println(" ====== Sort time        = " + d + " ms");
  }
  
  private boolean printCounters(InplaceSortDataProvider provider, InplaceSort inplaceSort, long nLogN) {
    out.println("Sort: " + inplaceSort.getName());
    out.println("Reads       = " + ((double)provider.numberOfReads()) / nLogN);
    out.println("Writes      = " + ((double)provider.numberOfWrites()) / nLogN);
    out.println("Comparisons = " + ((double)inplaceSort.numberOfComparisons()) / nLogN);
    return true;
  }
}
