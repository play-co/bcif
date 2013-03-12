package bcif;

/**
 * <p> Class created to write bitmap data into a byteMatrix, that can then be used to 
 * create a BmpImage Object.</p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class byteMatrixBmpWriter implements bmpWriter {

  private int width;
  private int height;
  private byteMatrix bm = null;
  int x = 0;
  int y = 0;
  int c = 0;
  int dph = 0;
  int biXPelsPerMeter = 0;
  int biYPelsPerMeter = 0;

  public byteMatrixBmpWriter() {

  }

	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
  public void setDims(int w, int h) {
    width = w;
    height = h;
    bm = new byteMatrix(w, h, 3);
    bm.firstVal();
    dph = w * 3;
  }

  public void writeBmpIntest() {    
		// Nothing to do
  }
  
  public void setRes(int resX, int resY) {
  	biXPelsPerMeter = resX;
  	biYPelsPerMeter = resY; 	
  }
  
  public int getResX() {
  	return biXPelsPerMeter;
  }

  public int getResY() {
  	return biYPelsPerMeter;
  }
  
  public byteMatrixBmpWriter(int w, int h) {
    width = w;
    height = h;
    bm = new byteMatrix(w, h, 3);
    bm.firstVal();
    dph = w * 3;
  }

  public void writeTriplet(byte b, byte g, byte r) {
    bm.setCurVal(b);
    bm.setCurVal(g, 1);
    bm.setCurVal(r, 2);
    bm.nextVal(3);
  }


  public void write(byte val) {
    bm.setCurVal(val);
    bm.nextVal();
  }

  public void close() {

  }

  public byteMatrix getMatrix() {
    return bm;
  }
}
