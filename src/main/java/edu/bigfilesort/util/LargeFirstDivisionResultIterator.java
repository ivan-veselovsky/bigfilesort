package edu.bigfilesort.util;

import edu.bigfilesort.Util.DivisionResult;

public class LargeFirstDivisionResultIterator {
  final DivisionResult dr;
  long returnedParts = 0; // the state: how many parts are returned
  public LargeFirstDivisionResultIterator(DivisionResult dr0) {
    dr = dr0;
  }
  public Range next() {
    if (returnedParts == dr.totalParts() || dr.largerPartLength == 0) {
      return null; // over; this is also true if "dr" contains zero parts at all.
    }
    final Range r;
    if (returnedParts < dr.largerParts) {
      r = new Range(returnedParts * dr.largerPartLength, dr.largerPartLength);
    } else if (dr.smallerPartLength == 0) {
      return null; // do never return Ranges of zero length.
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
