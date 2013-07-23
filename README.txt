The solution provides 2 sorting modes: 
1) radix + statistics sorting (default)
2) in-place comparison sorting of independent pieces + merging to auxiliary file. (Enabled with "-c" option) 
Some tips that may need to be noted:
- sorting algorithms in comparison sorting mode are: dual pivot quick sort (code borrowed from JDK7) with 
  a fallback to heap sort (instead of merge sort used in JDK7).
- mapped memory used for in-place sorting. Direct or mapped memory used for merging and radix data moving, as controlled by "-m" program option.
  Experiments show that the performance for the 2 memory variants is different on different systems, so the choice is allowed. 
  Due to direct memory usage -XX:MaxDirectMemorySize=... may need to be adjusted (see java options in the scripts).

The project is buildable from checkout (mvn clean install [-DskipTests]). Tests also can be run, ant should pass, but that will take about 6 minutes.
Shell script launchers for the sorting program (bigfilesort.sh/cmd) as well as data generators and sorting/checksum verifiers are present. 
Command line parameters of the scripts are explained when running them without parameters.

Performance results overview.
Experiments ere carried out primarily on CentOS 6.3 box (4-core, 16G ram, ordinary hard disk) using JDK6, and Win-32 (2-core, 2Gb ram) machive using JDK7.

1) For radix sort (default option) good results are observed even on small data files (1G file sorted in ~50 sec on the CentOS box). 
And, what is even more important, this sorting method shows linear sorting time vs. data file size curve with time coefficient ~2.7min/Gb, 
as opposed to comparison sort variant that fits N*log(N) curve.
Parallelling of radix-based solution does not show a benefit, e.g. using 4 threads instead of 1 makes the result worse by ~15%. 
This is likely because CPU usage is low, and the bottleneck is the I/O operations, that are not parallelled well on used environments. Besides, in case 
of radix for 1-thread running we use an optimization that utilizes primitive type counters, while for multi-threaded running we have to 
use atomic counters those are much slower. However, we believe that parelleling may give a benefit on systems with well-parallellable I/O operations.

Results of Radix sort mode are shown in the table (CentOS machine, random data file).
(Here the "max-native" parameter is maximum amount of (direct + mapped) memory allowed to be 
used by the program (this is the 3rd command line parameter of "bigfilesort.sh"). This parameter 
was introduced because the maximum amount of direct memory that JVM can allocate 
is limited (limit set with -XX:MaxDirectMemorySize parameter). Also this needed in order to make the tool 
more reliable (experience shows that on windows it's impossible to allocate mapped or direct memory 
larger than some limit, and we don't know where this limit value comes from.) 

Radix, CentOS: 
--------------------------------------------------------------
threads   max-native (Mb)   sorttime,sec     file size (bytes)

1         1024              50               1G
4         1024              77               1G

1         1024              250              4G
4         1024              333              4G
 
1         1024              2300             16G


1         1024              4800             32G
4         1024              5280             32G

1         1024              10316            64G
--------------------------------------------------------------
Radix, Win-32 machine:
--------------------------------------------------------------
threads   max-native (Mb)   sorttime,sec     file size (bytes)

1         792               311              1G
1         792               514              1.45G (movie)
--------------------------------------------------------------


2) For comparison sort algorythms the results nearly the same as for radix (for small data files) or worse (for large files) under otherwise equal conditions. 

Note about the comparison algorithms used. 
The tool was tested on a number of inputs of various length, including ascending, descending, flat and random sequences. 
In case of ascending, descending and flat pre-sorting the results are much better than the ones for random data because of the 
heuristics used in dual-pivot quick sort algorithm. Random data provide worse performance, but even in this case a fallback 
from quick-sort to another stable algorithm typically does not happen. If a fallback to HeapSort happens, the performance degrades 
by 30-50% (due to higher coefficient in n*log(n) deendency).

The following performance data observed on files filled with random data.
CentOS: 
--------------------------------------------------------------
threads   max-native (Mb)   sorttime,sec     file size (bytes)

7         600               75               1G
4         600               87               1G
1         600               188              1G

7         32                113              1G
7         128               81               1G
7         257               90               1G
7         1200              80               1G

7         600               436              4G
4         600               406              4G 
1         600               828              4G


4         2048              393              4G
5         2048              1806             10G
5         2048              9164             32G
5         2048              23089            64G
--------------------------------------------------------------
Windows-32 (2-core, 2G RAM, JDK7):

1         792               329              1G  
1         792               488              1.45G (movie) 
--------------------------------------------------------------
(from another measurement series:)
1         769               570              1.45G (movie)  
2         1024              495              1.45G (movie)

1         793  (*)          528              1.45G (movie)   
2         1580 (**)         341              1.45G (movie)  

(*)  -- 793m appears to be the largest amount of continously allocatable direct/mapped memory on the used Win machine (where this limit comes from??)
(**) -- 1580m is ~793m*2: 2 max allocatable mapped regions used, so the results are much better than in the previous case.
--------------------------------------------------------------


The performance data have shown that: 
1) in any case we fit the 1G/15min requirement for single thread: we have even ~2-18 times better results;
2) increasing the number of threads from 1 to number of processor cores (4 in my case) typically increases performance in case of comparison sorting, 
  but does not increase it in case of radix sorting. This seems to be caused by significant difference in CPU usage and low paralleling potential 
  of the I/O operations on used environments.
3) The native memory limit does really affect the performance only in case of small values (tens megabytes and lower). 
  (Because that leads to huge number of very small buffer shifts (direct buffers) or remaps (in caseof mapped buffers)).
4) Mapped memory buffers show slightly better results on Windows, but slightly worse on Unix.


