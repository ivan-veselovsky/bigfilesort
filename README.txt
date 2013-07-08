The solution in general follows the initial plan: sorting by independent pieces + merging to auxiliary file. Some tips that may need to be noted:
- sorting algorithms are: dual pivot quick sort (code borrowed from JDK7) with a fallback to heap sort (instead of merge sort used in JDK7).
- mapped memory used for sorting and direct memory for merging. Cannot say that there are much of intention under that, rather this just happened to be implemented in such way. Only reason is that direct memory behaves more predictable (at least in terms of max limit). Due to direct memory usage -XX:MaxDirectMemorySize= may need to be adjusted in some cases.
- Unfortunately I did not find time to incorporate net sorting at the end of the algorithm, however, the tool even in the current state shows satisfactory performance (see data below).

The project is buildable from checkout (mvn clean install -DskipTests). Tests also can be run, but that will take about 10 min and will generate some files in the current directory.
 Shell script launchers are present. Command line parameters are explained when running scripts without parameters.

I carried out the experiments primarily on CentOS 6.3 box (4-core, 16G ram).
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

Here the "max-native" parameter is maximum amount of (direct + mapped) memory allowed to be used by the program (this is the 3rd command line parameter). This parameter was introduced in order to make the tool more reliable and stable (experience shows that on windows it's impossible to allocate mapped memory larger that some limit.)

The data show that 
1) we fit the 1G/15min requirement for single thread: we have ~5 times better result;
2) increasing the number of threads from 1 to number of processor cores (4 in my case) increases performance significantly, but further number of threads grows  does makes the situation even worse;
3) The native memory limit does really affect the performance only in case of  small values (tens megabytes and lower). (Because that leads to huge number of very small sorting tasks with subsequent merging of these small pieces.)

The tool was testes on a number of inputs of various length, including ascending, descending, flat and random sequences. In case of ascending, descending and flat pre-sorting the results are much better than the ones for random data because of heuristics used in used algorithms. Random data provide worse performance, but even in this case a fallback from quick-sort to another stable algorithm typically does not happen. If a fallback to HeapSort happens, the performance degrades by 30-50% (due to coefficient ~3*n*log2(n)).
