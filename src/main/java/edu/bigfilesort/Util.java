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
    int n = 0;
    while (true) {
      if ((x >>> n) == 1) {
        return n;
      } 
      n++;
    }
  } 
}
