package bcif;
import java.io.*;

/**
 * <p> Class for the encoding of single bits in an output stream. Contains also the methods
 * to write more complex data following the BCIF conventions.</p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class bitWriter {

  private OutputStream out = null;
  private int buffer = 0;
  private int bitCount = 0;
  private int wroteBits = 0;

  public bitWriter(OutputStream o) {
    out = o;
  }

  public void writeBit(int bit) {
    buffer = (buffer >> 1) + (bit << 7);
    bitCount++;
    if (bitCount == 8) {
      try {
        out.write(buffer);
        buffer = 0;
        bitCount = 0;
      }
      catch (IOException ex) {
        ex.printStackTrace();
      }
    }
    wroteBits ++;
  }

  public void fwrite(int num, int bitNum) {
    while (bitNum > 0) {
      writeBit(num & 1);
      num = num >> 1;
      bitNum--;
    }
  }

  public void writeVbit(int num) {
    writeVbit(num, 0);
  }

  public void writeOnef(int num) {
    for (int i = 0; i < num; i++) {
      writeBit(1);
    }
    writeBit(0);
  }

  public void close() {
    while (bitCount > 0) {
      writeBit(0);
    }
    try {
      out.close();
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void writeVbit(int num, int initbits) {
    int bitNum = initbits;
    int repnum = (1 << bitNum) - 1;
    int precrep = -1;
    while (num > repnum) {
      bitNum++;
      precrep = repnum;
      repnum = repnum + (1 << bitNum);
    }
    for (int i = initbits; i < bitNum; i++) {
      writeBit(1);
    }
    writeBit(0);
    fwrite(num - precrep - 1, bitNum);
  }

  public void writeBits(String bits) {
    byte[] b = bits.getBytes();
    for (int i = 0; i < b.length; i++) {
      if (b[i] == 48) {
        writeBit(0);
      }
      else {
        writeBit(1);
      }
    }
  }

  public int getWroteBits() {
    return wroteBits;
  }

  public static void main(String[] args) {
    try {
      OutputStream out = new FileOutputStream("test");
      bitWriter bw = new bitWriter(out);
      int testNum = 1000;
      int testNum2 = 8;
      for (int i = 0; i < testNum; i++) {
        for (int i2 = 0; i2 < testNum2; i2++) {
          bw.writeVbit(i, i2);
        }
      }
      for (int i2 = 0; i2 < testNum2; i2++) {
        for (int i = 0; i < (1 << i2); i++) {
          bw.fwrite(i, i2);
        }
      }
      bw.close();
      InputStream in = new FileInputStream("test");
      bitReader br = new bitReader(in);
      boolean ok = true;
      for (int i = 0; i < testNum; i++) {
        for (int i2 = 0; i2 < testNum2; i2++) {
          int val = br.readVbit(i2);
          if (i != val) {
            System.err.println("Error in read/write var bit: " + i + " != " + val);
            ok = false;
          }
        }
      }
      for (int i2 = 0; i2 < testNum2; i2++) {
        for (int i = 0; i < (1 << i2); i++) {
          int val = br.fread(i2);
          if (i != val) {
            System.err.println("Error in read/write form bit: " + i + " != " + val);
            ok = false;
          }
        }
      }
      System.out.println();
      if (ok) { System.out.println("Test is ok"); }
    }
    catch (Exception e) { e.printStackTrace(); }
  }

}

