package edu.bigfilesort.radix;

import java.io.IOException;

public interface ReadProvider {

  /**
   * Total numbers that can be read from this provider.
   * The returned number is a constant during all the object lifecycle.   
   */
  long length();
  
  boolean hasNext() throws IOException;
  
  int next();
  
//  /**
//   * Resets the provider to the start position.
//   */
//  void reset() throws IOException;
  
  void dispose();
}
