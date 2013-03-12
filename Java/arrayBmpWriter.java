package bcif;

/**
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class arrayBmpWriter implements bmpWriter {

  private int cur = 255 << 24;
  private int initcur = 255 << 24;
  private int wrote = 0;
  private int[] out = null;
  private int x = 0, y = 0;
  private int resx = 0, resy = 0;
  private int outc = 0;
  protected int width, height;
  private boolean view = false;
  private String title = "Untitled";
  private bcifViewer viewer = null;
  private boolean start = false;

  public arrayBmpWriter() {

  }

  public void setRes(int resx, int resy) {
	this.resx = resx;
	this.resy = resy;
  }
  
  public int getWidth() {
  	return width;
  }
  
  public int getHeight() {
  	return height;
  }

  public void writeBmpIntest() {
  	// Nothing to to
  }
  
  public void setView(boolean v) {
    if (! start) { view = v; }
  }

  public void setTitle(String t) {
    title = t;
  }

  /**
   * To be called AFTER setView
   *
   */
   
  public void setDims(int width, int height) {
    start = true;
    y = 255;
    outc = (height - 1) * width;
    out = new int[width * height];
    this.width = width;
    this.height = height;
  }

  public void writeTriplet(byte b, byte g, byte r) {
    out[outc] = initcur | ((b < 0 ? b + 256 : b))
                        | ((g < 0 ? g + 256 : g) << 8)
                        | ((r < 0 ? r + 256 : r) << 16);
    outc ++;
    x++;
    if (x == width) {
      x = 0;
      y = y - 1;
      outc -= (width << 1);
    }
  }

  public void write(byte v) {
    System.out.println("--");
    cur = cur | ((v < 0 ? v + 256 : v) << (wrote << 3));
    wrote ++;
    if (wrote == 3) {
      wrote = 0;
      out[outc] = cur;
      cur = initcur;
      outc ++;
      x ++;
      if (x == width) {
        x = 0;
        y = y - 1;
        outc -= (width << 1);
      }
    }
  }

  public void close() {
    if (view) {
      viewer = bcifViewer.view(out, width, height, title);
      //viewer.getSource().newPixels();
    }
  }
}
