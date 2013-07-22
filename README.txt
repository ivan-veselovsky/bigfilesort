The solution provides 2 sorting modes: 
1) radix + statistics sorting (default)
2) in-place comparison sorting of independent pieces + merging to auxiliary file. (Enabled with "-c" option) 
Some tips that may need to be noted:
- sorting algorithms in comparison sorting more are: dual pivot quick sort (code borrowed from JDK7) with a fallback to heap sort (instead of merge sort used in JDK7).
- mapped memory used for in-place sorting. Direct or mapped memory used for merging and radix data moving as controlled by "-m" option.
 Experiments show that the performance for the 2 memory variants is different on different systems, so there is the choice. Due to direct memory usage -XX:MaxDirectMemorySize= may need to be adjusted in some cases.

The project is buildable from checkout (mvn clean install -DskipTests). Tests also can be run, but that will take about 6 minutes.
Shell script launchers for the sorting program (bigfilesort) as well as data generators and sorting/checksum verifiers are present. 
Command line parameters of the scripts are explained when running them without parameters.

Performance results overview.
I carried out the experiments primarily on CentOS 6.3 box (4-core, 16G ram, ordinary hard disk) using JDK6, and also on Win-32 (2-core, 2Gb ram) machive.

1) For radix sort (default) good results are observed even on small data files (1G file sorted in ~50 sec on CentOS box). But, what is more important, this sorting method
shows linear time vs. data file size curve with time coefficient ~3.2min/Gb, as opposed to comparison sort variant that shows N*log(N) curve.
Parallelling of radix-based solution does not show good benefit: e.g. using 4 threads instead of 1 improves the result only by 10-15%. This is because CPU usage is low, 
and the bottleneck is IO operations, that are not parallelled well on used environments.

2) For comparison sort algorythms the results are worse for any data files on CentOS machine, but in some cases are better on Windows machine. 
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
5         2048              384:49           64G
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
1) in any case we fit the 1G/15min requirement for single thread: we have even ~2-18 times better results;
2) increasing the number of threads from 1 to number of processor cores (4 in my case) increases performance, but not very significantly (not higher than 30-40%). 
3) The native memory limit does really affect the performance only in case of small values (tens megabytes and lower). (Because that leads to huge number of very small sorting tasks with subsequent merging of these small pieces.)

Note about the comparison algorithms used. 
The tool was tested on a number of inputs of various length, including ascending, descending, flat and random sequences. In case of ascending, descending and flat pre-sorting the results are much better than the ones for random data because of the heuristics used in sorting algorithms. Random data provide worse performance, but even in this case a fallback from quick-sort to another stable algorithm typically does not happen. If a fallback to HeapSort happens, the performance degrades by 30-50% (due to coefficient ~3*n*log2(n)).


