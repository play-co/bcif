package bcif;
import java.io.*;

/**
 * <P> This class implements a Thread that reads from an input stream and writes data
 * in an array of bytes. </P>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class readingThread extends Thread {

  private byte [] matrix;
  private int readBytes = 0;
  private boolean running = true;
  private InputStream in = null;
  private int maxReadBlock = 1 << 20;
  private boolean stop = false;
  private final boolean console = false;

    /**
     * Creates a new readinThread specifying its input and output structures.
     * @param in The input stream where to read from
     * @param matrix The output byte matrix where to write
     */

  public readingThread(InputStream in, byte [] matrix) {
    this.matrix = matrix;
    this.in = in;
  }

    /**
     * The method causes the Thread to start reading from the stream; this will end when
     * data is no longer available
     */

  public void run() {
    try {
      int currentReadBytes = 1;
      while (currentReadBytes > -1 && ! stop) {
        currentReadBytes = in.read(matrix, readBytes, matrix.length - readBytes);
        if (currentReadBytes > 0) {
          readBytes += currentReadBytes;
          if (console) { System.out.println("Reader: read " + currentReadBytes / 1024 + " Kbytes"); }
        }
      }
    } catch (Exception e) {
      System.err.println("Error while reading file ");
      e.printStackTrace();
    }
    running = false;
  }

    /**
     * Method to detect if the Thread is still running
     * @return True if the thread is running, false otherwise.
     */

  public boolean running() {
    return running;
  }

    /**
     * Method that gives the number of read bytes at the moment of the call
     * @return The number of readBytes
     */

  public int readBytes() {
    return readBytes;
  }

    /**
     * Method that imposes termination of the Thread
     */

  public void stopThread() {
    stop = true;
  }
}
