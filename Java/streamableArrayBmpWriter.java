package bcif;
import java.awt.image.*;

/**
 * <p> Class implementing the writing of BMP image data in a streamable byteMatrix. 
 * Allows streamable viewing of BCIF files. </p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class streamableArrayBmpWriter extends arrayBmpWriter implements bmpWriter {

  private int cur = 255 << 24;
  private int initcur = 255 << 24;
  private int wrote = 0;
  private int[] out = null;
  private int x = 0, y = 0;
  private int outc = 0;
  private boolean view = false;
  private String title = "Untitled";
  private bcifViewer viewer = null;
  private MemoryImageSource external = null;
  private boolean start = false;
  private int lineRefresh = 100;
  private int lrc = 0;
  private int oldy = 0;
  private int parts = 0;

  public streamableArrayBmpWriter() {

  }

  public void setParts(int p) {
    parts = p;
  }

  public void setView(boolean v) {
    if (! start) { view = v; }
  }

  public void setTitle(String t) {
    title = t;
  }

  public void setExternal(MemoryImageSource mis) {
  	external = mis;
  }
  
  public void setOut(int[] o) {
  	out = o;
  }
  
  /**
   * To be called AFTER setView
   *
   */
     
  public void setDims(int width, int height) {
    start = true;
    y = height - 1;
    oldy = y;
    outc = (height - 1) * width;
    if (out == null) { out = new int[width * height]; }
    super.width = width;
    super.height = height;
    if (parts > 0) { lineRefresh = height / parts; }
    if (view && external == null) { viewer = bcifViewer.view(out, width, height, title); }
  }

  public void writeTriplet(byte b, byte g, byte r) {
    out[outc] = initcur | ((b < 0 ? b + 256 : b))
                        | ((g < 0 ? g + 256 : g) << 8)
                        | ((r < 0 ? r + 256 : r) << 16);
    outc++;
    x++;
    if (x == width) {
      x = 0;
      y = y - 1;
      outc -= (width << 1);
      lrc++;
      if (view && lrc == lineRefresh) {
      	if (external == null) {
        	viewer.getSource().newPixels(0, y, width, lineRefresh + 1);
        } else {
        	external.newPixels(0, y, width, lineRefresh + 1);
        }
        oldy = y;
        lrc = 0;
      }
    }
  }

  public void write(byte v) {
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
        lrc++;
        if (view && lrc == lineRefresh) {
          viewer.getSource().newPixels(0, y, width, lineRefresh + 1);
          oldy = y;
          lrc = 0;
        }
      }
    }
  }

  public void close() {
    if (view) {
      if (external == null) {
        viewer.getSource().newPixels();
      } else {
      	external.newPixels();
      }
    }
  }
}
