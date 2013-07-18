package edu.bigfilesort.radix;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import edu.bigfilesort.Util;
import edu.bigfilesort.util.Range;

public class RadixConcurrentImpl {
  
  private Storage srcStorage;
  private Storage destinationStorage;
  private final int digitNumber;
  private ForwardDistribution distribution;
  private long totalWriteProvidersBuffersLength;
  
  // diagnostic/debug:
  private final AtomicBoolean integrated = new AtomicBoolean(false);
  
  public RadixConcurrentImpl(Storage mainStorage0, Storage tmpStorage0, int digitNumber0) {
    digitNumber = digitNumber0;
    // place the storages to the required src/dst positions: 
    if (digitNumber % 2 == 0) {
      srcStorage = mainStorage0;
      destinationStorage = tmpStorage0;
    } else {
      srcStorage = tmpStorage0; 
      destinationStorage = mainStorage0;
    }
    distribution = new ForwardDistribution(digitNumber, destinationStorage);
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
    // fills the distribution (counters) by the values found in the specified range: 
    int x;
    while (readProvider.hasNext()) {
       x = readProvider.next();
       distribution.countValue(x);
    }
    readProvider.dispose();
  }
  
  /**
   * This method must be called once, after all {@link #count(Range, long)} calls are finished.
   */
  public void integrate(final long totalWriteProvidersBuf) throws IOException {
    if (!integrated.compareAndSet(false, true)) {
      throw new IllegalStateException("must be integrated only once.");
    }
    distribution.integrate(); // integrate the distribution
    assert (distribution.total == srcStorage.length()); // all the values must be counted

    // prepare for the data moving:
    distribution.createWriteProviders(totalWriteProvidersBuf); // create write providers according to the regions 
    distribution.disposeCounters(); // counters are not needed any more
    totalWriteProvidersBuffersLength = totalWriteProvidersBuf;
  }
  
  public long getTotalWriteProvidersBuffersLength() {
    return totalWriteProvidersBuffersLength;
  }

  /**
   * Is to be called concurrently for non-overlapping ranges of digit values
   * after {@link #integrate()} call is returned. 
   * @param digitValueRange
   * @param buf
   */
  public void moveForFilteredDigitValueRange(Range digitValueRange, long bufForReadProvider) throws IOException {
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
      distribution.write(srcValue, digitValueRange);
    }
    readProvider.dispose();
  }

  /**
   * Must be called once.
   * @throws IOException
   */
  public void finish() throws IOException {
    distribution.dispose(); // flush and dispose all the write providers
    distribution = null;
    destinationStorage.flush(); // flush any updates to the dst since on the next loop we will read from it.
    destinationStorage = null;
    srcStorage = null;
  }
}
