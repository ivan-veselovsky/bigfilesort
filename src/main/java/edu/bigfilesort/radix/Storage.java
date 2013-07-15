package edu.bigfilesort.radix;

import java.io.IOException;

public interface Storage {
  
  /**
   * Length in numbers (not in bytes).
   * TODO: Clarify contact or remove: the length may change in case of file.
   * @return
   * @throws IOException
   */
  long length();
  
  /**
   * NB: all the input in numbers, not bytes.
   * @param start
   * @param len
   * @return
   */
  ReadProvider createReadProvider(long start, long len, int bufSize) throws IOException;
  
  /**
   * NB: all the input in numbers, not bytes.
   * @param start
   * @param len
   * @return
   */
  WriteProvider createWriteProvider(long start, long len, int bufSize) throws IOException;
  
  void flush() throws IOException;
  
  void close() throws IOException;
}
