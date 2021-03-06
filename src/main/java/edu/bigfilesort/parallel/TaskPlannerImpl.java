package edu.bigfilesort.parallel;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import edu.bigfilesort.BufferedMerger;
import edu.bigfilesort.ByteBufferInplaceSortDataProvider;
import edu.bigfilesort.ChoiceQuickHeapInplaceSort;
import edu.bigfilesort.InplaceSort;
import edu.bigfilesort.InplaceSortDataProvider;
import edu.bigfilesort.Main;
import edu.bigfilesort.Util;
import edu.bigfilesort.Util.DivisionResult;
import edu.bigfilesort.util.CoveringIterator;
import edu.bigfilesort.util.GroupingIterator;
import edu.bigfilesort.util.LargeFirstDivisionResultIterator;
import edu.bigfilesort.util.LoopingIterator;
import edu.bigfilesort.util.Range;
import edu.bigfilesort.util.ResettableIterator;
import edu.bigfilesort.util.SmallFirstDivisionResultIterator;

import static java.lang.System.out;

public class TaskPlannerImpl implements TaskPlanner {
  
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

  //private final long allocNumbersPerOneThread; // numbers per one sorting task
  private final ResettableIterator<Range> sortCoveringIterator;
  
  private final ResettableIterator<Range> mergeMemoryLoopingIterator;
  private final GroupingIterator mergeGroupingIterator;
  
  // dynamic sorting cascade variables:
  private long sortingRangeSubmitCovered = 0; // diagnostic only
  private long sortingTasksSubmited = 0;  
  private long sortingTasksDone = 0;

  private long mergeTasksSubmited = 0;  
  private long mergeTasksDone = 0;
  
  // dynamic merging cascade variables:
  private int mergeCascadeIndex = 1;
  private long cascadeTaskCount = 0; // allows to detect the last cascade
  private long cascadeMergeRangeCovered = 0; // diagnostic only 
  
  // common:
  private long allocResource;

  private final Lock lock = new ReentrantLock(true);
  private final Condition condition = lock.newCondition();
  
  
  public TaskPlannerImpl(int threads0, long totalNumbers0, long maxAlloc, String fileName0) throws IOException {
    threads = threads0;
    totalNumbers = totalNumbers0;
    allocatableTotalNumbersMemory = maxAlloc;
    fileName = fileName0;
    zFileName = fileName + ".z";
    
    raf = new RandomAccessFile(fileName, "rw");
    fc = raf.getChannel();
    assert (raf.length() >> Main.log2DataLength == totalNumbers0);
    assert (raf.length() % Main.dataLength == 0);
    
    final DivisionResult allocByThreads = Util.divideByApproximatelyEqualParts(allocatableTotalNumbersMemory, threads);

    // covers entire number range by the ranges to sort:
    sortCoveringIterator = new CoveringIterator(totalNumbers, new SmallFirstDivisionResultIterator(allocByThreads));
    
    mergeMemoryLoopingIterator = new LoopingIterator(new SmallFirstDivisionResultIterator(allocByThreads));
    
    // NB: iterator "mergeGroupingIterator" gives the ranges to merge.
    // Its' group factor is multiplied by 2 each time we start next merging cascade. 
    ResettableIterator<Range> mergeBase = new CoveringIterator(totalNumbers, new SmallFirstDivisionResultIterator(allocByThreads));
    mergeGroupingIterator = new GroupingIterator(mergeBase, 1);

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
  public ResourceTask nextTask(final int id) throws Exception {
    lock.lock();
    try {
      ResourceTask task;
      Range numberRangeToSort = sortCoveringIterator.next();
      if (numberRangeToSort != null) {
          while (true) {
            if (numberRangeToSort.length > allocResource) {
              if (Main.debug) { out.println("##### planner: waiting for available resources..."); }
              // wait while next sorting task completed
              // to increase the allocResource:
              condition.await();
            } else {
              // create and submit next sorting task:
              task = createSortingTaskImpl(id, numberRangeToSort);
              // update:
              sortingTasksSubmited++;
              sortingRangeSubmitCovered += numberRangeToSort.length;
              allocResource -= numberRangeToSort.length;
              assert (allocResource >= 0);
              return task;
            }
          }
      } 
      assert (sortingRangeSubmitCovered == totalNumbers); // all sort submitted
      assert (allocResource >= 0); // must not be negative
      // wait until all sorting tasks are finished:
      while (sortingTasksDone < sortingTasksSubmited) {
        condition.await();
      } 
      assert (sortingTasksDone == sortingTasksSubmited);
      if (Main.debug) { out.println("############## planner: all sorting tasks are finished."); }

      if (sortingTasksDone > 1) {
        // Need merge. So, allocate temp file for merging:
        if (fcZ == null) {
          allocateZFile();
        }
        ResourceTask t = getNextMergeTask(id);
        if (t != null) {
          return t;
        }
      }
      
      if (Main.debug) { out.println("##### planner: all tasks are submitted."); }
      return null; // all tasks submitted.
    } finally {
      lock.unlock();
    }
  }

 //----------------------------------------------------
 // cyclomatic overview:
//  while(true) {
//  
//    while (true) {  
//      if (!allowed) {
//        condition.await();
//      } else if (...) {
//        return nextTask(); // for this cascade
//      } else {
//        break; // end of *this* cascade 
//      }
//    }
//  
//    // wait the current cascade to complete:  
//    while(!allProcessed) {
//       condition.await();  
//    }
//  
//    cascade++;
//    if (noMoreCascades) {
//      return null;  
//    }   
//  }
  // -----------------------------------------------
  private ResourceTask getNextMergeTask(final int id) throws InterruptedException {
    FileChannel srcFc, dstFc;
    // loop by the merging cascades:
    while (true) {
      // 1st mergeCascadeIndex == 1, so 1st time go by 'true' branch:
      // set refs accordingly to the current cascade index:
      if (isDestinationZFile()) {
        srcFc = fc;
        dstFc = fcZ;
        if (Main.debug) { out.println(" DST = Z."); }
      } else {
        srcFc = fcZ;
        dstFc = fc;
        if (Main.debug) { out.println(" DST = original."); }
      }
      
      final Range leftMergeRange = mergeGroupingIterator.next();
      if (leftMergeRange != null) {
        final Range rightMergeRange = mergeGroupingIterator.next(); // NB: it may be null
      
        final Range allocRange = mergeMemoryLoopingIterator.next();
        final long alloc = allocRange.length;
        while (true) {
          if (alloc > allocResource) {
            if (Main.debug) { out.println("Planner: Merge: waiting for available resources."); }
            // wait while some merging task completed
            // to increase the allocResource:
            condition.await();
          } else {
            break; // ok, necessary resources available
          }
        }
      
        // plan next task:
        cascadeTaskCount++;
        cascadeMergeRangeCovered += leftMergeRange.length;
        if (rightMergeRange != null) {
          cascadeMergeRangeCovered += rightMergeRange.length;
        }
        return nextInCurrentMergeCascade(id, alloc, leftMergeRange, rightMergeRange, srcFc, dstFc);
      }
      
      // wait the current merge cascade to finish completely:
      while (mergeTasksDone < mergeTasksSubmited) {
        condition.await();
      }
      assert (mergeTasksDone == mergeTasksSubmited);
      assert (cascadeMergeRangeCovered == totalNumbers);
      assert (mergeGroupingIterator.next() == null);

      if (cascadeTaskCount == 1) { // the last cascade
        if (Main.debug) { out.println("====================================== all merge tasks submitted."); }
        return null; // all merging tasks submitted.
      }
      
      mergeCascadeIndex++; // go to the next cascade. (1 is the 1st merging cascade).
      mergeMemoryLoopingIterator.reset();
      
      mergeGroupingIterator.reset();
      mergeGroupingIterator.setGroupSize(1 << (mergeCascadeIndex - 1));
      
      cascadeTaskCount = 0;
      cascadeMergeRangeCovered = 0;
      
      if (Main.debug) { 
        out.println(cascadeTaskCount + " pieces merged in current cascade." );
        out.println("================================= cascade " +mergeCascadeIndex+ " started." );
      }
    }
  }
  
  private ResourceTask nextInCurrentMergeCascade(int id, long alloc, 
        Range left, Range right,
        FileChannel srcFc, FileChannel dstFc) {
      assert (left != null);
      assert (right == null || left.start + left.length == right.start);
      // compose next merge task:
      // NB: 2nd element may not be present. In such case the merge is simply copying to 
      // the other file:
      final long rightLen = (right == null) ? 0 : right.length; 
      // create task:
      ResourceTask task = createMergingTask(id, alloc, 
          left.start, left.length, rightLen, srcFc, dstFc);
      // update:
      mergeTasksSubmited++;
      allocResource -= alloc;
      assert (allocResource >= 0);
      return task;
  }
  
  private boolean isDestinationZFile() {
    return (mergeCascadeIndex % 2) != 0;
  }
  
  @Override
  public void done(ResourceTask rt) {
    lock.lock();
    try {
      if (rt.isSorting) {
        sortingTasksDone++;
      } else {
        mergeTasksDone++;
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

  private ResourceTask createSortingTaskImpl(final int taskId, final Range range) {
    assert (range.start >= 0);
    assert (range.length > 0);
    final Resource r = new Resource(range.length);
    if (Main.debug) { 
      out.println("========================== Planned sorting task: ");
      out.println("id         = " + taskId);
      out.println("start pos  = " + range.start);
      out.println("len        = " + range.length);
      out.println("==========================");
    }
    return new ResourceTask(taskId, r, true) {
      @Override
      public Void call() throws Exception {
        InplaceSortDataProvider provider = createProvider(range.start, range.length);
        InplaceSort inplaceSort = createInplaceSort(provider);
        
        inplaceSort.sort(); // ** sort
        provider.force();   // flush
        provider.dispose(); // cleanup and dispose the underlying buffer.
        
        return null;
      }
    };

  }
  
  protected InplaceSortDataProvider createProvider(long startNumPos, long numLen) throws Exception {
    assert (startNumPos >= 0);
    assert (numLen > 0);
    
    long bytePos = startNumPos << Main.log2DataLength ;
    long byteLength = numLen << Main.log2DataLength;
    
    // otherwise we cannot create the byte buffer, whose length is int: 
    assert (byteLength <= Integer.MAX_VALUE);
    
    MappedByteBuffer mbb = fc.map(MapMode.READ_WRITE, bytePos, byteLength);
    mbb.order(Main.byteOrder);
    
    assert (byteLength == mbb.capacity());
    
    return new ByteBufferInplaceSortDataProvider(mbb, -1/*1-based index*/);
  }
  
  protected InplaceSort createInplaceSort(InplaceSortDataProvider provider) {
    return new ChoiceQuickHeapInplaceSort(provider);
  }
  
  /*
   * Allocates temp file for merge of the same length that the input data file.
   */
  private void allocateZFile() throws IOException {
    rafZ = new RandomAccessFile(zFileName , "rw");
    rafZ.setLength(totalNumbers << Main.log2DataLength);
    fcZ = rafZ.getChannel();
  }
  
  private void closeFiles() throws IOException {
    fc.force(false);
    fc.close();
    raf.close();
    
    if (fcZ != null) {
      fcZ.force(false);
      fcZ.close();
    }
    if (rafZ != null) {
      rafZ.close();
    }
  }
  
  private ResourceTask createMergingTask(final int taskId, 
      final long allowedSummaryNumBuffersLength, 
      final long startNumPos, final long leftNumLen, final long rightNumLen,
      final FileChannel srcFc, final FileChannel dstFc) {
    assert (startNumPos >= 0);
    assert (leftNumLen > 0);
    assert (rightNumLen >= 0); /// right length may be null. In such case this is copying. 
    final Resource r = new Resource(allowedSummaryNumBuffersLength);
    if (Main.debug) { 
      out.println("========================== Planned Merging task: ");
      out.println("id         = " + taskId);
      out.println("start pos  = " + startNumPos);
      out.println("left  len  = " + leftNumLen);
      out.println("right len  = " + rightNumLen);
      out.println("==========================");
    }
    return new ResourceTask(taskId, r, false) {
      @Override
      public Void call() throws Exception {
        final BufferedMerger merger = new BufferedMerger(allowedSummaryNumBuffersLength);
        // NB: start pos equal in arc and dst files.
        merger.setLeft(srcFc,  startNumPos, leftNumLen);
        merger.setRight(srcFc, startNumPos + leftNumLen, rightNumLen);
        merger.setTarget(dstFc, startNumPos);
        merger.init();
        try {
          merger.merge();
        } finally {
          merger.dispose();
        }
        return null;
      }
    };
  }
  
  @Override
  public void finish() throws IOException {
    closeFiles();
    
    File z = new File(zFileName);
    if (isDestinationZFile() && mergeTasksDone > 0) {
      File original = new File(fileName);
      if (!original.exists()) {
        throw new IOException("Cannot find original file ["+original.getAbsolutePath()+"].");
      }
      // delete original:
      original.delete();
      if (original.exists()) {
        throw new IOException("Cannot remove original file ["+original.getAbsolutePath()+"]. The sorting result is in file ["+z.getAbsolutePath()+"].");
      }
      // move temp to original:
      if (!z.exists()) {
        throw new IOException("Cannot find temp file ["+z.getAbsolutePath()+"].");
      }
      z.renameTo(original);
      if (!original.exists()) {
        throw new IOException("Failed to move the data file ["+z.getAbsolutePath()+"] to ["+original.getAbsolutePath()+"].");
      } else {
        if (Main.debug) { out.println("Successfully moved z-file to original name."); }
      }
    } else {
      // remove temp:
      if (z.exists()) {
        z.delete();
      }
      if (z.exists()) {
        throw new IOException("Failed to remove temporary file ["+z.getAbsolutePath()+"].");
      } else {
        if (Main.debug) { out.println("Successfully removed tmp file " + z.getAbsolutePath()); }
      }
    }
  }
}
