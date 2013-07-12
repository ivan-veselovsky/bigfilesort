# The sorting utility.
# Run w/o parameters for usage.
MEM=14
java -Xms${MEM}m -Xmx${MEM}m -XX:MaxPermSize=${MEM}m -XX:MaxDirectMemorySize=2100m -cp target/bigfilesort-0.0.1-SNAPSHOT.jar edu.bigfilesort.Main "${@}"
