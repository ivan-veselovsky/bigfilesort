package edu.bigfilesort.parallel;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import edu.bigfilesort.Main;
import static java.lang.System.out;

public class Exec {

  public Exec() {
  }

  /**
   * Executes tasks with help of planner. Planer allowed to decide on its own
   * when to submit the next task and what exactly it will do. This allows to
   * wait for necessary "sync-bar" points.
   */
  public void executeWithPalnner(final int threads, final long timeout, final TimeUnit unit,
      final TaskPlanner planner) throws Exception {
    if (threads == 0) {
      throw new IllegalArgumentException("No threads.");
    }
    final ErrorHandler errorHandler = new ErrorHandler();
    // NB: unlimited capacity queue *required* there because there is some time
    // gap between resource freeing (returning it to planner) and actual finishing
    // of the task in the executor:
    final ExecutorService executor = Executors.newFixedThreadPool(threads);
    try {
      int commandSeqNum = 0;
      if (Main.debug) {
        out.println("=============== exec started.");
      }
      while (true) {
        if (errorHandler.shouldAbort()) {
          break;
        }
        // plan next tasks:
        commandSeqNum++;
        final ResourceTask task = planner.nextTask(commandSeqNum);
        if (task == null) {
          // no more tasks:
          --commandSeqNum;
          if (Main.debug) {
            out.println("+++ exec: all tasks submitted.");
          }
          break;
        } else {
          if (errorHandler.shouldAbort()) {
            break;
          }
          executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
              try {
                if (errorHandler.shouldAbort()) {
                  return null;
                }
                final long startTime = System.currentTimeMillis();
                out.println("exec: started task [" + task + "]");
                try {
                  task.call();
                  final long tookMs = System.currentTimeMillis() - startTime;
                  out.println("exec:       finished task [" + task + "], took " + tookMs + " ms");
                } catch (Exception e) {
                  errorHandler.reportThrowable(e);
                  final long tookMs = System.currentTimeMillis() - startTime;
                  out.println("exec:       FAILED task [" + task + "], took " + tookMs + " ms");
                }
              } catch (Throwable t) {
                errorHandler.reportThrowable(t);
                throw new RuntimeException(t);
              } finally {
                // return the task (resource) to the planner:
                planner.done(task);
              }
              return null;
            }
          });
        }
      }
    } finally {
      executor.shutdown();
      if (errorHandler.shouldAbort()) {
        out.println("Will abort after waiting for all the commands to finish...");
      }
      boolean finishedOk = executor.awaitTermination(timeout, unit);
      errorHandler.printErrors();
      if (!finishedOk) {
        throw new TimeoutException("Executor did not finish all tasks in ${timeout} ${unit}s.");
      }
      errorHandler.abortIfNeeded();
    }
    if (Main.debug) {
      out.println("=============== exec finished.");
    }
  }
}
