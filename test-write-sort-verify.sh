#!/bin/bash -e

if [ "$#" -lt 4 ]; then
  echo "Generates data file with random data, sorts it, then verifies the sorting."
  echo "Parameters: [-c] [-m] <file_name> <file_size[k,m,g,t]> <number_of_threads> <max_native_memory_alloc_Mb>"
  echo "-c, -m options are to be passed to 'bigfilesort' script."
  exit 1
fi

BIGFILESORT_OPTS=""
while [ "$#" -gt 4 ]; do
  BIGFILESORT_OPTS="${BIGFILESORT_OPTS} ${1}"
  shift
done

FILE=$1
SIZE=$2
THREADS=$3
ALLOC_MB=$4

./write-data-file.sh "${FILE}" "${SIZE}" rand

./bigfilesort.sh ${BIGFILESORT_OPTS} "${FILE}" "${THREADS}" "${ALLOC_MB}"

./check-sorting.sh "${FILE}" 

