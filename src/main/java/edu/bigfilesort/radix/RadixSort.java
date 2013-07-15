package edu.bigfilesort.radix;

import java.io.IOException;

import edu.bigfilesort.Main;
import edu.bigfilesort.Util;

public class RadixSort {

  // Radix constants: --------------------------
  /**
   * Number of bits per one digits
   */
  static final int bitsPerDigit = 8;

  /**
   * Number of digits in one number 
   */
  static final int numberOfDigits = (Main.dataLength/*bytes*/ * 8) / bitsPerDigit;
  
  /**
   * Number of possible values of one digit (256 for 8-bit digit)
   */
  static final int numDigitValues = 1 << bitsPerDigit;
  // --------------------------------------------

  private final Storage mainStorage, tmpStorage;
  private final long totalBuf;
  private final long numLength;
  
  public RadixSort(Storage mainStorage0, Storage tmpStorage0, long totalBuf0) {
    mainStorage = mainStorage0;
    numLength = mainStorage.length();
    tmpStorage = tmpStorage0;
    totalBuf = totalBuf0;
  }
  
  /**
   * Entry point.
   * Main logic described here.
   */
  public void sort() throws IOException {
    ForwardDistribution distribution;
    Storage destinationStorage;
    Storage srcStorage;
    ReadProvider readProvider;
    for (int d=0; d<numberOfDigits; d++) {
      System.out.println("=========================== digit #" + d);
      final long t = System.currentTimeMillis();
      if (d % 2 == 0) {
        srcStorage = mainStorage;
        destinationStorage = tmpStorage;
      } else {
        srcStorage = tmpStorage; 
        destinationStorage = mainStorage;
      }

      final int writeBuffersRatio = 8;
      distribution = new ForwardDistribution(d, destinationStorage, ((writeBuffersRatio - 1) * totalBuf)/writeBuffersRatio ); //was: 1/2
      // here we use all the available buffer space since no writing is performed yet:
      // XXX: yes, can use total buf space, but Windows does not allow to alloc one continuous piece, so /2 it:
      readProvider = srcStorage.createReadProvider(0, numLength, Util.toIntNoTruncation(totalBuf/2) );
      assert (readProvider.length() == numLength);
      fill(distribution, readProvider); // fills the distribution (counters) 
      distribution.integrate(); // integrate the distribution
      assert (distribution.total == numLength);
      System.out.println("Counting finished: " + (System.currentTimeMillis() - t)/1000 + " sec");
      
      readProvider.dispose();
      distribution.createWriteProviders(); // create write providers according to the regions 
      
      distribution.disposeCounters(); // counters are not needed any more 
      
      //readProvider.reset(); // prepare to the next reading from the beginning
      readProvider = srcStorage.createReadProvider(0, numLength, Util.toIntNoTruncation(totalBuf/writeBuffersRatio) );
      
      write(distribution, readProvider); // write data from src -> dst
      
      readProvider.dispose();
      distribution.dispose(); // flush and dispose all the write providers
      
      destinationStorage.flush(); // flush any updates to the dst since on the next loop we will read from it.
      long delta = System.currentTimeMillis() - t;
      System.out.println("=========================== digit #" + d + " finished. Took " + delta/1000 + " sec");
    }
  }
  
  private void fill(ForwardDistribution d, ReadProvider rp) throws IOException {
    int x;
    while (rp.hasNext()) {
       x = rp.next();
       d.countValue(x);
    }
  }
  
  private void write(ForwardDistribution distribution, ReadProvider rp) throws IOException {
    int srcValue;
    long count = 0;
    while (rp.hasNext()) {
      srcValue = rp.next();
      distribution.write(srcValue);
      count++;
    }
    assert (count == mainStorage.length());
  }
}
