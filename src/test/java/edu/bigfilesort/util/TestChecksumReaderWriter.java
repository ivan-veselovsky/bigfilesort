package edu.bigfilesort.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestChecksumReaderWriter {

  @Test
  public void testChecksumBasic() {
    final int x = 0xee3c1800;
    String s = Integer.toHexString(x);
    assertEquals("ee3c1800", s);
    long yLong = Long.parseLong(s, 16);
    int y = (int)yLong;
    assertEquals(x, y); 
  }
}
