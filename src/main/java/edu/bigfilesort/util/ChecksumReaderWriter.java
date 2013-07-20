package edu.bigfilesort.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class ChecksumReaderWriter {
  
  public static final String checksumFileSuffix = ".checksum"; 
  
  private static final String lengthKey = "length";
  private static final String xorKey = "xor";
  private static final String sumKey = "sum";

  private final String checksumFileName;
  
  public ChecksumReaderWriter(String checksumFileName0) {
    checksumFileName = checksumFileName0;
  }
  
  public Checksum readChecksum() throws IOException {
    Properties p = new Properties();
    InputStream is = new FileInputStream(checksumFileName);
    try {
      p.load(is);
    } finally {
      is.close();
    }
    
    long len = getLong(p, lengthKey);
    int  xor = getInt(p, xorKey);
    int  sum = getInt(p, sumKey); 
    
    return new Checksum(len, xor, sum);
  }
  
  private long getLong(Properties p, String key) {
    String valueStr = p.getProperty(key);
    long v = Long.parseLong(valueStr, 16);
    return v;
  }

  private int getInt(Properties p, String key) {
    String valueStr = p.getProperty(key);
    // NB: value parsed through long because Integer.parseInt() expects signed value, while
    // Integer.toHexString() returns unsigned value. 
    long vLong = Long.parseLong(valueStr, 16);
    int v = (int)vLong;
    return v;
  }
  
  public void writeChecksum(Checksum sum, String checksummedFileName) throws IOException {
    Properties p = new Properties();
    
    p.put(lengthKey, Long.toHexString(sum.length));
    p.put(xorKey, Integer.toHexString(sum.xor));
    p.put(sumKey, Integer.toHexString(sum.sum));
    
    OutputStream os = new FileOutputStream(new File(checksumFileName));
    try {
      p.store(os, "Checksum of file ["+checksummedFileName+"]");
    } finally {
      os.close();
    }
  }

}
