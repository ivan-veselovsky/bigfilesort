@echo off

set FILE=%1
set SIZE=%2
set THREADS=%3
set ALLOC_MB=%4

call write-data-file.cmd %FILE% %SIZE% rand
call bigfilesort.cmd %FILE% %THREADS% %ALLOC_MB%
call check-sorting.cmd %FILE%