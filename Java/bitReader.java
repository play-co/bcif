package bcif;
import java.io.*;

/**
 * <p>Class for the reading of single bits from an inputStream. Contains also the methods
 * to read more complex data following the BCIF conventions.</p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class bitReader {

  private InputStream in = null;
  private int bitCount = 0;
  private int buffer = 0;
  public int readBits = 0;

  public bitReader(InputStream i) {
    in = i;
  }

  public int readBit() {
    if (bitCount == 0) {
      try {
        buffer = in.read();
        bitCount = 8;
      }
      catch (IOException ex) { ex.printStackTrace(); }
    }
    bitCount --;
    int res = buffer & 1;
    buffer = buffer >> 1;
    readBits ++;
    return res;
  }

  public int getReadBits() {
    return readBits;
  }

  public int fread(int bitNum) {
    int res = 0;
    for (int i = 0; i < bitNum; i++) {
      res = res + (readBit() << i);
    }
    return res;
  }

  public int readVbit() {
    return readVbit(0);
  }

  public int readOnef() {
    int res = 0;
    while (readBit() == 1) { res ++; }
    return res;
  }

  public void close() {
    try {
      in.close();
    } catch (Exception e) { e.printStackTrace(); }
  }

  public int readVbit(int initBits) {
    int res = 0;
    int bitNum = initBits;
    int precRep = -1;
    int rep = (1 << initBits) - 1;
    while (readBit() == 1) {bitNum ++; precRep = rep; rep = (rep + (1 << bitNum));}
    return precRep + 1 + fread(bitNum);
  }
}
