package edu.bigfilesort;

import static org.junit.Assert.*;
import static java.lang.System.out;

import java.util.Random;

public class UtilForTest {

  public static void assertArraySorted(int[] a) {
    for (int i=0 ; i<a.length-1; i++) {
      if (a[i] > a[i+1]) {
        fail("Sorting violation at array index "+i+": "+a[i]+" > "+a[i+1]);
      }
    }
  }
  
  public static void fillArray(int[] a, int start, int increment) {
    for (int i=0 ; i<a.length; i++) {
      a[i] = start + i * increment; 
    }
  }
  
  public static long fillArrayRandom(int[] a, long seed) {
    if (seed == 0) {
      seed = System.currentTimeMillis();
    }
    out.println("seed = " + Long.toHexString(seed));
    final Random random = new Random(seed);
    for (int i=0 ; i<a.length; i++) {
      a[i] = (int)(random.nextLong() * (377 + random.nextInt())); 
    }
    return seed;
  }
}
