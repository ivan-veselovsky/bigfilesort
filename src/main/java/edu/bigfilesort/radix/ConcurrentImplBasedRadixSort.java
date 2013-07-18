package edu.bigfilesort.radix;

import java.io.IOException;

import edu.bigfilesort.Util;
import edu.bigfilesort.util.LargeFirstDivisionResultIterator;
import edu.bigfilesort.util.Range;

import static edu.bigfilesort.radix.RadixSort.*;

/**
 * Plain single-thread implementation
 * based on concurrent impl.
 * Created primarily to test the concurrent impl 
 * correctness at least in serial mode. 
 */
public class ConcurrentImplBasedRadixSort {

  private final Storage mainStorage, tmpStorage;
  private final long numLength;
  private final int threads; // simulated, not real threads
  
  public ConcurrentImplBasedRadixSort(Storage mainStorage0, Storage tmpStorage0, int threads0) {
    mainStorage = mainStorage0;
    numLength = mainStorage.length();
    tmpStorage = tmpStorage0;
    threads = threads0;
  }
  
  /**
   * Entry point.
   * Main logic described here.
   */
  public void sort(long totalBuf) throws IOException {
    RadixConcurrentImpl radixConcurrentImpl;
    final LargeFirstDivisionResultIterator digitIt = new LargeFirstDivisionResultIterator(Util.divideByApproximatelyEqualParts(numDigitValues, threads));
    for (int d=0; d<numberOfDigits; d++) {
      System.out.println("=========================== digit #" + d);
      final long t = System.currentTimeMillis();
      // NB: impl is created for each digit:
      radixConcurrentImpl = new RadixConcurrentImpl(mainStorage, tmpStorage, d); 

      LargeFirstDivisionResultIterator numberRangeIterator = new LargeFirstDivisionResultIterator(
          Util.divideByApproximatelyEqualParts(numLength, threads));
      Range numberRange;  
      while (true) {
        numberRange = numberRangeIterator.next();
        if (numberRange == null) {
          break;
        }
        radixConcurrentImpl.count(numberRange, totalBuf/2);
      }
      
      long writeTotalBuf = ((writeBuffersRatio - 1) * totalBuf)/writeBuffersRatio;
      radixConcurrentImpl.integrate(writeTotalBuf);
      System.out.println("Counting finished: " + (System.currentTimeMillis() - t)/1000 + " sec");
      
      digitIt.reset();
      Range digitRange;
      final int readBufferPerThread = Util.toIntNoTruncation(totalBuf/(writeBuffersRatio*threads));
      while (true) {
        digitRange = digitIt.next();
        if (digitRange == null) {
          break;
        }
        radixConcurrentImpl.moveForFilteredDigitValueRange(digitRange, readBufferPerThread);
      }
      
      radixConcurrentImpl.finish();
      
      long delta = System.currentTimeMillis() - t;
      System.out.println("=========================== digit #" + d + " finished. Took " + delta/1000 + " sec");
    }
  }
  
}
