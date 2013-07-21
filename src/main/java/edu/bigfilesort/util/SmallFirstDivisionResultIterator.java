package edu.bigfilesort.util;

import edu.bigfilesort.Util.DivisionResult;

public class SmallFirstDivisionResultIterator implements ResettableIterator<Range> {
  
  private final DivisionResult dr;
  private final long effectiveTotalParts;
  private long returnedParts = 0; // the state: how many parts are returned
  
  public SmallFirstDivisionResultIterator(DivisionResult dr0) {
    dr = dr0;
    if (dr.smallerPartLength == 0) {
      effectiveTotalParts = dr.largerParts; 
    } else {
      effectiveTotalParts = dr.totalParts();
    }
  }
  
  @Override
  public Range next() {
    if (returnedParts == effectiveTotalParts || dr.largerPartLength == 0) {
      return null; // over; this is also true if "dr" contains zero parts at all.
    }
    final Range r;
    if (dr.smallerPartLength == 0) {
      r = new Range(returnedParts * dr.largerPartLength, dr.largerPartLength);
    } else if (returnedParts < dr.smallerParts) {
      r = new Range(returnedParts * dr.smallerPartLength, dr.smallerPartLength);
    } else {
      r = new Range(dr.smallerParts * dr.smallerPartLength 
          + (returnedParts - dr.smallerParts) * dr.largerPartLength, dr.largerPartLength);
    }
    returnedParts++;
    return r;
  }
  
  @Override
  public void reset() {
    returnedParts = 0;
  }
}
