package edu.bigfilesort.util;

import edu.bigfilesort.Util.DivisionResult;

public class LargeFirstDivisionResultIterator {
  final DivisionResult dr;
  long returnedParts = 0; // the state: how many parts are returned
  public LargeFirstDivisionResultIterator(DivisionResult dr0) {
    dr = dr0;
  }
  public Range next() {
    if (returnedParts == dr.totalParts()) {
      return null; // over
    }
    final Range r;
    if (returnedParts < dr.largerParts) {
      r = new Range(returnedParts * dr.largerPartLength, dr.largerPartLength);
    } else {
      r = new Range(dr.largerParts * dr.largerPartLength 
          + (returnedParts - dr.largerParts) * dr.smallerPartLength, dr.smallerPartLength);
    }
    returnedParts++;
    return r;
  }
  public void reset() {
    returnedParts = 0;
  }
}
