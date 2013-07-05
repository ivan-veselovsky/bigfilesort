package edu.bigfilesort;

public class Util {
  
  public static boolean isPowerOf2(long x) {
    return (x & (x - 1)) == 0;
  }
  
  public static long expectedNetSortCmp(long n) {
    int lgN = Util.log2(n);
    return (lgN * (lgN + 1) * n )/4; 
  }
  
  public static int log2(long x) {
    if (x <= 0) {
      throw new RuntimeException("log2("+x+")");
    } 
    int n = 0;
    while (true) {
      if ((x >>> n) == 1) {
        return n;
      } 
      n++;
    }
  } 
  
  public static int log2plus(long x) {
     final int log2minus = log2(x);
     long back  = (1 << log2minus);
     if (back == x) {
        return log2minus;
     } else {
        assert (back < x);
        return log2minus + 1;
     }
  }
}
