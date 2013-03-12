package bcif;
import java.io.*;

/**
 * <p> Class that allows the writing of bitmap info in a BMP file. </p> 
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class fileBmpWriter implements bmpWriter {

  private OutputStream out;
  private int biWidth;
  private int biHeight;
  private int caxis, xaxis, yaxis, wrote;
  private int adl;
  private int cxaxis;
  private int biXPelsPerMeter = 0;
  private int biYPelsPerMeter = 0;

  public fileBmpWriter(String dest) {
    try {
      out = new BufferedOutputStream(new FileOutputStream(dest));
    }
    catch (FileNotFoundException ex) {
      ex.printStackTrace();
    }
  }

	public int getWidth() {
		return biWidth;
	}
	
	public int getHeight() {
		return biHeight;
	}
	
  public byte[] createIntest() {
    byte[] intest = new byte[54];
    adl = (4 - (int)biWidth * 3 % 4) % 4;
	int wb = 0; //(biHeight * (biWidth * 3 + adl) - 2) & 3;
    int linewidth = biWidth * 3 + adl;
    int bfSize = biHeight * linewidth + 54 + wb;
    if (biXPelsPerMeter == 0) { biXPelsPerMeter = bfSize - 54; }
    intest[0] = (byte) 'B';
    intest[1] = (byte) 'M';
    writeDWord(intest, 2, (int) bfSize);
    intest[10] = 54;
    intest[14] = 40;
    writeDWord(intest, 18, (int) biWidth);
    writeDWord(intest, 22, (int) biHeight);
    intest[26] = 1;
    intest[28] = 24;
    writeDWord(intest, 34, (int) bfSize - 54);
    writeDWord(intest, 38, (int) biXPelsPerMeter);
    writeDWord(intest, 42, (int) biYPelsPerMeter);
    return intest;
  }

  public static void writeDWord(byte[] buffer, int init, int dw) {
    for (int i = init; i < init + 4; i++) {
      buffer[i] = (byte) (dw % 256);
      dw = dw >>> 8;
    }
  }

  public void setDims(int width, int height) {
  	biWidth = width;
  	biHeight = height;
  }

  public void setRes(int resX, int resY) {
  	biXPelsPerMeter = resX;
  	biYPelsPerMeter = resY;
  }
  
  public void writeBmpIntest() {
    byte[] intest = createIntest();
    try {
      out.write(intest);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }
    
  public void writeTriplet(byte b, byte g, byte r) {
    try {
      out.write(b);
      out.write(g);
      out.write(r);
      wrote += 3;
      xaxis++;
      if (xaxis == biWidth) {
        xaxis = 0;
        yaxis++;
        out.write(new byte[adl]);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void write(byte v) {
    try {
      out.write(v);
      wrote ++;
      caxis++;
      if (caxis == 3) {
        caxis = 0;
        xaxis++;
        if (xaxis == biWidth) {
          xaxis = 0;
          yaxis++;
          out.write(new byte[adl]);
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void close() {
  	int wb = 0;
    try {
      if (wb > 0) {
        out.write(new byte[wb]);
      }
      out.close();
    }
    catch (IOException ex) {
      ex.printStackTrace();
    }
  }
}
