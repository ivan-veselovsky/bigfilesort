The solution in general follows the initial plan: sorting by independent pieces + merging to auxiliary file. Some tips that may need to be noted:
- sorting algorithms are: dual pivot quick sort (code borrowed from JDK7) with a fallback to heap sort (instead of merge sort used in JDK7).
- mapped memory used for sorting and direct memory for merging. Cannot say that there is some special intention under that, rather this just happened to be implemented in such way. Only reason is that direct memory behaves more predictable (at least in terms of max limit). Due to direct memory usage -XX:MaxDirectMemorySize= may need to be adjusted in some cases.
- Unfortunately I did not find time to incorporate net sorting at the end of the algorithm, however, the tool even in the current state shows satisfactory performance (see data below).

The project is buildable from checkout (mvn clean install -DskipTests). Tests also can be run, but that will take about 6 minutes and will generate some files in the current directory.
 Shell script launchers are present. Command line parameters of the scripts are explained when running them without parameters.

I carried out the experiments primarily on CentOS 6.3 box (4-core, 16G ram, ordinary hard disk) using JDK6.
The following performance data observed on files filled with random data:
 
threads   max-native (Mb)   sorttime,min:sec file size (bytes)
7         600               7:16             4G
4         600               6:46             4G 
1         600               13:48            4G

7         600               1:15             1G
4         600               1:27             1G
1         600               3:08             1G

7         32                1:53             1G
7         128               1:21             1G
7         257               1:30             1G
7         1200              1:20             1G

4         2048              6:33             4G
5         2048              30:6             10G
5         2048              152:44           32G
------------------------------------------------------
Windows-32 (2-core, 2G RAM, JDK7):
1         769               9:30             1.48G (movie)  
2         1024              8:15             1.48G (movie)

1         793  (*)          8:48             1.48G (movie)   
2         1580 (**)         5:41             1.48G (movie)  

(*)  -- 793m appears to be the largest amount of continously allocatable mapped memory on the used Win machine (where this limit comes from??)
(**) -- 1580m is ~793m*2: 2 max allocatable mapped regions used, so the results are much better than in the previous case.
------------------------------------------------------

Here the "max-native" parameter is maximum amount of (direct + mapped) memory allowed to be used by the program (this is the 3rd command line parameter of "bigfilesort.sh"). This parameter was introduced in order to make the tool more reliable and stable (experience shows that on windows it's impossible to allocate mapped memory larger than some limit, and I don't know where this limit value comes from.)

The performance data show that: 
1) we fit the 1G/15min requirement for single thread: we have even ~5 times better result;
2) increasing the number of threads from 1 to number of processor cores (4 in my case) increases performance significantly, but further number of threads grows does make the situation even worse;
3) The native memory limit does really affect the performance only in case of small values (tens megabytes and lower). (Because that leads to huge number of very small sorting tasks with subsequent merging of these small pieces.)

The tool was tested on a number of inputs of various length, including ascending, descending, flat and random sequences. In case of ascending, descending and flat pre-sorting the results are much better than the ones for random data because of the heuristics used in sorting algorithms. Random data provide worse performance, but even in this case a fallback from quick-sort to another stable algorithm typically does not happen. If a fallback to HeapSort happens, the performance degrades by 30-50% (due to coefficient ~3*n*log2(n)).



TODO:
1) planning: if there are less than num of threads tasks in the current cascade, and we don't start next cascade, give all the available resources to the pending tasks (if they may need more resources). Currently this is not the case, and planner always gives Res/threads resource to each task.
2) planning: in some cases it is possible to start tasks in the next cascade while current is not yet finished. Consider to implement at least in simple cases.
3) consider to implement the net sorting in the final stage of the algorythm. The benefit is questionable, should be checked experimentally.
   Related idea that can be used if only 2 threads are available for one merge task: start the merge from 2 ends in parallel, finish when they meet somewhere in between. This is much simplier than the net sort, but only allows merge paralleling in 2 threads, not more, while net sorting allows paralleling in large number of threads (proportional to the length of merged piece).

4) It looks like in many cases RW operations are much slower (or may become such) than the comparison operations in memory. This leads to the following idea: what if we will merge not pairs of pieces, but "k" sorted pieces at once? In such case we need in O(log2(k)) times more comparison operations for entire merge, but we reduce the number of R/W operations in log2(k) times. If the R/W operations are really the bottleneck, we would optain significant benefit in performance, even thouh general solution assimptotic of N*log2(N) remains the same.

