package edu.bigfilesort.radix;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import edu.bigfilesort.Util;
import edu.bigfilesort.util.Range;
import static edu.bigfilesort.radix.RadixSort.*;

public class ForwardDistribution {
  
  static final int mask0 = numDigitValues - 1;
  
  private AtomicLong[] counters; // the statistics counters
  private WriteProvider[] regionWriteProviders;
  final int digitNumber; /* 0 .. (numberOfDigits-1) */
  final int bitShift;  
  final int digitMask;
  /**
   * minimal value of one digit
   */
  final int minDigitValue;
  /**
   * maximum value of one digit
   */
  final int maxDigitValue;
  
  private final Storage destinationStorage;
  
  long total; 

  
  public ForwardDistribution(int digitNumber0, Storage destinationStorage0) {
    counters = new AtomicLong[numDigitValues];
    for (int i=0; i<counters.length; i++) {
      counters[i] = new AtomicLong(0);
    }
    
    digitNumber = digitNumber0;
    bitShift = digitNumber * bitsPerDigit;
    digitMask = (mask0 << bitShift);
    
    // The highest digit treated as signed value,
    // while others are treated as positive numbers:
    if (digitNumber == numberOfDigits - 1) {
      // last (highest) digit:
      minDigitValue = -numDigitValues/2;
      maxDigitValue =  numDigitValues/2 - 1; 
    } else {
      // other digits:
      minDigitValue = 0;
      maxDigitValue = numDigitValues - 1;    
    }
    
    destinationStorage = destinationStorage0;
  }
  
  protected void createWriteProviders(long totalBuf) throws IOException {
    long sum = 0;
    long regionStart, regionLength;
    int buf;
    regionWriteProviders = new WriteProvider[numDigitValues];
    for (int regionIndex=0; regionIndex<numDigitValues; regionIndex++) {
      regionStart = getRegionStartPosition(regionIndex);
      assert (sum == regionStart);
      regionLength = getRegionLength(regionIndex);
      if (regionLength > 0) {
        sum += regionLength;
        buf = Util.toIntNoTruncation((totalBuf * regionLength) / total);
        if (buf == 0) {
          buf = 16; // fallback for very small totalNumBufferSpace 
        }
        regionWriteProviders[regionIndex] = destinationStorage.createWriteProvider(regionStart, regionLength, buf);
      } else {
        // if the region is empty, do not create anything for it.
      }
    }
    assert (sum == total);
  }

  /**
   * This operation is thread-safe,
   * can be invoked concurrently from several threads.
   * @param v
   */
  public final void countValue(int v) {
    // Cut the digit from the value. Note that the shift is signed:
    int digitValue = getDigitValue(v);
    assert (digitValue >= minDigitValue);
    assert (digitValue <= maxDigitValue);
    int index = digitValue - minDigitValue;
    assert (index >= 0);
    assert (index < numDigitValues);
    counters[index].incrementAndGet();
  }
  
  void disposeCounters() {
    counters = null;
  }
  
  public void dispose() throws IOException {
    counters = null;
    for (WriteProvider wp: regionWriteProviders) {
      if (wp != null) {
        wp.flush();
        wp.dispose();
        wp = null;
      }
    }
    regionWriteProviders = null;
  }
  
  final int getDigitValue(int v) {
    return (v & digitMask) >> bitShift;
  }
  
  protected final void integrateBackward() {
    long previous;
    for (int i=1; i<counters.length; i++) {
      previous = counters[i-1].get();
      counters[i].addAndGet(previous);
    }
    total = counters[counters.length - 1].get();
  }
  
  /**
   * integrates the distribution for backward traversing (decrement)
   */
  public void integrate() {
    integrateBackward(); // total is set here
    for (int i=counters.length - 1; i>0; i--) {
      counters[i].set(counters[i - 1].get());
    }
    counters[0].set(0);
  }

  /**
   * Current starting position for the specified digit region
   */
  private final long getRegionStartPosition(int index) {
    //int index = digitValue - minDigitValue;
    assert (index >= 0);
    assert (index < numDigitValues);
    return counters[index].get();
  }
  
  /**
   * Remaining length of the specified digit region
   */
  private final long getRegionLength(int index) {
    //int index = digitValue - minDigitValue;
    assert (index >= 0);
    assert (index < numDigitValues);
    assert (total > 0);
    if (index == numDigitValues - 1) {
      return total - counters[index].get();
    } else {
      return counters[index+1].get() - counters[index].get();
    }
  }
  
  public long getNextPosition(int digitValue) {
    int index = digitValue - minDigitValue;
    assert (index >= 0);
    assert (index < numDigitValues);
    return counters[index].getAndIncrement();
  }
  
  /**
   * This operation may be used concurrently, but with the following restrictions:
   * 1) all the src values *must* be passed in the order they are read from src.
   * 2) values that have different 'digitValue' may be passed concurrently since they are 
   * independent (served by different writeProviders). 'digitValueMask' serves to process
   * in different threads only different digitValues. 
   * @param srcValue
   */
  public void write(int srcValue, Range digitRange) throws IOException {
    int digitValue = getDigitValue(srcValue);
    int index = digitValue - minDigitValue;
    if (digitRange == null/*no filter*/ || digitRange.contains(index)) {
      regionWriteProviders[index].put(srcValue);
    }
  }
  
}
