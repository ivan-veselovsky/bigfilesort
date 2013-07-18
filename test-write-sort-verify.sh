#!/bin/bash -ex

FILE=$1
SIZE=$2
THREADS=$3
ALLOC_MB=$4

./write-data-file.sh "${FILE}" "${SIZE}" rand

./bigfilesort.sh  "${FILE}" "${THREADS}" "${ALLOC_MB}"

./check-sorting.sh "${FILE}" 

