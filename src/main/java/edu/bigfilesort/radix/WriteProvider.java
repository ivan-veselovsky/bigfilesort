package edu.bigfilesort.radix;

import java.io.IOException;

public interface WriteProvider {
  
  void put(int nextValue) throws IOException;

  void flush() throws IOException;
  
  void dispose();
}
