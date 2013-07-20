package edu.bigfilesort;

import edu.bigfilesort.radix.FileStorage;
import edu.bigfilesort.radix.Storage;
import edu.bigfilesort.util.Checksum;
import edu.bigfilesort.util.ChecksumBuilder;
import edu.bigfilesort.util.ChecksumReaderWriter;

/**
 * Is to be used to checksum a file before sorting.  
 */
public class WriteChecksumMain {

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      System.out.println("Parameters: <name of the file to be checksummed>");
      System.exit(1);
    }
    final String file = args[0];

    long t = System.currentTimeMillis();
    Storage storage = new FileStorage(file, true/*readonly*/);
    int bufNum = (int)(WriteDataMain.bufferSizeBytes >> Main.log2DataLength);
    Checksum checksum = ChecksumBuilder.calculateChecksum(storage, bufNum);
    
    ChecksumReaderWriter crw = new ChecksumReaderWriter(file + ChecksumReaderWriter.checksumFileSuffix);
    crw.writeChecksum(checksum, file);
    long delta = System.currentTimeMillis() - t;
    System.out.println("Checksum of ["+file+"] written in "+delta+" ms.");
  }

}
