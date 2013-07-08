# aux tool.
# checks ascending sorting of big-endian integers in the given file.
java -cp target/bigfilesort-0.0.1-SNAPSHOT.jar edu.bigfilesort.CheckSortedMain "${@}"
