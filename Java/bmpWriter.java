package bcif;

/**
 * <p> Interface for the writing of Bitmap image data on some output, as a file or a
 * byteMatrix. </p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public interface bmpWriter {

  public void setDims(int width, int height);
  
  public void setRes(int resX, int resY);
  
  public int getWidth();
  
  public int getHeight();

  public void writeBmpIntest();

  public void write(byte v);

  public void writeTriplet(byte b, byte g, byte r);

  public void close();

}