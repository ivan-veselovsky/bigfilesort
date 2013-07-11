package edu.bigfilesort.kmerge;

public class KMerger implements Runnable {

  /*
   * K input data streams to be merged into one output stream. 
   */
  protected final InStream[] inputStreams;
  
  protected final OutStream outputStream;
  
  protected KSorter kSorter;

  public KMerger(InStream[] ins, OutStream out, KSorter ks) {
    inputStreams = ins;
    outputStream = out;
    kSorter = ks;
  }
  
  @Override
  public void run() {
    final NumberContainer[] containers = new NumberContainer[inputStreams.length];
    
    for (int i=0; i<containers.length; i++) {
      containers[i] = new NumberContainer(inputStreams[i]);
      containers[i].loadNext(); // load 1st element, if any
    }
    
    kSorter.putValues(containers); // set pre-loaded containers
    
    NumberContainer min;
    while (true) {
      kSorter.reSort(); // re-sort the values
      min = kSorter.removeMinAndReload();
      if (min == null) {
        break; //   TODO: clarify contract
      } 
      outputStream.putNext(min.number); // write the value to the out stream.
    }
  }
  
}
