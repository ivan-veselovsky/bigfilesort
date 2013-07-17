package edu.bigfilesort;

import java.nio.ByteBuffer;

import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

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
  
  public static void disposeDirectByteBuffer(ByteBuffer buf) {
    if (buf.isDirect() && buf instanceof DirectBuffer) {
      //int cap = buf.capacity();
      /*
       * NB: DirectBuffer and Cleaner are not parts of the
       * official nio API. So, these classes may be changed in further
       * JDK releases. However, they seem to be present in both JDK6 and 7. 
       */
      Cleaner cleaner = ((DirectBuffer)buf).cleaner();
      if (cleaner != null) {
        cleaner.clean();
//        if (Main.debug) {
//          System.out.println("     direct buffer of cap = " +cap+ " freed.");
//        }
      }
    }
  }
  
  public static final class DivisionResult {
    public final long smallerParts;
    public final long smallerPartLength;
    public final long largerParts;
    public final long largerPartLength;
    public DivisionResult(long sp, long spLen, long lp, long lpLen) {
      // if larger part length > 0, larger part must be larger than the smaller part:
      if (lpLen > 0 && lpLen <= spLen) {
        throw new IllegalArgumentException(lpLen + " <= " + spLen);
      }
      smallerParts = sp;
      smallerPartLength = spLen;
      largerParts = lp;
      largerPartLength = lpLen;
    }
    public long totalParts() {
      return smallerParts + largerParts;
    }
    public long totalLength() {
      return smallerParts * smallerPartLength + largerParts * largerPartLength;
    }
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof DivisionResult) {
        DivisionResult other = (DivisionResult)obj;
        return (other.smallerParts == smallerParts)
            && (other.smallerPartLength == smallerPartLength)
            && (other.largerParts == largerParts)
            && (other.largerPartLength == largerPartLength);
      }
      return false;
    }
    @Override
    public int hashCode() {
      throw new UnsupportedOperationException("Not intended to be used in hash-based collections.");
    }
    @Override
    public String toString() {
      return smallerParts + " * " + smallerPartLength +" + " + largerParts  + " * " +  largerPartLength;
    }
  }
  
  /**
   * Some number of pieces not longer than "pieceSize" each.
   * @param totalLength
   * @param pieceSize
   * @return
   */
  public static DivisionResult divideNotLonger(final long totalLength, final long pieceSize) {
    long f = totalLength / pieceSize;
    long r = totalLength % pieceSize;
    long smallerParts = (r > 0) ? 1 : 0;
    DivisionResult dr = new DivisionResult(smallerParts, r, f, pieceSize);
    assert (dr.totalLength() == totalLength);
    assert (dr.largerPartLength == pieceSize);
    return dr;
  }

  /**
   * Desire exactly "numPieces" of approximately equal pieces
   * @param totalApples total amount of apples to divide
   * @param numParts
   * @return the division result
   */
  public static DivisionResult divideByApproximatelyEqualParts(final long totalApples, final long numParts) {
    long f = totalApples / numParts;
    long r = totalApples % numParts;
    final DivisionResult dr;
    if (r == 0) {
      dr = new DivisionResult(0, 0,  numParts, f);
    } else {
      dr = new DivisionResult(numParts - r, f,  r, f + 1);
    }
    assert (dr.totalLength() == totalApples);
    assert (dr.totalParts() == numParts);
    return dr;
  }
  
  public static int toIntNoTruncation(long x) {
    if (x > Integer.MAX_VALUE || x < Integer.MIN_VALUE) {
      throw new RuntimeException("long ["+Long.toHexString(x)+"] cannot be casted to int w/o truncation.");
    }
    return (int)x;
  }
  
  public static boolean assertionsEnabled() {
    System.out.println("Assertions enabled.");
    return true;
  }
}
