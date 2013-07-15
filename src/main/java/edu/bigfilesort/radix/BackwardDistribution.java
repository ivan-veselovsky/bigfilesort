//package edu.bigfilesort.radix;
//
//import static edu.bigfilesort.radix.Radix.*;
//
//public class BackwardDistribution extends ForwardDistribution {
//  
//  public BackwardDistribution(int digitNumber0) {
//    super(digitNumber0);
//  }
//  
//  /**
//   * integrates the distribution for backward traversing (decrement)
//   */
//  public void integrate(long total0) {
//    total = total0;
//    integrateBackward();
//  }
//  
//  public long getNextPosition(int digitValue) {
//    int index = digitValue - minDigitValue;
//    assert (index >= 0);
//    assert (index < numDigitValues);
//    return counters[index].decrementAndGet();
//  }
//}
