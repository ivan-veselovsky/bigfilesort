@echo off
rem The sorting utility.
rem Run w/o parameters for usage.
set MEM=14
java -Xms%MEM%m -Xmx%MEM%m -XX:MaxPermSize=%MEM%m -XX:MaxDirectMemorySize=1600m -cp target/bigfilesort-0.0.1-SNAPSHOT.jar edu.bigfilesort.Main %*
