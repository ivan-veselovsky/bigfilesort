package edu.bigfilesort.radix;

import static java.lang.System.out;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.bigfilesort.Main;
import edu.bigfilesort.Util;
import edu.bigfilesort.Util.DivisionResult;
import edu.bigfilesort.parallel.Resource;
import edu.bigfilesort.parallel.ResourceTask;
import edu.bigfilesort.parallel.TaskPlanner;
import edu.bigfilesort.util.LargeFirstDivisionResultIterator;
import edu.bigfilesort.util.Range;

public class RadixPlannerImpl implements TaskPlanner {
  
  private final int threads;
  private final long totalNumbers; // full size of the initial array to be sorted, in numbers.
  
  private final String fileName;
  private final String zFileName; // tmp file called "z" for historical reasons
  // storages for the files:
  private final Storage mainStorage, tmpStorage;
  
  // max total number of ints we can allocate using mapped or direct buffers:
  private final long allocatableTotalNumbersMemory;

  private final DivisionResult allocMemoryPerThread;
  private final LargeFirstDivisionResultIterator allocMemoryIterator;
  
  private final DivisionResult numbersPerThread;
  private final LargeFirstDivisionResultIterator numberRangeIterator;
  
  private final DivisionResult digitValuesPerThread;
  private final LargeFirstDivisionResultIterator digitValuesRangeIterator;   
  
  // dynamic sorting cascade variables:
  private long sortingTasksSubmitted = 0; 
  private long sortingTasksDone = 0;
  
  private long countingTasksSubmitted = 0;
  private long countingTasksDone = 0;
  
  int digitNumber = 0;
  private long allocResource; // the total allocatable memory resource counter

  // locks:
  private final Lock lock = new ReentrantLock(true);
  private final Condition condition = lock.newCondition();
  
  private RadixConcurrentImpl radixConcurrentImpl; // created for each digit, so not final

  
  public RadixPlannerImpl(int threads0, long totalNumbers0, long maxAlloc, String fileName0) throws IOException {
    threads = threads0;
    totalNumbers = totalNumbers0;
    allocatableTotalNumbersMemory = maxAlloc;
    fileName = fileName0;
    zFileName = fileName + ".z";
    
    allocMemoryPerThread = Util.divideByApproximatelyEqualParts(allocatableTotalNumbersMemory, threads);
    allocMemoryIterator = new LargeFirstDivisionResultIterator(allocMemoryPerThread);
    assert (allocMemoryPerThread.totalParts() == threads);
    assert (allocMemoryPerThread.totalLength() == allocatableTotalNumbersMemory);
    
    numbersPerThread = Util.divideByApproximatelyEqualParts(totalNumbers, threads);
    numberRangeIterator = new LargeFirstDivisionResultIterator(numbersPerThread);
    assert (numbersPerThread.totalParts() == threads);
    assert (numbersPerThread.totalLength() == totalNumbers);
    
    digitValuesPerThread = Util.divideByApproximatelyEqualParts(RadixSort.numDigitValues, threads);
    digitValuesRangeIterator = new LargeFirstDivisionResultIterator(digitValuesPerThread);
    assert (numbersPerThread.totalParts() == threads);
    assert (numbersPerThread.totalLength() == totalNumbers);

    mainStorage = new FileStorage(fileName);
    assert (mainStorage.length() == totalNumbers);
    tmpStorage = new FileStorage(zFileName, totalNumbers);
    
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
  public ResourceTask nextTask(final int taskId) throws Exception {
    lock.lock();
    try {
      while (true) {
        // 1. counting tasks:
        {
          final Range numberRange = numberRangeIterator.next();
          if (numberRange != null) {
            // compose and submit a new counting task:
            Range alloc = allocMemoryIterator.next();
            assert (alloc != null); // memory is divided by "threads" as well as
                                    // the digit values range.
            checkAlloc(alloc.length);
            allocResource -= alloc.length;
            assert (allocResource >= 0);
            countingTasksSubmitted++;
            return new CountingTask(taskId, new Resource(alloc.length),
                digitNumber, numberRange, radixConcurrentImpl);
          }
        }
        // all counting tasks submitted.
        // wait all the counting tasks to finish completely:
        while (countingTasksDone < countingTasksSubmitted) {
          condition.await();
        }
        assert (countingTasksDone == countingTasksSubmitted);
        assert (sortingTasksSubmitted == 0);
        assert (allocResource == allocatableTotalNumbersMemory);
        allocMemoryIterator.reset();
        
        // 2. Integrate the Radix. Do not submit special task for that since this is very fast:
        // TODO: alloc correct space for write buffers and pass it in here: 
        //long writeProvidersBuf = ((writeBuffersRatio - 1) * totalBuf)/writeBuffersRatio;
        radixConcurrentImpl.integrate();

        // 3. Sorting tasks:
        final Range digitValueRange = digitValuesRangeIterator.next();
        if (digitValueRange != null) {
          // compose and submit next sorting task:
          // TODO: Util.toIntNoTruncation(buf/writeBuffersRatio)
          Range alloc = allocMemoryIterator.next();
          assert (alloc != null); // memory is divided by "threads" as well as
                                  // the digit values range.
          checkAlloc(alloc.length);
          allocResource -= alloc.length;
          assert (allocResource >= 0);
          sortingTasksSubmitted++;
          return new SortingTask(taskId, new Resource(alloc.length),
              digitNumber, digitValueRange, radixConcurrentImpl);
        }
        // wait all the sorting tasks to finish:
        while (sortingTasksDone < sortingTasksSubmitted) {
          condition.await();
        }
        assert (sortingTasksDone == sortingTasksSubmitted);
        assert (allocResource == allocatableTotalNumbersMemory);

        // 4. finish processing of this digit:
        radixConcurrentImpl.finish();
        
        // next digit:
        digitNumber++;
        if (digitNumber == RadixSort.numberOfDigits) {
          return null; // done, no more tasks.
        }
        nextDigit();
      }
    } finally {
      lock.unlock();
    }
  }

  private void checkAlloc(long alloc) {
    // XXX: likely we need to set minimum memory limit:
    if (alloc < 64) {
      // temporary solution
      throw new IllegalStateException("Too few memory or too many threads. Need to implement resource-waiting logic there for such case.");
    }
  }
  
  private void nextDigit() {
    sortingTasksSubmitted = 0;
    sortingTasksDone = 0;
    
    countingTasksSubmitted = 0;
    countingTasksDone = 0;
    
    digitValuesRangeIterator.reset();
    allocMemoryIterator.reset();
    numberRangeIterator.reset();
    // re-create radix impl for next digit:
    radixConcurrentImpl = new RadixConcurrentImpl(mainStorage, tmpStorage, digitNumber);
  }
  
  static class CountingTask extends ResourceTask {
    private final int digit; // digit field held for diagnostic purposes only
    private final Range numberRange;
    private final RadixConcurrentImpl radixSort;
    private final long buf;
    public CountingTask(int id0, Resource r0, int digit0, Range numberRange0, RadixConcurrentImpl rs0) {
      super(id0, r0, false);
      digit = digit0;
      numberRange = numberRange0; 
      radixSort = rs0;
      buf = r0.allowedNumNativeMemory;
    }
    @Override
    public Void call() throws Exception {
      radixSort.count(numberRange, buf);
      return null;
    }
    @Override
    public String toString() {
      return "Count Task #"+id+": digit="+digit+", numberRange="+numberRange;
    }
  }
  
  static class SortingTask extends ResourceTask {
    private final int digit; // digit field held for diagnostic purposes only
    private final Range digitValueRange;
    private final RadixConcurrentImpl radixSort;
    private final long buf;
    public SortingTask(int id0, Resource r0, int digit0, Range digitValueRange0, RadixConcurrentImpl rs0) {
      super(id0, r0, false);
      digit = digit0;
      digitValueRange = digitValueRange0; 
      radixSort = rs0;
      buf = r0.allowedNumNativeMemory;
    }
    @Override
    public Void call() throws Exception {
      radixSort.moveForFilteredDigitValueRange(digitValueRange, buf);
      return null;
    }
    @Override
    public String toString() {
      return "Sorting Task #"+id+": digit="+digit+", digitValueRange="+digitValueRange;
    }
  }
  
  @Override
  public void done(ResourceTask rt) {
    lock.lock();
    try {
      if (rt.isSorting) {
        sortingTasksDone++;
      } else {
        countingTasksDone++;
      }
      // "return" the memory:
      assert (allocResource >= 0);
      assert (rt.resource.allowedNumNativeMemory > 0);
      allocResource += rt.resource.allowedNumNativeMemory;
      // signal the state change:
      condition.signal();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void finish() throws IOException {
    // 1. close files:
    mainStorage.flush();
    mainStorage.close();
    
    tmpStorage.flush();
    tmpStorage.close();
    
    // 2. delete temp file;
    File z = new File(zFileName);
    z.delete();
    if (z.exists()) {
      throw new IOException("Failed to delete tmp file ["+z.getAbsolutePath()+"]");
    }
  }

}
