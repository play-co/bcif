package bcif;
import java.io.*;

/**
 * <P> This class represents a bitMatrix accessible even if its data is not completely available.
 * At the moment of the creation, data is read parallely. Controls on if the data requested has been
 * read are done only if the data is read sequencially. </P>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class streamableBitMatrix extends bitMatrix {

  private byte[] matrix;
  private int readBytes = 0;
  private boolean reading = true;
  private readingThread reader = null;
  private long sleepTime = 25;
  private int timeoutPerK = 1000;
  private int minTimeout = 10000;
  private long initTime = 0;
  private final boolean console = false;

    /**
     * Creates a new streamableBitMatrix reading its data from a given stream.
     * @param in The inputStream where to read
     * @param maxSize The maximum number of bits to read
     */

  public streamableBitMatrix(InputStream in, int maxSize) {
    super(maxSize, 0);
    matrix = getMatrix();
    reader = new readingThread(in, matrix);
    initTime = System.currentTimeMillis();
    reader.start();
  }

  private void checkAvaiability(int x, int y) {
    if (reading) {
      readBytes = reader.readBytes();
      reading = reader.running();
      if (console && (x >> 3) >= readBytes) { System.out.print("Waiting for byte " + (x >> 3) + " .."); }
      while ((x >> 3) >= reader.readBytes()) {
        if (console) { System.out.print("."); }
        try {
          Thread.sleep(sleepTime);
        }
        catch (Exception e) {}
        if (System.currentTimeMillis() - initTime >
            minTimeout + (readBytes / 1024 * timeoutPerK)) {
          throw new RuntimeException("File reading timed out.");
        }
      }
    }
  }

    /**
     * Gets a specified bit from the matrix. If not available, method will wait.
     * @param x The horizontal coordinate of the bit to read
     * @param y The vertical coordinate of the bit to read
     * @return The requested bit
     */

  public byte getBit(int x, int y) {
    checkAvaiability(x, y);
    return super.getBit(x, y);
  }

    /**
     * Makes a xor with a bit in the matrix. If not available, method will wait.
     * @param x The horizontal coordinate of the bit
     * @param y The vertical coordinate of the bit
     * @param val The other argument of the xor function
     */

  public void xorBit(int x, int y, int val) {
    checkAvaiability(x, y);
    super.xorBit(x, y, val);
  }
}
