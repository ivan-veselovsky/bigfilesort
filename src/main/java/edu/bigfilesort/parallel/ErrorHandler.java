package edu.bigfilesort.parallel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import static java.lang.System.out;

public class ErrorHandler {
  
  // all the errors are accumulated there regardless of the fact if they are 
  // causing execution abort or not:
  private final BlockingQueue<Throwable> errQueue = new LinkedBlockingQueue<Throwable>();
  // this ref contains cause of abort, or null if none:
  private final AtomicReference<Throwable> abortCause = new AtomicReference<Throwable>(null);  
  
  ErrorHandler() {
  }

  /**
   * Reports a Throwable error to the handler.
   */
  void reportThrowable(final Throwable t) throws Exception {
    if (t == null) { throw new IllegalArgumentException("null throwable"); }
    if (abortCause.compareAndSet(null, t)) {
      out.println("***** Abort: " + t);
    }
    errQueue.put(t);
  }

  /**
   * Prints out the abort cause and all the collected errors, if any.
   * Expected to be called on the main thread at the end of execution, when 
   * all the worker threads are joined.   
   */
  void printErrors() {
    final Throwable t = abortCause.get();
    if (t != null) {
      out.println("=== First abort cause: " + t);
    }
    Throwable[] errArr = errQueue.toArray(new Throwable[0]);
    if (errArr.length > 0) {
      out.println("=== All Errors:");
      for (Throwable x: errArr) {
        x.printStackTrace();
      }
    }
  }
  
  /**
   * Re-throws the primary abort cause, if any, as a StatusException.
   * If no abort cause, does nothing.
   * Expected to be called on the main thread when all the 
   * worker threads are joined.
   */
  void abortIfNeeded() throws Exception {
    final Throwable se = abortCause.get();
    if (se != null) {
      throw new RuntimeException(se);
    }
  }
  
  /**
   * Answers if there is a cause of abort.
   * Can be called on any thread.
   * Typically used by the worker threads to know 
   * if they should abort execution. 
   */
  boolean shouldAbort() {
    return (abortCause.get() != null);
  }

}
