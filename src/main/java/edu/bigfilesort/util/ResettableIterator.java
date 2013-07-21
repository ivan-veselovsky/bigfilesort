package edu.bigfilesort.util;

public interface ResettableIterator <T> {
  T next();
  void reset();
}
