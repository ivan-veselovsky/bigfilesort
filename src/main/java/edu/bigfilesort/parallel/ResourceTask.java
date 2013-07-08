package edu.bigfilesort.parallel;

import java.util.concurrent.Callable;

public abstract class ResourceTask  implements Callable<Void> {

  final int id;
  final boolean isSorting;
  protected final Resource resource; 
  
  public ResourceTask(int id0, Resource r, boolean sorting) {
    id = id0;
    resource = r;
    isSorting = sorting;
  }
  
  public final Resource getResource() {
    return resource;
  }

  @Override
  public String toString() {
    if (isSorting) {
      return "Sorting Task #"+id;
    } else {
      return "Merging Task #"+id;
    }
  }
  
  @Override
  public abstract Void call() throws Exception;
  
}
