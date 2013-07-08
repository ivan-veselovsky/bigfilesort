package edu.bigfilesort.parallel;

import java.io.IOException;



public interface TaskPlanner {
  
    ResourceTask nextTask(int id) throws Exception;
    
    void done(ResourceTask rt);

    void finish() throws IOException;
}
