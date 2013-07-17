package edu.bigfilesort.parallel;

/**
 * Represents shared resource that should be returned to the pool.
 * (In our case this is some amount of allocatable native memory.)
 */
public class Resource {
  
  public final long allowedNumNativeMemory;
  
  public Resource(long allowedNumNativeMemory0) {
    allowedNumNativeMemory = allowedNumNativeMemory0;
  }
}
