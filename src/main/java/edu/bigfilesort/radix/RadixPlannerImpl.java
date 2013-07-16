package edu.bigfilesort.radix;

import static java.lang.System.out;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.bigfilesort.Main;
import edu.bigfilesort.Util;
import edu.bigfilesort.Util.DivisionResult;
import edu.bigfilesort.parallel.ResourceTask;
import edu.bigfilesort.parallel.TaskPlanner;

// TODO: unfinished stub!
public class RadixPlannerImpl implements TaskPlanner {
  
  private final int threads;
  private final long totalNumbers; // full size of the inital array to be sorted, in numbers.
  private final String fileName;
  private final String zFileName;

  private final RandomAccessFile raf;
  private final FileChannel fc;
  
  private RandomAccessFile rafZ;
  private FileChannel fcZ;
  
  // max total number of ints we can allocate using mapped or direct buffers:
  private final long allocatableTotalNumbersMemory;

  private final long allocNumbersPerOneThread; // numbers per one sorting task
  //private final long allocNumbersLastThread; // last thread should process this number of numbers.
  
  // dynamic sorting cascade variables:
  private long sortingRangeSubmitCovered = 0;
  private long sortingTasksSubmited = 0;  
  private long sortingTasksDone = 0;

  private long mergeTasksSubmited = 0;  
  private long mergeTasksDone = 0;

  // common:
  private long allocResource;

  private final Lock lock = new ReentrantLock(true);
  private final Condition condition = lock.newCondition();
  
  public RadixPlannerImpl(int threads0, long totalNumbers0, long maxAlloc, String fileName0) throws IOException {
    threads = threads0;
    totalNumbers = totalNumbers0;
    allocatableTotalNumbersMemory = maxAlloc;
    fileName = fileName0;
    zFileName = fileName + ".z";
    
    raf = new RandomAccessFile(fileName, "rw");
    fc = raf.getChannel();
    assert (raf.length() >> Main.log2DataLength == totalNumbers0);
    assert (raf.length() % Main.dataLength == 0);
    
    {
      DivisionResult dr = Util.divideByApproximatelyEqualParts(allocatableTotalNumbersMemory, threads);
      allocNumbersPerOneThread = Math.max(dr.smallerPartLength, dr.largerPartLength);
    }

    sortingRangeSubmitCovered = 0;
    allocResource = allocatableTotalNumbersMemory; // set total
   
    if (Main.debug) { 
      out.println("========================== Planner created with:");
      out.println("num threads =           " + threads);
      out.println("total numbers in file = " + totalNumbers);
      out.println("max alloc numbers     = " + allocatableTotalNumbersMemory);
      out.println("========================== ");
    }
  }

  @Override
  public ResourceTask nextTask(int id) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void done(ResourceTask rt) {
    // TODO Auto-generated method stub

  }

  @Override
  public void finish() throws IOException {
    // TODO Auto-generated method stub

  }

}
