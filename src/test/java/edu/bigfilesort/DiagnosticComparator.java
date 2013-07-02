package edu.bigfilesort;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class DiagnosticComparator implements Comparator {
  
  private final AtomicLong cmpCount = new AtomicLong(0); 

  @Override
  public void compareAndSwap(long numPosA, long numPosB) throws IOException {
    cmpCount.incrementAndGet();
  }
  
  @Override
  public long getCmpCount() {
    return cmpCount.get();
  }

}
