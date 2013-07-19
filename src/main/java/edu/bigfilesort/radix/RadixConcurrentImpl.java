package edu.bigfilesort.radix;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import edu.bigfilesort.Util;
import edu.bigfilesort.util.Range;

public class RadixConcurrentImpl {
  
  private final Storage mainStorage;
  private final Storage tmpStorage;
  private Storage srcStorage;
  private Storage destinationStorage;
  private ForwardDistribution[] distributions;
  private long totalWriteProvidersBuffersLength;
  
  // diagnostic/debug:
  private final AtomicBoolean integrated = new AtomicBoolean(false);
  
  public RadixConcurrentImpl(Storage mainStorage0, Storage tmpStorage0, boolean atomicCounters) {
    if (mainStorage0 == null || tmpStorage0 == null) {
      throw new NullPointerException();
    }
    mainStorage = mainStorage0;
    tmpStorage = tmpStorage0;
    distributions = new ForwardDistribution[RadixSort.numberOfDigits];
    for (int d=0; d<distributions.length; d++) {
      distributions[d] = new ForwardDistribution(d, atomicCounters);
    }
    assignStorages(0); // initial storage assignment    
  }

  private void assignStorages(int digitNumber) {
    // place the storages to the required src/dst positions: 
    if (digitNumber % 2 == 0) {
      srcStorage = mainStorage;
      destinationStorage = tmpStorage;
    } else {
      srcStorage = tmpStorage; 
      destinationStorage = mainStorage;
    }
  }
  
  /**
   * Is to be called concurrently for non-overlapping number ranges.
   * This is possible if the number ranges do not overlap and
   * the counters in the distribution are thread-safe (we use Atomics). 
   * @param numberRange
   * @param buf
   */
  public void count(Range numberRange, long buf) throws IOException {
    if (integrated.get()) {
      throw new IllegalStateException("must be counted before integration.");
    }
    // here we use all the available buffer space since no writing is performed yet:
    // XXX: yes, can use total buf space, but Windows does not allow to alloc one continuous piece, so /2 it:
    ReadProvider readProvider = srcStorage.createReadProvider(numberRange.start, 
        numberRange.length, Util.toIntNoTruncation(buf) );
    assert (readProvider.length() == numberRange.length);
    // fills the distributions (counters) by the values found in the specified range: 
    // copied to local vars for performance reasons:
    final ForwardDistribution[] distributions0 = distributions;
    final int distributionsLength = distributions0.length;
    int x;
    int i;
    while (readProvider.hasNext()) {
       x = readProvider.next();
       // NB: fill all the distributions in one shot:
       for (i=0; i<distributionsLength; i++) {
         distributions0[i].countValue(x);
       }
    }
    readProvider.dispose();
  }
  
  /**
   * This method must be called once, after all {@link #count(Range, long)} calls are finished.
   */
  public void integrateAllDigits() {
    if (!integrated.compareAndSet(false, true)) {
      throw new IllegalStateException("must be integrated only once.");
    }
    // integrate the distributions:
    for (ForwardDistribution distribution: distributions) {
      distribution.integrate();
      assert (distribution.total == srcStorage.length()); // all the values must be counted
    }
  }
  
  public long getTotalWriteProvidersBuffersLength() {
    return totalWriteProvidersBuffersLength;
  }

  public void startDigit(int digitNumber, final long totalWriteProvidersBuf) throws IOException {
    if (digitNumber > 0) {
      // NB: 1st time storages are assigned in the constructor because they are needed for countinfg
      assignStorages(digitNumber);
    }
    // prepare for the data moving:
    distributions[digitNumber].createWriteProviders(destinationStorage, totalWriteProvidersBuf); // create write providers according to the regions 
    distributions[digitNumber].disposeCounters(); // counters are not needed any more
    // save the number:
    totalWriteProvidersBuffersLength = totalWriteProvidersBuf;
  }
  
  /**
   * Is to be called concurrently for non-overlapping ranges of digit values
   * after {@link #integrate()} call is returned. 
   * @param digitValueRange
   * @param buf
   */
  public void moveForFilteredDigitValueRange(int digitNumber, Range digitValueRange, long bufForReadProvider) throws IOException {
    if (!integrated.get()) {
      throw new IllegalStateException("must be integrated prio to moving data.");
    }
    // Note that the read provider created for the entire number range:
    ReadProvider readProvider 
      = srcStorage.createReadProvider(0, srcStorage.length(), Util.toIntNoTruncation(bufForReadProvider));
    // write data from src -> dst:
    int srcValue;
    while (readProvider.hasNext()) {
      srcValue = readProvider.next();
      distributions[digitNumber].write(srcValue, digitValueRange);
    }
    readProvider.dispose();
  }
  
  public void finishDigit(int digitNumber) throws IOException {
    destinationStorage.flush(); // flush any updates to the dst since on the next loop we will read from it.
    destinationStorage = null;
    srcStorage = null;
    distributions[digitNumber].dispose();
  }

  /**
   * Must be called once.
   * @throws IOException
   */
  public void finish() throws IOException {
    for (int i=0; i<distributions.length; i++) {
      distributions[i].dispose(); // flush and dispose all the write providers
      distributions[i] = null;
    }
    distributions = null;
    
    if (destinationStorage != null) {
      destinationStorage.flush(); // flush any updates to the dst since on the next loop we will read from it.
      destinationStorage = null;
    }
    
    srcStorage = null;
  }
}
