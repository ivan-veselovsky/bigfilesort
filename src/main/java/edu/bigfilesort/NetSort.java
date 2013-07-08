package edu.bigfilesort;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class NetSort {

  final Comparator cmp;

  public NetSort(Comparator cmp0, ExecutorService es) {
    cmp = cmp0;
  }
  
  
  /**
   * Merges 2 neighbor pieces of data assuming that each of 
   * the 2 pieces of length/2 is already sorted.
   * (length is the length of the 2 sorted pieces together.)
   * @param startPos -- number (data) position, inclusive.
   * @param length -- number length, currently may only be a power of 2. 
   */
  public void merge(final long startPos, final long length) throws Exception {
    assert length >= 2;
    assert Util.isPowerOf2(length); // now only power of 2
    
    // Cascade 1: inverted half-cleaner: 
    for (long i=0; i<length/2; i++) {
      final long a = startPos + i;
      final long b = startPos + length - 1 - i;
      //System.out.println("HC*: cmp: ("+a+","+b+")");
      cmp.compareAndSwap(a, b);
      
//      submitParallel(new Callable<Void>() {
//        @Override
//        public Void call() throws Exception {
//          comparator(a, b);
//          return null;
//        }
//      });
    }
    
    //out.println("------------- Cascade 2:");
    // Cascade 2: descending half-cleaners:
    // ex: len = 16: k = 4, 2, 1
    for (long k = length/4; k >= 1; k >>= 1) {
      // -------------------------------------------------------
      // Block of (length/2) operations:
      // i = 0..3, 0..1, 0
      for (long i=0; i<k; i++) {
        for (long q=0; q<(length/2)/k; q++) {
          long a = startPos + q*2*k + k - 1 - i;    // 3,7   2,6  1,5  0,4
          cmp.compareAndSwap(a, a+k);
        }
        //out.println("-----");
      }
      //out.println("-------------");
      // -------------------------------------------------------
    }
  }
  
  /*
   * 
   */
  void submitParallel(Callable<Void> callable) throws Exception {
    callable.call();
  }

  public void aggregatedMerge(final long startNumPos, final long numPieces, final long pieceNumLength) throws Exception {
    assert Util.isPowerOf2(numPieces); // now only power of 2
    assert Util.isPowerOf2(pieceNumLength); // now only power of 2

    long len = 2 * pieceNumLength;
    for (long q=numPieces; q >= 1; q >>= 1) {
      // q/2 because we merge pairs of pieces:
      for (long i=0; i<q/2; i++) {
        // -----------
        long start = startNumPos + i * len;
        merge(start, len);
        // -----------
      }
      len <<= 1;
    }
  }
  
}
