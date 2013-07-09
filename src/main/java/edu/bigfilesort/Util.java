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
  
  public static class Couple {
    public final long fraction;
    public final long remainder;
    public Couple(long f, long rem) {
      fraction = f;
      remainder = rem;
    }
  }
  
  /**
   * Some number of pieces not longer than "pieceSize" each.
   * @param totalLength
   * @param pieceSize
   * @return
   */
  public static Couple divideByPiecesOfLength(long totalLength, long pieceSize) {
    long numPieces = totalLength / pieceSize;
    long remainderLength =  totalLength % pieceSize;
    if (remainderLength > 0) {
      numPieces++;
    }
    if (remainderLength > 0) {
      assert ((numPieces - 1) * pieceSize + remainderLength == totalLength);
    } else {
      assert (numPieces * pieceSize + remainderLength == totalLength);
    }
    return new Couple(numPieces, remainderLength);
  }

  /**
   * Desire exactly "numPieces" of approximately equal pieces
   * @param totalNumbers
   * @param numPieces
   * @return
   */
  public static Couple divideByNumberOfPieces(final long totalNumbers, final long numPieces) {
    long remainder = totalNumbers % numPieces;
    long numbersInPiece = totalNumbers / numPieces;
    if (remainder > 0) {
       numbersInPiece++;
       remainder = totalNumbers % numbersInPiece;
    }
    if (remainder == 0) {
       assert (numbersInPiece * numPieces + remainder == totalNumbers);
    } else {
       assert (numbersInPiece * (numPieces - 1) + remainder == totalNumbers);
    }
    return new Couple(numbersInPiece, remainder);
  }
}
