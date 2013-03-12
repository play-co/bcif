package bcif;
import java.io.*;
import java.util.*;
import java.net.*;

/**
 * <p> This class represents an uncompressed image. </p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class BmpImage {

  private String sbfType = "";
  private long bfType = 0;
  private long bfSize = 0;
  private long bfOffset = 0;
  private long biSize = 0;
  private long biWidth = 0;
  private long biHeight = 0;
  private long biPlanes = 0;
  private long biBitCount = 0;
  private long biCompression = 0;
  private long biSizeImage = 0;
  private long biXPelsPerMeter = 0;
  private long biYPelsPerMeter = 0;
  private long biClrUsed = 0;
  private long biClrImportant = 0;
  private int rgbBlue = 0;
  private int rgbGreen = 0;
  private int rgbRed = 0;
  private byte [] intest = {};
  private byteMatrix image = null;
  private byte [] lineFilter = null;
  private byte [] colorLineFilter = null;

  private int[][] simbolFreq = null;

  private int colorFilterNum = 6;
  private int filterNum = 12;
  private int filterZoneDim = 8;
  private int filterZoneDimBits = 3;
  private int colorFilterZoneDim = 8;
  private int colorFilterZoneDimBits = 3;

  private static int[] cost = null;
  private int[] errZones;

  private boolean layerXor = false;
  private boolean zigzag = true;
  private int filterStep = 1;

  private boolean filterA = false;
  private int enStart = 200000;
  private String filename = "From memory";

  private int lossy = 0;

  private final boolean newCM = false;

  private static final boolean entRep = false;
  private static boolean console = false;

  /**
   * Creates a BmpImage object from a specified filename. File must be a well-formed uncompressed bmp image with 24 bit color depth,
   * or an exception will be thrown.
   * @param filename The file to be read
   */

  public BmpImage(String filename) {
    try {
      File InputFile = new File(filename);
      if (! InputFile.exists()) { throw new FileNotFoundException("File not found: " + filename); }
      this.filename = filename;
      InputStream in = new BufferedInputStream(new FileInputStream(InputFile));
      readIntest(in);

      if (biBitCount < 24) {
        throw new RuntimeException("Image must have a 24 bit color depth instead of " + biBitCount);
      }
	  if (bfOffset > 54) {
		System.out.println("WARNING: BMP header longer than expected !");
		System.out.format("Attempting to proceed anyway, but discarding %d header bytes.", bfOffset - 54);
	  	int tind = 54;
	  	while (tind < bfOffset) {
	  		in.read();
	  		tind ++;
	  	}
	  }
	  
      long readbytes = InputFile.length() - bfOffset;
      int xaxis = 0;
      int yaxis = 0;
      int caxis = 0;
      image = new byteMatrix((int)biWidth, (int)biHeight, 3);
      int i = 0;
      int adl = (4 - (int)biWidth * 3 % 4) % 4;

      simbolFreq = new int[3][256];
      image.firstVal();
      for (i = 0; i < readbytes && yaxis < biHeight; i++) {
        byte val = (byte)in.read();
          image.setCurVal(val);
          image.nextVal();
          simbolFreq[caxis][val >= 0 ? val : val + 256] ++;
          caxis ++;
          if (caxis == 3) {
            caxis = 0;
            xaxis++;
            if (xaxis == biWidth) {
              for (int i2 = 0; i2 < adl; i2++) {
                i++;
                in.read();
              }
              xaxis = 0;
              yaxis++;
            }
            image.setPoint(xaxis, yaxis, caxis);
          }
      }
      in.close();
    } catch (Exception e) {
      System.err.println("Error while reading file - " + e.getMessage());
      throw new RuntimeException(e.getMessage());
    }
    //printBmpInfo();
  }

  public int getResX() {
  	return (int)biXPelsPerMeter;
  }
  
  public int getResY() {
  	return (int)biYPelsPerMeter;
  }
  
  public void setRes(int resX, int resY) {
  	biXPelsPerMeter = resX;
  	biYPelsPerMeter = resY;
    writeDWord(intest, 38, (int) biXPelsPerMeter);
    writeDWord(intest, 42, (int) biYPelsPerMeter);  	
  }
  
  /**
   * Creates a BmpImage from the byte represented image img, creating a coeherent bmp intestation for it.
   * The matrix dimensions must be width, height and 3 (the color number). Img is not copied; modifing it
   * means modifing the created object.
   * @param img The byte represented image.
   */

  public BmpImage(byteMatrix img) {
    createIntest(img.width(), img.height());
    image = img;
  }

  /**
   * Creates an image with the same intestation of a source image (dimensions, size, etc.).
   * @param source The source image
   * @param imgCopy If equal to 1, also the image of the new object will be copied from the source.
   */

  public BmpImage(BmpImage source, int imgCopy) {
    sbfType = "BM";
    bfType = source.bfType = 0;
    bfSize = source.bfSize;
    bfOffset = source.bfOffset;
    biSize = source.biSize;
    biWidth = source.biWidth;
    biHeight = source.biHeight;
    biPlanes = source.biPlanes;
    biBitCount = source.biBitCount;
    biCompression = source.biCompression;
    biSizeImage = source.biSizeImage;
    biXPelsPerMeter = source.biXPelsPerMeter;
    biYPelsPerMeter = source.biYPelsPerMeter;
    biClrUsed = source.biClrUsed;
    biClrImportant = source.biClrImportant;
    rgbBlue = source.rgbBlue;
    rgbGreen = source.rgbGreen;
    rgbRed = source.rgbRed;
    intest = new byte[source.intest.length];
    for (int i = 0; i < intest.length; i++) {
      intest[i] = source.intest[i];
    }
    intest = source.intest;
    image = new byteMatrix((int)biWidth, (int)biHeight, 3);
    int xaxis = 0;
    int yaxis = 0;
    int caxis = 0;
    int i = 0;
    while (yaxis < biHeight + 1 && imgCopy == 1) {
      image.setVal(xaxis, yaxis, caxis, source.getImage().getVal(xaxis, yaxis, caxis));
      caxis ++;
      i++;
      if (caxis == 3) {
        caxis = 0;
        xaxis++;
        if (xaxis == biWidth) {
          xaxis = 0;
          yaxis++;
        }
      }
    }
  }

  // Creates an image with no data, so fields must be initialized in other ways.

  private BmpImage() {

  }

  /**
   * Creates an image extracting only some bits from the source. Can be used to extract determinate
   * colors or to isolate most significant image info. Not used in compression.
   * @param ce The bits to extract. First index represents color, second represents the position. Every
   * value > 0 will appear in the new image. Ex. : extract color(new byte[][] {{1,1,1,1,1,1,0,0},
   * {0,0,0,0,0,0,0,0},{0,0,0,0,0,0,0,0}} will extract the 6 most significative bits of blue.
   * @return The extracted image
   */

  public BmpImage extractColor(byte[][] ce) {
    BmpImage res = new BmpImage(this,0);
    for (int i = 0; i < biWidth; i++) {
       for (int i2 = 0; i2 < biHeight; i2++) {
         res.image.setVal(i, i2, 0, 0);
         res.image.setVal(i, i2, 1, 0);
         res.image.setVal(i, i2, 2, 0);
         for (int i3 = 0; i3 < 3; i3 ++) {
           for (int i4 = 0; i4 < 8; i4 ++) {
             if (ce[i3][i4] > 0) {
               res.image.sumVal(i, i2, i3, bitMatrix.exBit(image.getVal(i, i2, i3), i4) << i4);
             }
           }
         }
       }
     }
    return res;
  }

  /**
   * Creates a standard bmp intestation of the object given height and width
   * @param imageWidth The width of the image
   * @param imageHeight The height of the image
   */

  public void createIntest(int imageWidth, int imageHeight) {
    sbfType = "BM";
    biWidth = imageWidth;
    biHeight = imageHeight;
    int adl = (4 - imageWidth * 3 % 4) % 4;
    int width = imageWidth * 3 + adl;
    bfSize = imageHeight * width + 54;    
    bfOffset = 54;
    biSize = 40;
    biPlanes = 1;
    biBitCount = 24;
    if (biXPelsPerMeter == 0) {
    	biXPelsPerMeter = bfSize - 54;
    	biYPelsPerMeter = 0;
    }
    biSizeImage = bfSize - 54;
    biClrUsed = 0;
    biClrImportant = 0;
    rgbBlue = 0;
    rgbGreen = 0;
    rgbRed = 0;
    intest = new byte[54];
    intest[0] = (byte)'B';
    intest[1] = (byte)'M';
    writeDWord(intest, 2, (int)bfSize);
    intest[10] = 54;
    intest[14] = 40;
    writeDWord(intest, 18, (int)biWidth);
    writeDWord(intest, 22, (int)biHeight);
    intest[26] = 1;
    intest[28] = 24;
    writeDWord(intest, 34, (int)biSizeImage);
    writeDWord(intest, 38, (int)biXPelsPerMeter);
    writeDWord(intest, 42, (int)biYPelsPerMeter);
    image = new byteMatrix((int)biWidth, (int)biHeight, 3);
  }

    /**
     * Swaps the values of two colors of the image.
     * @param c1 The first of the two colors
     * @param c2 The second of the two colors
     */

  public void swapColors(int c1, int c2) {
    for (int i = 0; i < biWidth; i ++) {
      for (int i2 = 0; i2 < biHeight; i2 ++) {
        int v = image.getVal(i, i2, c1);
        image.setVal(i, i2, c1, image.getVal(i, i2, c2));
        image.setVal(i, i2, c2, v);
      }
    }
  }

  /**
   * Rounds the less significative bits of the image. Implemented to see the effects of a particular
   * lossy compression but actually not used.
   * @param lim The bits to round, will be 0 after this operation.
   */

  public void roundBits(int lim) {
    int slim = 1 << (lim - 1);
    int mod = 1 << lim;
    int exL = mod - 1;
    int exH = 255 - exL;
    int thisval = 0;
    int newval = 0;
    for (int i = 0; i < biWidth; i++) {
      for (int i2 = 0; i2 < biHeight; i2 ++) {
        for (int i3 = 0; i3 < 3; i3 ++) {
          newval = image.getIntVal(i, i2, i3);
          if ((newval & exL) > slim) { newval += slim; }
          if (newval > 255) { newval = 255; }
          newval = newval & exH;
          image.setVal(i, i2, i3, newval);
        }
      }
    }
  }

  /**
   * Experimental lossy filter, not used in BCIF.
   */
   
  public void lossyFilterTriple(int lim) {
    System.out.println("Determinating filters and applying lossy filter...");
    long initTime = System.currentTimeMillis();
    lossy = lim;
    if (lineFilter == null) { filterDeterminate(); }
    int slim = 1 << (lim - 1);
    int mod = (1 << lim) + 1;
    int exL = mod - 1;
    int exH = 255 - exL;
    int thisval = 0;
    int err = 0;
    int val, fv, filVal, r;
    int c255 = 0;
    for (int i2 = 0; i2 < biHeight; i2 ++) {
      for (int i = 0; i < biWidth; i ++) {
        for (int i3 = 0; i3 < 3; i3 ++) {
          image.setPoint(i, i2, i3);
          val = image.getIntCurVal();
          fv = filter(i, i2, i3);
          if (fv < 0) { fv += 256; }
          filVal = val - fv;
          r = filVal % mod;
          if (filVal > 0) {
            if (r <= slim) {
              val -= r;
              err += r;
            }
            else {
              val += (mod - r);
              err += (mod - r);
            }
          } else {
            if (-r <= slim) {
              val -= r;
              err -= r;
            }
            else {
              val -= (mod + r);
              err += (mod + r);
            }
          }
          if (val > 255) { val = 255; c255 += 1;}
          image.setCurVal((byte)val);
        }
      }
    }
    System.out.println("Medium lossy filter introduced error is " + (float)err / (biWidth * biHeight * 3) + " per byte");
    System.out.println("Filter determination and lossy filter applying time is " +
                       (System.currentTimeMillis() - initTime) + " ms. ");
  }

    /**
     * Experimental lossy filter, not used in BCIF; this function determines the filters 
     * for the image and then
     * applies a lossy transormation that will improve compression rounding the value
     * of each point to the closest number that makes the differende between itself
     * and the filter estimated value a multiple of 2^n where n is the selected lossy
     * level. The tranformation gives very low visual effects for a lossy level of
     * one.
     * @param lim The lossy level.
     */

  public void lossyFilterNull(int lim) {
    System.out.println("Determinating filters and applying lossy filter...");
    long initTime = System.currentTimeMillis();
    lossy = lim;
    if (lineFilter == null) { filterDeterminate(); }
    int slim = 1 << (lim - 1);
    int mod = 1 << lim;
    int exL = mod - 1;
    int exH = 255 - exL;
    int thisval = 0;
    int newval = 0;
    int err = 0;
    boolean precSub = false;
    int precMod = 0;
    int propErr[] = new int[3];
    int propC = 0;
    int[] svs = new int[3];
    int[][] oPropErr = new int[(int)biWidth][3];
    int precVal = 0;
    for (int i2 = 0; i2 < biHeight; i2 ++) {
      for (int i = 0; i < biWidth; i ++) {
        for (int i3 = 0; i3 < 3; i3 ++) {
          image.setPoint(i, i2, i3);
          newval = image.getCurVal();
          int fv = filter(i, i2, i3);
          if (newval < 0) { newval += 256; }
          if (fv < 0) { fv += 256; }
          int filVal = newval - fv;
          while (filVal < 0) { filVal += 256; }
          while (filVal > 255) { filVal -= 256; }
          int subVal = filVal & exL;
          if (subVal > slim || (subVal == slim && filVal > 128)) { subVal = subVal - mod; }
          newval = newval - subVal;
          precMod = subVal;
          if (subVal > 0) { svs[i3] ++; }
          if (subVal < 0) { svs[i3] --; }
          propErr[i3] += subVal;
          oPropErr[i][i3] += subVal;
          propC ++;
          err += subVal < 0 ? - subVal : subVal;
          if (newval < 0) { newval = 0; }
          if (newval > 255) { newval = 255; }
          if (lossy == 1 || true) {
            image.setCurVal((byte)newval);
          } else {
            propErr[i3] = 0;
            oPropErr[i][i3] = 0;
            propC = 0;
            svs[i3] = 0;
          }
          precVal = newval;
        }
      }
    }
    System.out.println("Medium lossy filter introduced error is " + (float)err / (biWidth * biHeight * 3) + " per byte");
    System.out.println("Filter determination and lossy filter applying time is " +
                       (System.currentTimeMillis() - initTime) + " ms.");
  }


  private void lossyFilterNear(int q) {
    for (int i = 0; i < biWidth; i ++) {
      for (int i2 = 0; i2 < biHeight; i2 ++) {
        for (int i3 = 0; i3 < 3; i3++) {
          image.setPoint(i, i2, i3);
          int val = image.getCurVal();
          int nv = filter(i, i2, i3);
          if (nv < 0) { nv += 256; }
          if (val < 0) { nv += 256; }
          if (val < nv) {
            if (nv - val > q) {
              val += q;
            } else {
              val = nv;
            }
          }
          if (val > nv) {
            if (val - nv > q) {
              val -= q;
            } else {
              val = nv;
            }
          }
          image.setVal(i, i2, i3, val);
        }
      }
    }
  }

  /**
   * Reads a bmp intestation from the given input stream. Will read 54 bytes. If bmp intestation specifies
   * a compressed file or does not start with 'BM', method throws an exception.
   * @param in The input stream where to read (usually a file)
   */

  public void readIntest(InputStream in) {
    try {
      intest = new byte[54];
      in.read(intest, 0, 54);
      for (int i = 0; i < 2; i++) {bfType = bfType * 256 + intest[i];}
      sbfType = (char)intest[0] + "" + (char)intest[1];
      if (! sbfType.equals("BM")) { throw new RuntimeException("Input file is not a valid bitmap file !"); }
      bfSize = readDWord(intest,2);
      bfOffset = readDWord(intest,10);
      biSize = readDWord(intest,14);
      biWidth = readLong(intest,18);
      biHeight = readLong(intest,22);
      biPlanes = readWord(intest,26) >> 8;
      biBitCount = readWord(intest,28) >> 8;
      biCompression = readDWord(intest,30);
      if (biCompression > 0) { throw new RuntimeException("Source image must be uncompressed !"); }
      biSizeImage = readDWord(intest, 34);
      biXPelsPerMeter = readLong(intest,38);
      biYPelsPerMeter = readLong(intest,42);
      biClrUsed = readWord(intest,44);
      biClrImportant = readWord(intest,46);
      rgbBlue = intest[50] >= 0 ? intest[50] : 256 + intest[50];
      rgbGreen = intest[51] >= 0 ? intest[51] : 256 + intest[51];
      rgbRed = intest[52] >= 0 ? intest[52] : 256 + intest[52];
    } catch (RuntimeException e) {
      throw e;
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }


  private void square(int ix, int iy, int fx, int fy, int ft) {
    if (ft == 1) { square(ix, iy, fx, fy, 100, 100, 100); } else
      if (ft == 2) { square(ix, iy, fx, fy, 200, 200, 200); } else
         if (ft == 3) { square(ix, iy, fx, fy, 250, 0, 0); } else
            if (ft == 4) { square(ix, iy, fx, fy, 0, 250, 250); } else
               if (ft == 5) { square(ix, iy, fx, fy, 0, 0, 250); } else
                  if (ft == 6) { square(ix, iy, fx, fy, 0, 250, 0); }
  }

  private void square(int ix, int iy, int fx, int fy, int c0, int c1, int c2) {
    int squareSide = 1;
    for (int i = ix; i <= fx; i ++) {
      for (int i2 = 0; i2 < squareSide; i2 ++) {
        if (i < biWidth && i2 + iy < biHeight) {
          image.setVal(i, i2 + iy, 0, c0);
          image.setVal(i, i2 + iy, 0, c1);
          image.setVal(i, i2 + iy, 0, c2);
        }
        if (i < biWidth && fy - i2 < biHeight) {
          image.setVal(i, fy - i2, 0, c0);
          image.setVal(i, fy - i2, 0, c1);
          image.setVal(i, fy - i2, 0, c2);
        }
      }
    }
    for (int i2 = iy; i2 <= fy; i2 ++) {
      for (int i = 0; i < squareSide; i ++) {
        if (i + ix < biWidth && i2 < biHeight) {
          image.setVal(i + ix, i2, 0, c0);
          image.setVal(i + ix, i2, 0, c1);
          image.setVal(i + ix, i2, 0, c2);
        }
        if (fx - i < biWidth && i2 < biHeight) {
          image.setVal(fx - i, i2, 0, c0);
          image.setVal(fx - i, i2, 0, c1);
          image.setVal(fx - i, i2, 0, c2);
        }
      }
    }
  }

  /**
   * Gets the image represented by the object as a matrix where first index represents the x coordinate,
   * the second the y and the third the color (0 is blue, 1 is green, 2 is red). Does not make a copy;
   * modifing the results means modifing the original image.
   * @return The image contained in the object
   */

  public byteMatrix getImage () {
    return image;
  }

  /**
   * Return the image width in pixels
   * @return Image width
   */

  public long getWidth() {
    return biWidth;
  }

  public byte[] getZoneFilters() {
    return lineFilter;
  }

  public byte[] getColorZoneFilters() {
    return colorLineFilter;
  }

  public void setZoneFilters(byte[] f) {
    lineFilter = f;
  }

  public void setColorZoneFilters(byte[] cf) {
    colorLineFilter = cf;
  }

  /**
   * Return the image height in pixels
   * @return Image height
   */

  public long getHeight() {
    return biHeight;
  }

  /**
   * Prints on standard output some bmp intestation info about the image, such as height, width, filesize
   * and intestation size
   */

  public void printBmpInfo() {
    System.out.println("Type: " + sbfType);
    System.out.println("Size: " + bfSize + " bytes");
    System.out.println("Offset: " + bfOffset + " bytes");
    System.out.println("Bitmap info size: " + biSize + " bytes");
    System.out.println("Width: " + biWidth + " pixels");
    System.out.println("Height: " + biHeight + " pixels");
    System.out.println("Planes (1): " + biPlanes);
    System.out.println("Bits per pixel: " + biBitCount + " bits");
    System.out.println("Compression: " + biCompression);
    System.out.println("Image size: " + biSizeImage);
    System.out.println("Horizontal resolution: " + biXPelsPerMeter);
    System.out.println("Vertical resolution: " + biYPelsPerMeter);
    System.out.println("Color indexes: " + biClrUsed);
    //System.out.println("Important color indexes (0 is all): " + biClrImportant);
    //System.out.println("Blue: " + rgbBlue);
    //System.out.println("Green: " + rgbGreen);
    //System.out.println("Red: " + rgbRed);
    System.out.println();
  }

  /**
   * Creates a BmpFile describing the BmpImage object. If file exists, it will be overwritten
   * @param dest The filename to write
   */

  public void writeImage(String dest) {
    try {
      OutputStream out = new BufferedOutputStream(new FileOutputStream(dest), 8096);
      byte[] buffer = new byte[100];
      out.write(intest,0,54);
      int xaxis = 0;
      int yaxis = 0;
      int caxis = 0;
      int i = 0;
      int adl = (4 - (int)biWidth * 3 % 4) % 4;    // Padding bytes at the end of each line
      int wroteBytes = 0;
      while (yaxis < biHeight) {
        try {
          out.write(image.getVal(xaxis, yaxis, caxis));
          wroteBytes ++;
        } catch (Exception e) {
          e.printStackTrace();
        }
        caxis ++;
        i++;
        if (caxis == 3) {
          caxis = 0;
          xaxis++;
          if (xaxis == biWidth) {
            xaxis = 0;
            yaxis++;
            for (int i2 = 0; i2 < adl; i2++) {
              i++;
              out.write(new byte[1]);
              wroteBytes ++;
            }
          }
        }
      }
      out.close();
    } catch (Exception e) {e.printStackTrace();}
  }

  private void writeColor(int col, String dest) {
     try {
       OutputStream out = new BufferedOutputStream(new FileOutputStream(dest), 8096);
       for (int i = 0; i < biWidth; i++) {
         for (int i2 = 0; i2 < biHeight; i2 ++) {
           out.write(image.getVal(i, i2, col));
         }
       }
       out.close();
     } catch (Exception e) {e.printStackTrace();}
   }

   private void writeColorzz(int col, String dest) {
      try {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(dest), 8096);
        int i = 0;
        int i2 = 0;
        int step = 0;
        int ii = 0;
        while (i2 < biHeight) {
          out.write(image.getVal(i, i2, col));
          if (step == 0) { i2 ++; step = 1; }
          else if (step == 1 || step == 3) { i ++; step = (step + 1) & 3; }
          else { i2 --; step = 3; }
          if (i2 >= biHeight && ii < biHeight) { i2 = (int)biHeight - 1; i ++; step = 0; }
          if (i >= biWidth && ii < biHeight - 1) { ii += 2; i2 = ii; i = 0; step = 0; }
        }
       out.close();
      } catch (Exception e) {e.printStackTrace();}
    }

  /**
   * Reads a 4-byte word from specified byte buffer. Most significative bytes are right. Used while
   * reading bmp intestations.
   * @param buffer The byte buffer where to read from
   * @param init The point of the array from where to read
   * @return The obtained value
   */

  public static long readDWord (byte[] buffer, int init) {
    long res = 0;
    for (int i = init + 3; i > init - 1; i--) {
      if (buffer[i] >= 0) {
        res = (res * 256) + buffer[i];
      } else {
        res = (res * 256) + buffer[i] + 256;
      }
    }
    return res;
  }

  /**
   * Writes a 4-byte word from specified byte buffer. Most significative bytes are right. Used while
   * writing and creating bmp intestations.
   * @param buffer The byte buffer where to write
   * @param init The point of the array from where to write the value
   * @param dw The value to write
   */

  public static void writeDWord (byte[] buffer, int init, int dw) {
    for (int i = init; i < init + 4; i++) {
      buffer[i] = (byte)(dw % 256);
      dw = dw >>> 8;
    }
  }

  /**
   * Reads a 4-byte word from specified byte buffer. Most significative bytes are right. Used while
   * reading bmp intestations.
   * @param buffer The byte buffer where to read from
   * @param init The point of the array from where to read
   * @return The obtained value
   */

  public static long readLong (byte[] buffer, int init) {
    long res = 0;
    for (int i = init + 3; i > init - 1; i--) {
      if (buffer[i] >= 0) {
        res = (res * 256) + buffer[i];
      } else {
        res = (res * 256) + buffer[i] + 256;
      }
    }
    return res;
  }

  /**
   * Reads a 2-byte word from specified byte buffer. Most significative bytes are right. Used while
   * reading bmp intestations.
   * @param buffer The byte buffer where to read from
   * @param init The point of the array from where to read
   * @return The obtained value
   */

  public static long readWord (byte[] buffer, int init) {
    long res = 0;
    for (int i = init; i < init + 2; i++) {
      if (buffer[2 * (init) + 1 - i] >= 0) {
        res = (res * 256) + buffer[i];
      } else {
        res = (res * 256) + buffer[i] + 256;
      }
    }
    return res;
  }

  /**
   * Reads a string from the begenning of the byte array.
   * @param buffer The array where to read from
   * @param charNum The number of bytes to read
   * @return The obtained string
   */

  public static String readString (byte[] buffer, int charNum) {
    String res = "";
    for (int i = 0; i < charNum; i++) {res += (char)buffer[i];}
    return res;
  }

  /**
   * Reads from the specified input stream the spatial and color filters 
   * used for the different zones of the image. Filters must be encoded with the 
   * writeLineFilters method, and both of these methods are NOT compatible
   * with the BCIF filter encoding procedures.
   * @param in The input stream where to read from, usually a file
   */

  public void readLineFilters(InputStream in) {
    int zoneNum = (int)(((biWidth - 1) / filterZoneDim + 1) * ((biHeight - 1) / filterZoneDim + 1));
    int cZoneNum = (int)(((biWidth - 1) / colorFilterZoneDim + 1) * ((biHeight - 1) / colorFilterZoneDim + 1));
    try {
      byte[] b = new byte[1];
       lineFilter = new byte[zoneNum];
       int filterDim = 0;
       if (filterNum > 1) {
         filterDim = bitMatrix.log2(filterNum - 1) + 1;
       }
       bitMatrix lbm = new bitMatrix(in, ((zoneNum * filterDim - 1) >> 3) + 1);
       for (int i = 0; i < lineFilter.length; i ++) {
         lineFilter[i] = (byte)(lbm.fread(filterDim));
       }
       colorLineFilter = new byte[cZoneNum];
       int colorFilterDim = 0;
       if (colorFilterNum > 1) {
         colorFilterDim = bitMatrix.log2(colorFilterNum - 1) + 1;
       }
       bitMatrix lbmc = new bitMatrix(in, ((cZoneNum * colorFilterDim - 1) >> 3) + 1);
       for (int i = 0; i < colorLineFilter.length; i ++) {
         colorLineFilter[i] = (byte)(lbmc.fread(colorFilterDim));
       }
    } catch (Exception e) {
        e.printStackTrace();
    }
  }

  /**
   * Write the type of spatial and color filters for the different zones of the image, 
   * in a format NOT compatible with BCIF but easy to read and write. If 
   * filterDeterminate has not
   * been called the method consideres the zero filter for all the image.
   * @param out The output stream where to write.
   */

  public void writeLineFilters(OutputStream out) {
    try {
      if (lineFilter == null) {
        lineFilter = new byte[(int)(((biWidth - 1) / filterZoneDim + 1) * ((biHeight - 1) / filterZoneDim + 1))];
      } 

      int filterDim = 0;
      if (filterNum > 1) {
        filterDim = bitMatrix.log2(filterNum - 1) + 1;
      }
      bitMatrix lbm = new bitMatrix(lineFilter.length * filterDim, 0);
      for (int i = 0; i < lineFilter.length; i++) {
        lbm.fwrite(lineFilter[i], filterDim);
      }
      lbm.writeStream(out);
      if (colorLineFilter == null) {
        colorLineFilter = new byte[(int)(((biWidth - 1) / colorFilterZoneDim + 1) *
                                         ((biHeight - 1) / colorFilterZoneDim + 1))];
      }
      int colorFilterDim = 0;
      if (colorFilterNum > 1) {
        colorFilterDim = bitMatrix.log2(colorFilterNum - 1) + 1;
      }
      bitMatrix lbmc = new bitMatrix(colorLineFilter.length * colorFilterDim, 0);
      for (int i = 0; i < colorLineFilter.length; i++) {
        lbmc.fwrite(colorLineFilter[i], colorFilterDim);
      }
      lbmc.writeStream(out);
    } catch (Exception e) {
        e.printStackTrace();
    }
  }

  public int bestFil(int i, int i2, int i3) {
    int cur = image.getIntVal(i, i2, i3);
    int left = Math.abs(image.getIntVal(i - 1, i2, i3) - cur);
    int low = Math.abs(image.getIntVal(i, i2 - 1, i3) - cur);
    int ll = Math.abs(image.getIntVal(i - 1, i2 - 1, i3) - cur);
    int lr = Math.abs(image.getIntVal(i + 1, i2 - 1, i3) - cur);
    if (left <= low && left <= ll && left <= lr) {
      return 2;
    } else if (low <= ll && low <= lr) {
      return 4;
    } else if (ll <= lr) {
      return 5;
    } else {
      return 7;
    }
  }

  /**
   * Applies the filters to the image, from right to left and from high to low.
   */

  public void applyFilter() {
    println("Applying filter ...");
    image.lastVal();
    simbolFreq = new int[3][256];
    int c255 = 0;
    for (int i2 = (int)biHeight - 1; i2 > -1; i2--) {
      for (int i = (int)biWidth - 1; i > -1; i --) {
        for (int i3 = 2; i3 > -1; i3--) {
          image.sumCurVal(- filter(i, i2, i3));
          int val = image.getCurVal();
          simbolFreq[i3][val >= 0 ? val : val + 256] ++;
          image.precVal();
        }
      }
    }
    if (entRep) { entropyReport(); }
  }

  private void applyAdColFilter() {
	throw new RuntimeException("Advanced color filters not supported.");
  }

  public void locoColTransform() {
    println("Applying color transfrom ...");
    int forlim = (int)biWidth - 1;
    int forlim2 = (int)biHeight - 1;
    image.lastVal();
    for (int i2 = forlim2; i2 > -1; i2--) {
      for (int i = forlim; i > -1; i --) {
      	image.setPoint(i, i2, 0);
        image.sumCurVal(- image.getCurVal(1));
        image.setPoint(i, i2, 2);
        image.sumCurVal(- image.getCurVal(-1));
      }
    }
    if (entRep) {
      entropyReport();
    }
  }
  
    /**
     * Applies the color filters on the image.
     */

  public void applyColFilter() {
    println("Applying color filter ...");
    simbolFreq = new int[3][256];
    int forlim = (int)biWidth - 1;
    int forlim2 = (int)biHeight - 1;
    image.lastVal();
    for (int i2 = forlim2; i2 > -1; i2--) {
      for (int i = forlim; i > -1; i --) {
        for (int i3 = 2; i3 > -1; i3--) {
          image.sumCurVal( - colorFilter(i, i2, i3));
          int val = image.getCurVal();
          simbolFreq[i3][val >= 0 ? val : val + 256] ++;
          image.precVal();
        }
      }
    }
    if (entRep) {
      entropyReport();
    }
  }

  public void detAndApplyAdvancedColFilter() {
	throw new RuntimeException("Advanced color filters not implemented.");
  }

  private void invAdvancedColFilter() {
	throw new RuntimeException("Advanced color filters not implemented.");
  }

  public void applyAdvancedColFilter() {
	throw new RuntimeException("Advanced color filters not implemented.");
  }

  /**
   * Returns the absolute value of the argument
   * @param v0 The argument of the function
   * @return the absolute value
   */

  public int abs(int v0) {
    return (v0 >= 0 ? v0 : -v0);
  }

  public void removeColFilter() {
    println("Removing color filters ...");
    image.firstVal();
    int curcFil = 0;
    int curcInd = 0;
    for (int i2 = 0; i2 < biHeight; i2 ++) {
      curcFil = colorFilterOfZone(0, i2);
      curcInd = 0;
      for (int i = 0; i < biWidth; i ++) {
        if (curcInd == colorFilterZoneDim) {
          curcInd = 0;
          curcFil = colorFilterOfZone(i, i2);
        }
        curcInd ++;
          for (int i3 = 0; i3 < 3; i3++) {
            image.sumCurVal(colorFilter(i, i2, i3, curcFil));
            image.nextVal();
          }
      }
    }
  }

  public void removeBothFilters() {
    println("Removing standard and color filters ...");
    image.firstVal();
    int curcFil = 0;
    int curcInd = 0;
    int curFil = 0;
    int curInd = 0;
    for (int i2 = 0; i2 < biHeight; i2 ++) {
      curcFil = colorFilterOfZone(0, i2);
      curcInd = 0;
      curFil = filterOfZone(0, i2);
      curInd = 0;
      for (int i = 0; i < biWidth; i ++) {
        if (curcInd == colorFilterZoneDim) {
          curcInd = 0;
          curcFil = colorFilterOfZone(i, i2);
        }
        if (curInd == filterZoneDim) {
          curInd = 0;
          curFil = filterOfZone(i, i2);
        }
        curcInd ++;
        curInd ++;
        for (int i3 = 0; i3 < 3; i3++) {
          image.sumCurVal(colorFilter(i, i2, i3, curcFil));
          image.nextVal();
        }
        image.precVal(3);
        for (int i3 = 0; i3 < 3; i3++) {
          image.sumCurVal(filter(i, i2, i3, curFil));
          image.nextVal();
        }
      }
    }
  }

  /**
   * Removes the filters from the image, from left to right and from low to high. To obtain the original
   * image the zone-settings must be known, for example read by the readLineFilter method.
   */

  public void removeFilter() {
    println("Removing filters ...");
    image.firstVal();
    int curFil = 0;
    int curInd = 0;
    for (int i2 = 0; i2 < biHeight; i2 ++) {
      curFil = filterOfZone(0, i2);
      curInd = 0;
      for (int i = 0; i < biWidth; i ++) {
        if (curInd == filterZoneDim) {
          curInd = 0;
          curFil = filterOfZone(i, i2);
        }
        curInd ++;
        for (int i3 = 0; i3 < 3; i3 ++) {
          image.sumCurVal(filter(i, i2, i3, curFil));
          image.nextVal();
        }
      }
    }
  }

  private int colorFilterOfZone(int i, int i2) {
    int xZones = (int)(((biWidth - 1) >> colorFilterZoneDimBits) + 1);
    int zoneRef = (int)((i >> colorFilterZoneDimBits) + xZones * (i2 >> colorFilterZoneDimBits));
    return colorLineFilter[zoneRef];
  }

  private int filterOfZone(int i, int i2) {
    int xZones = (int)(((biWidth - 1) >> filterZoneDimBits) + 1);
    int zoneRef = (int)((i >> filterZoneDimBits) + xZones * (i2 >> filterZoneDimBits));
    return lineFilter[zoneRef];
  }

  /**
   * Return the unsigned value of a value of the image as an integer
   * @param x  The orizzontal coordinate of the interessed valur
   * @param y  The vertical coordinate of the interessed value
   * @param color The color coordinate of the interessed value
   * @return The unsigned value of the requested point, as an integer
   */

  public int getIntVal(int x, int y, int color) {
    return image.getIntVal(x, y, color);
  }

    /**
     * Determinates and applies filters and color filters and makes the
     * remapping. This method just calls sequencially the other methods.
     */

  public void detAndApplyAllFilters() {
    filterDeterminate();
    applyFilter();
    colorFilterDeterminate();
    applyColFilter();
    //remap();
  }

    /**
     * Writes on standard output information about the entropy of the image.
     */

  public void entropyReport() {
    double bentropy = entropy(simbolFreq[0]);
    double gentropy = entropy(simbolFreq[1]);
    double rentropy = entropy(simbolFreq[2]);
    int threeentropy = (int)(biWidth * biHeight * (bentropy + gentropy + rentropy));
    System.out.println("Single colour entropy is " + (float)((int)(bentropy * 100)) / 100 + ", " +
                       (float)((int)(gentropy * 100)) / 100 + " and " +
                       (float)((int)(rentropy * 100)) / 100 + ", minimal 1 order model compressed size is " +
                       (threeentropy >> 3) + " bytes (" +
                       (threeentropy >> 13) + " KB)");
  }

  private void enFillCost(int[] f) {
    cost = new int[256];
    int tot = 0;
    for (int i = 0; i < 256; i++) {
      if (f[i] == 0) { f[i] = 1; }
      tot += f[i];
    }
    for (int i = 0; i < 256; i++) {
      float freq = (float)f[i] / tot;
      cost[i] = -(int)Math.round(Math.log(freq) / Math.log(2));
    }
  }

  private int[] calcFilErrs(int val, int left, int low, int ll, int dr, int x, int y) {
    int[] res = new int[12];
    if (val < 0) { val += 256; }
    if (left < 0) { left += 256; }
    if (low < 0) { low += 256; }
    if (ll < 0) { ll += 256; }
    if (dr < 0) { dr += 256; }
    res[1] = val;
    res[2] = val - left;
    res[3] = val - ((left + low) >> 1);
    res[4] = val - low;
    res[5] = val - ll;
    int lp = (left + low - ll);
    if (lp < 0) { lp = 0; } else if (lp > 255) { lp = 255; }
    res[6] = val - lp;
    res[7] = val - dr;
    res[8] = val - (left + ((low - ll) >> 1));
    res[9] = val - (low + ((left - ll) >> 1));
    res[10] = val - ((low + left + ll + dr + 1) >> 2);
    res[11] = val - ((dr + low) >> 1);
    if (ll >= left && ll >= low) {
      if (left > low) { res[0] = res[4]; } else { res[0] = res[2]; }
    } else if (ll <= left && ll <= low) {
      if (left > low) { res[0] = res[2]; } else { res[0] = res[4]; }
    } else {
      res[0] = res[6];
    }
    if (x == 0) {
      if (y == 0) {
        for (int i = 0; i < 12; i++) { res[i] = val; }
      } else {
        int bres = val - low;
        res[0] = bres; res[3] = bres; res[5] = bres; res[6] = bres; res[7] = bres;
        res[2] = bres; res[8] = bres; res[9] = bres; res[10] = bres; res[11] = bres;
      }
    }
    if (y == 0 && x > 0) {
      int bres = val - left;
      res[2] = bres;
      res[0] = bres; res[3] = bres; res[5] = bres; res[6] = bres;
      res[8] = bres; res[9] = bres; res[10] = bres; res[11] = bres; res[4] = bres;
    }
    if (x >= biWidth - 1 && y > 0) {
      int bres = val - left;
      res[7] = bres; res[10] = bres; res[11] = bres;
    }
    return res;
  }

  /**
   * Estimates the best filters for every zone of the image or fraction. Obtained info is stored
   * in the object and can be wrote by a writeLineFilter call
   */

  public void filterDeterminate() {
    println("Determinating best filters ...");
    if (cost == null) { cost = costEvaluator.getCosts(); }
    int[] e = new int[filterNum];
    int val = 0;
    int newVal = 0;
    int best = 0;
    int bestInd = 0;
    int zoneNum = (int)(((biWidth - 1) / filterZoneDim + 1) * ((biHeight - 1) / filterZoneDim + 1));
    int zoneStep = (int)(((biWidth - 1) >> filterZoneDimBits) + 1);
    lineFilter = new byte[zoneNum];
    int minx = 0;
    int miny = 0;
    int maxx = filterZoneDim - 1;
    int maxy = filterZoneDim - 1;
    int maxFilterUsed = 0;

    if (entRep) { entropyReport(); }

    BmpImage imageOut = null;
    if (filterA) {
      imageOut = new BmpImage(this, 1);
    }
    byte left = 0;
    byte low = 0;
    byte lowLeft = 0;
    byte upLeft = 0;
    byte rightDown = 0;
    costEvaluator[] ce = new costEvaluator[3];
    for (int i = 0; i < 3; i ++) {
      ce[i] = new costEvaluator(filterNum);
    }
    for (int i5 = 0; i5 < zoneNum; i5 ++) {
      e = new int[filterNum];
      for (int i3 = miny; i3 <= maxy; i3 ++) {
        int precErr = 0;
        int newErr = 0;
        int precVal = 0;
        for (int i2 = minx; i2 <= maxx; i2 += filterStep) {
          int [] pErrs = new int[3];
          for (int i4 = 0; i4 < 3; i4++) {
            image.setPoint(i2, i3, i4);
            val = image.getCurVal();
            if (i2 > 0) { left = image.getLeftVal(); } else { left = 0; }
            if (i3 > 0) { low = image.getLowVal(); } else { low = 0; }
            if (i2 > 0 && i3 > 0) { lowLeft = image.getLowLeftVal(); } else { lowLeft = 0; }
            if (i3 > 0 && i2 < biWidth - 1) { rightDown = image.getRightDownVal(); } else { upLeft = 0; }
            int[] filterErrs = calcFilErrs(val, left, low, lowLeft, rightDown, i2, i3);
            for (int i = 0; i < filterNum; i++) {
              newErr = (byte)filterErrs[i];
              if (newCM) {
                if (newErr < 0) { newErr += 256; }
                if (newErr > 255) { newErr -= 256; }
                ce[i4].putVal(i, newErr);
              } else {
                int sumVal = cost[newErr > 0 ? newErr : -newErr];
                e[i] += sumVal;
              }
            }
          }
        }
      }
      bestInd = 0;
      if (newCM) {
        best = ce[0].getFilCost(0) + ce[1].getFilCost(0) + ce[2].getFilCost(0);
      } else {
        best = e[0];
      }
      for (int i = 1; i < filterNum; i ++) {
        int filCost = 0;
        if (newCM) {
          filCost = ce[0].getFilCost(i) + ce[1].getFilCost(i) + ce[2].getFilCost(i);
        } else {
          filCost = e[i];
        }
        if (filCost < best) { best = filCost; bestInd = i; }
      }
      lineFilter[i5] = (byte)bestInd;
      if (newCM) { ce[0].signalSel(bestInd); ce[1].signalSel(bestInd); ce[2].signalSel(bestInd); }
      maxFilterUsed = maxFilterUsed >= bestInd + 1 ? maxFilterUsed: bestInd + 1;
      if (filterA) { imageOut.square(minx, miny, maxx, maxy, bestInd); }
      minx += filterZoneDim;
      if (minx > biWidth - 1) {
        minx = 0;
        miny += filterZoneDim;
      }
      maxx = (int)Math.min(minx + filterZoneDim - 1, biWidth - 1);
      maxy = (int)Math.min(miny + filterZoneDim - 1, biHeight - 1);
    }
    //if (maxFilterUsed < filterNum) { setFilterNum(maxFilterUsed); }
    if (filterA) { imageOut.writeImage(filename + ".fa" + filterNum + ".bmp"); }
  }

  /**
   * Estimates the best color filters for every zone of the image or fraction. Obtained info is stored
   * in the object and can be wrote by a writeColorLineFilter call
   */

  public void colorFilterDeterminate() {
    println("Determinating best color filters ...");
    int[] e = new int[colorFilterNum];
    int val = 0;
    int newVal = 0;
    int best = 0;
    int bestInd = 0;
    int zoneNum = (int)(((biWidth - 1) / colorFilterZoneDim + 1) * ((biHeight - 1) / colorFilterZoneDim + 1));
    colorLineFilter = new byte[zoneNum];
    int minx = 0;
    int miny = 0;
    int maxx = colorFilterZoneDim - 1;
    int maxy = colorFilterZoneDim - 1;
    int maxColorFilterUsed = 0;
    int [] cost2 = costEvaluator.getCosts();
    costEvaluator[] ce = new costEvaluator[3];
    for (int i = 0; i < 3; i ++) {
      ce[i] = new costEvaluator(colorFilterNum);
    }
    int [][] filterErrs = new int[3][10];
    for (int i5 = 0; i5 < zoneNum; i5 ++) {
      e = new int[colorFilterNum];
      for (int i3 = miny; i3 <= maxy; i3 ++) {
        int precErr = 0;
        int newErr = 0;
        for (int i2 = minx; i2 <= maxx; i2 += filterStep) {
          image.setPoint(i2, i3, 0);
          calcFilColErrs(image.getCurVal(), image.getCurVal(1), image.getCurVal(2), filterErrs);
          for (int i4 = 0; i4 < 3; i4++) {
            image.setPoint(i2, i3, i4);
            val = image.getCurVal();
            for (int i = 0; i < colorFilterNum; i++) {
              newErr = filterErrs[i4][i];
              if (newErr < 0) { newErr += 256; }
              ce[i4].putVal(i, newErr);
            }
            image.nextVal();
          }
        }
      }
      bestInd = 0;
      best = ce[0].getFilCost(0) + ce[1].getFilCost(0) + ce[2].getFilCost(0);
      for (int i = 0; i < colorFilterNum; i ++) {
        int filCost = ce[0].getFilCost(i) + ce[1].getFilCost(i) + ce[2].getFilCost(i);
        if (filCost < best) { best = filCost; bestInd = i; }
      }
      colorLineFilter[i5] = (byte)bestInd;
      ce[0].signalSel(bestInd); ce[1].signalSel(bestInd); ce[2].signalSel(bestInd);
      if (maxColorFilterUsed < bestInd + 1) { maxColorFilterUsed = bestInd + 1; }
      minx += colorFilterZoneDim;
      if (minx > biWidth - 1) {
        minx = 0;
        miny += colorFilterZoneDim;
      }
      maxx = (int)Math.min(minx + colorFilterZoneDim - 1, biWidth - 1);
      maxy = (int)Math.min(miny + colorFilterZoneDim - 1, biHeight - 1);
    }
    //if (maxColorFilterUsed < colorFilterNum) { setColorFilterNum(maxColorFilterUsed); }
  }

  public void detZoneErrs() {
    int zoneNum = (int)(((biWidth - 1) / colorFilterZoneDim + 1) * ((biHeight - 1) / colorFilterZoneDim + 1));
    int val;
    int totErr = 0;
    errZones = new int[zoneNum];
    image.firstVal();
    for (int i = 0; i < biWidth; i ++) {
      for (int i2 = 0; i2 < biHeight; i2 ++) {
        for (int i3 = 0; i3 < 3; i3++) {
          totErr += Math.abs(image.getCurVal());
          image.nextVal();
        }
      }
    }
    int medErr = totErr / zoneNum;
    int minx = 0;
    int miny = 0;
    int maxx = colorFilterZoneDim - 1;
    int maxy = colorFilterZoneDim - 1;
    for (int i5 = 0; i5 < zoneNum; i5 ++) {
      int curErr = 0;
      for (int i3 = miny; i3 <= maxy; i3 ++) {
        for (int i2 = minx; i2 <= maxx; i2 ++) {
          image.setPoint(i2, i3, 0);
          for (int i4 = 0; i4 < 3; i4++) {
            val = image.getCurVal();
            curErr += Math.abs(val);
            image.nextVal();
          }
        }
      }
      if (curErr > medErr) {
        errZones[i5] = 1;
      } else {
        errZones[i5] = 0;
      }
      minx += colorFilterZoneDim;
      if (minx > biWidth - 1) {
        minx = 0;
        miny += colorFilterZoneDim;
      }
      maxx = (int)Math.min(minx + colorFilterZoneDim - 1, biWidth - 1);
      maxy = (int)Math.min(miny + colorFilterZoneDim - 1, biHeight - 1);
    }
    minx = 0;
    miny = 0;
    maxx = colorFilterZoneDim - 1;
    maxy = colorFilterZoneDim - 1;
    for (int i5 = 0; i5 < zoneNum; i5 ++) {
      int curErr = 0;
      for (int i3 = miny; i3 <= maxy; i3 ++) {
        for (int i2 = minx; i2 <= maxx; i2 ++) {
          image.setPoint(i2, i3, 0);
          for (int i4 = 0; i4 < 3; i4++) {
            if (errZones[i5] == 1) {
              image.setCurVal((byte)200);
            }
            image.nextVal();
          }
        }
      }
      minx += colorFilterZoneDim;
      if (minx > biWidth - 1) {
        minx = 0;
        miny += colorFilterZoneDim;
      }
      maxx = (int)Math.min(minx + colorFilterZoneDim - 1, biWidth - 1);
      maxy = (int)Math.min(miny + colorFilterZoneDim - 1, biHeight - 1);
    }
    bcifViewer.main(this);
  }

  public int[] getZoneErrs() {
    return errZones;
  }

  private int abs2 (int a) {
    return a < 0 ? 256 + a : a;
  }

  private int[][] calcFilColErrs(int p0, int p1, int p2, int[][] res) {
    int ap0 = abs2(p0);
    int ap1 = abs2(p1);
    int ap2 = abs2(p2);
    res[0][0] = ap0;
    res[0][1] = ap0;
    res[0][2] = ap0;
    res[0][3] = abs2(p0 - p2);
    res[0][4] = abs2(p0 - p1);
    res[0][5] = abs2(p0 - p1);
    res[1][0] = abs2(p1 - p0);
    res[1][1] = ap1;
    res[1][2] = abs2(p1 - p0);
    res[1][3] = abs2(p1 - p2);
    res[1][4] = abs2(p1 - p2);
    res[1][5] = ap1;
    res[2][0] = abs2(p2 - p1);
    res[2][1] = ap2;
    res[2][2] = abs2(p2 - p0);
    res[2][3] = ap2;
    res[2][4] = ap2;
    res[2][5] = abs2(p2 - p1);
    return res;
  }

  /*
   * Estimates the value of a point of the image using a linear interpolation with the points immediately
   * left, low and lower left. Called these lf, lw and ll the formula is lf + lw - ll.
   * @param i The x of the point to estimate
   * @param i2 The y of the point to estimate
   * @param i3 The color of the point to estimate
   * @return The guess of the value
   */

  private byte filterLinear(int i, int i2, int i3) {
    byte res = 0;
      if (i > 0 && i2 > 0) {
        int newValx = image.getLeftVal(/*i - 1, i2, i3*/);
        int newValy = image.getLowVal(/*i, i2 - 1, i3)*/);
        int newValxy = image.getLowLeftVal(/*i - 1, i2 - 1, i3*/);
        newValx = newValx < 0 ? newValx + 256 : newValx;
        newValy = newValy < 0 ? newValy + 256 : newValy;
        newValxy = newValxy < 0 ? newValxy + 256 : newValxy;
        int dx = newValx - newValxy;
        int dy = newValy - newValxy;
        int newVal = newValxy + dx  + dy;
        if (newVal > 255) { newVal = 255; }
        if (newVal < 0) { newVal = 0;}
        res = (byte)newVal;
      } else {
	  res = filterBorder(i, i2, i3);
      }
      return res;
  }

  private byte filterNextNext(int i, int i2, int i3) {
    if (i > 0 && i2 > 0) {
      int lf = image.getLeftVal(/*i - 1, i2, i3*/);
      int ll = image.getLowLeftVal(/*i - 1, i2 - 1, i3*/);
      int lw = image.getLowVal(/*i, i2 - 1, i3*/);
      lw = lw < 0 ? lw + 256 : lw;
      lf = lf < 0 ? lf + 256 : lf;
      ll = ll < 0 ? ll + 256 : ll;
      return (byte)(lw + ((lf - ll) >> 1));
    } else { return filterBorder(i, i2, i3); }
  }

  private byte filterNextNextNext(int i, int i2, int i3) {
    if (i > 0 && i2 > 0 && i2 < biHeight - 1) {
      int lf = image.getLeftVal(/*i - 1, i2, i3*/);
      int ll = image.getLowLeftVal(/*i - 1, i2 - 1, i3*/);
      int lw = image.getLowVal(/*i, i2 - 1, i3*/);
      int hl = image.getUpLeftVal(/*i - 1, i2 + 1, i3*/);
      lw = lw < 0 ? lw + 256 : lw;
      lf = lf < 0 ? lf + 256 : lf;
      ll = ll < 0 ? ll + 256 : ll;
      hl = hl < 0 ? hl + 256 : hl;
      return (byte) ( (lf + ll + lw + hl + 1) >> 2);
    } else { return filterBorder(i, i2, i3); }
  }

  private byte filterNextNextNextRU(int i, int i2, int i3) {
    if (i > 0 && i2 > 0 && i < biWidth - 1) {
      int lf = image.getLeftVal(/*i - 1, i2, i3*/);
      int ll = image.getLowLeftVal(/*i - 1, i2 - 1, i3*/);
      int lw = image.getLowVal(/*i, i2 - 1, i3*/);
      int hl = image.getRightDownVal(/*i - 1, i2 + 1, i3*/);
      lw = lw < 0 ? lw + 256 : lw;
      lf = lf < 0 ? lf + 256 : lf;
      ll = ll < 0 ? ll + 256 : ll;
      hl = hl < 0 ? hl + 256 : hl;
      return (byte) ( (lf + ll + lw + hl + 1) >> 2);
    } else { return filterBorder(i, i2, i3); }
  }

  private byte filterHL(int i, int i2, int i3) {
    int res = 0;
    if (i > 0 && i2 < biHeight - 1) {
      res = image.getUpLeftVal(/*i - 1, i2 + 1, i3*/);
    } else {
      res = filterBorder(i, i2, i3);
    }
    return (byte)res;
  }

  private byte filterLR(int i, int i2, int i3) {
    int res = 0;
    if (i < biWidth - 1 && i2 > 0) {
      res = image.getRightDownVal();
    } else {
      res = filterBorder(i, i2, i3);
    }
    return (byte)res;
  }

  /*public byte filterLL(int i, int i2, int i3) {
    int res = 0;
    if (i > 0 && i2 > 0) {
      res = image.getLowLeftVal();
    } else {
      res = filterBorder(i, i2, i3);
    }
    return (byte)res;
  }*/

  private byte filterLowLeft(int i, int i2, int i3) {
    if (i > 0 && i2 > 0) {
      return (byte)image.getLowLeftVal();
    } else { return filterBorder(i, i2, i3); }
  }

  private byte filterMedO(int i, int i2, int i3) {
    if (i > 0 && i2 > 0 && i2 < biHeight - 1) {
      int lw = image.getLeftVal(/*i, i2 - 1, i3*/);
      int hl = image.getUpLeftVal(/*i - 1, i2 + 1, i3*/);
      lw = lw < 0 ? lw + 256 : lw;
      hl = hl < 0 ? hl + 256 : hl;
      return (byte)((lw + hl) >> 1);
    } else { return filterBorder(i, i2, i3); }
  }

  private byte filterMedU(int i, int i2, int i3) {
    if (i2 > 0 && i < biWidth - 1) {
      int lw = image.getLowVal(/*i, i2 - 1, i3*/);
      int hl = image.getRightDownVal(/*i - 1, i2 + 1, i3*/);
      lw = lw < 0 ? lw + 256 : lw;
      hl = hl < 0 ? hl + 256 : hl;
      return (byte)((lw + hl) >> 1);
    } else { return filterBorder(i, i2, i3); }
  }

  private byte filterLinearO(int i, int i2, int i3) {
    if (i > 0 && i2 > 0 && i2 < biHeight - 1) {
      int lf = getIntVal(i - 1, i2, i3);
      int lw = getIntVal(i, i2 - 1, i3);
      int hl = getIntVal(i - 1, i2 + 1, i3);
      return (byte)(lw + hl - lf);
    } else { return filterLinear(i, i2, i3); }
  }


  /*
   * Estimates the value of a point of the image making an average of the point to the left and the point
   * immediately under
   * left, low and lower left. Called these lf, lw and ll the formula is lf + lw - ll.
   * @param i The x of the point to estimate
   * @param i2 The y of the point to estimate
   * @param i3 The color of the point to estimate
   * @return The guess of the value
   */

  private byte filterMed(int i, int i2, int i3) {
    byte res = 0;
    if (i > 0 && i2 > 0) {
      int lw = image.getLeftVal(/*i - 1, i2, i3*/);
      int hl = image.getLowVal(/*i, i2 - 1, i3*/);
      lw = lw < 0 ? lw + 256 : lw;
      hl = hl < 0 ? hl + 256 : hl;
      int newVal = (lw + hl) >> 1;
      res = (byte)newVal;
    } else { res = filterBorder(i, i2, i3); }
    return res;
  }

  /*
   * Estimates the value of a point of the image using the Paeth method. It calculates the point as
   * the linear filter, but choses as guess the point that gets closer to this value between the three
   * used for the interpolation.
   * @param i The x of the point to estimate
   * @param i2 The y of the point to estimate
   * @param i3 The color of the point to estimate
   * @return The guess of the value
   */

  private byte filterPaeth(int i, int i2, int i3) {
    byte res = 0;
    if (i > 0 && i2 > 0) {
      int newValx = (int)(getIntVal(i - 1, i2, i3));
      int newValy = (int)(getIntVal(i, i2 - 1, i3));
      int newValxy = getIntVal(i - 1, i2 - 1, i3);
      int dx = newValx - newValxy;
      int dy = newValy - newValxy;
      int newVal = newValxy + dx  + dy;
      int dl = Math.abs(newVal - newValx);              // Paeth filter
      int dr = Math.abs(newVal - newValy);
      int dll = Math.abs(newVal - newValxy);
      if (dl < dr && dl < dll) {
        newVal = newValx;
      }
      else {
        if (dr < dll) {
          newVal = newValy;
        }
        else {
          newVal = newValxy;
        }
      }
      res = (byte)newVal;
    } else { res = filterBorder(i, i2, i3); }
    return res;
  }

  /*
   * Estimates the value of a point of the image returning the value of the point immediately under.
   * @param i The x of the point to estimate
   * @param i2 The y of the point to estimate
   * @param i3 The color of the point to estimate
   * @return The guess of the value
   */

  private byte filterLow(int i, int i2, int i3) {
    byte res = 0;
    if (i2 > 0) { res = image.getLowVal(/*i, i2 - 1, i3*/); } else { res = filterBorder(i, i2, i3); }
    return res;
  }

  /*
   * Estimates the value of a point of the image returning the value of the point to the left.
   * @param i The x of the point to estimate
   * @param i2 The y of the point to estimate
   * @param i3 The color of the point to estimate
   * @return The guess of the value
   */

  private byte filterLeft(int i, int i2, int i3) {
    byte res = 0;
    if (i > 0) { res = image.getLeftVal(/*i - 1, i2, i3*/); } else { res = filterBorder(i, i2, i3); }
    return res;
  }

  private byte filterPLO(int i, int i2, int i3) {
    int res = 0;
    if ( i > 0 && i2 > 0 && i2 < biHeight - 1) {
      int l = getIntVal(i - 1, i2 + 1, i3);
      int d = getIntVal(i, i2 - 1, i3);
      int ld = getIntVal(i - 1, i2, i3);
      if (ld >= l && ld >= d) {
        if (l > d) { res = d; } else { res = l; }
      } else if (ld <= l && ld <= d) {
        if (l > d) { res = l; } else { res = d; }
      } else {
        res = d + l - ld;
      }
    } else {
        res = filterPL(i, i2, i3);
    }
    return (byte)res;
  }

  /*
   * Estimates the value of a point of the image making a linear interpolation of the two points to the
   * left.
   * @param i The x of the point to estimate
   * @param i2 The y of the point to estimate
   * @param i3 The color of the point to estimate
   * @return The guess of the value
   */

  private byte filterPL(int i, int i2, int i3) {
    int res = 0;
    if ( i > 0 && i2 > 0) {
      int l = image.getLeftVal(/*i - 1, i2, i3*/);
      int d = image.getLowVal(/*i, i2 - 1, i3*/);
      int ld = image.getLowLeftVal(/*i - 1, i2 - 1, i3*/);
      l = l < 0 ? l + 256 : l;
      d = d < 0 ? d + 256 : d;
      ld = ld < 0 ? ld + 256 : ld;
      if (ld >= l && ld >= d) {
        if (l > d) { res = d; } else { res = l; }
      } else if (ld <= l && ld <= d) {
        if (l > d) { res = l; } else { res = d; }
      } else {
        res = d + l - ld;
      }
    } else {
	res = filterBorder(i, i2, i3);
    }
    return (byte)res;
  }

  /*
   * Estimates the value of a point of the image
   * @param i The x of the point to estimate
   * @param i2 The y of the point to estimate
   * @param i3 The color of the point to estimate
   * @return The guess of the value
   */

  private byte filterNext(int i, int i2, int i3) {
    if (i > 0 && i2 > 0) {
      int lf = image.getLeftVal(/*i - 1, i2, i3*/);
      int ll = image.getLowLeftVal(/*i - 1, i2 - 1, i3*/);
      int lw = image.getLowVal(/*i, i2 - 1, i3*/);
      lf = lf < 0 ? lf + 256 : lf;
      ll = ll < 0 ? ll + 256 : ll;
      lw = lw < 0 ? lw + 256 : lw;
      return (byte)(lf + ((lw - ll) >> 1));
    } else { return filterBorder(i, i2, i3); }
  }

    private byte filterBorder(int i, int i2, int i3) {
	if (i == 0 && i2 == 0) {
	    return 0;
	} else if (i == 0) {
	    return image.getLowVal(/*i, i2 - 1, i3*/);
	} else return image.getLeftVal(/*i - 1, i2, i3*/);
    }

  /**
   * Estimates the value of a point of the image using the specified filter.
   * @param i The x of the point to estimate
   * @param i2 The y of the point to estimate
   * @param i3 The color of the point to estimate
   * @param fn The number of the filter to use
   * @return The guess of the value
   */

  public byte filter(int i, int i2, int i3, int fn) {
    byte res = 0;
    if (fn == 0) { res = filterPL(i, i2, i3); }
    else if (fn == 2) { res = filterLeft(i, i2, i3); }
    else if (fn == 3) { res = filterMed(i, i2, i3); }
    else if (fn == 4) { res = filterLow(i, i2, i3); }
    else if (fn == 5) { res = filterLowLeft(i, i2, i3); }
    else if (fn == 6) { res = filterLinear(i, i2, i3); }
    else if (fn == 7) { res = filterLR(i, i2, i3); }
    else if (fn == 8) { res = filterNext(i, i2, i3); }
    else if (fn == 9) { res = filterNextNext(i, i2, i3); }
    else if (fn == 10) { res = filterNextNextNextRU(i, i2, i3); }
    else if (fn == 11) { res = filterMedU(i, i2, i3); }
    else if (fn == 12) { res = filterPaeth(i, i2, i3); }
    else if (fn == 13) { res = filterLinearO(i, i2, i3); }
    else if (fn == 14) { res = filterPLO(i, i2, i3); }
    return (byte)res;
  }

  /**
   * Guesses the value of the point of the image calling the filter(int i, int i2, int i3, int fn) method
   * with the appropriate values depending on the filters chosen by the filterDeterminate call. If no
   * call to this method has been done, the linear filter is used for all the image
   * @param i The x of the point
   * @param i2 The y of the point
   * @param i3 The color of the point
   * @return The guessed value
   */

  public byte filter(int i, int i2, int i3) {
    int xZones = (int)(((biWidth - 1) >> filterZoneDimBits) + 1);
    int zoneRef = (int)((i >> filterZoneDimBits) + xZones * (i2 >> filterZoneDimBits));
    if (lineFilter == null) {
      return filter(i, i2, i3, 5);
    } else {
      return filter(i, i2, i3, lineFilter[zoneRef]);
    }
  }

  private void reportDist(int bitNum) {
  int eNum = 1 << bitNum;
  int[] d = new int[eNum];
  int val;
  for (int i = 0; i < biWidth; i ++) {
     for (int i2 = 0; i2 < biHeight; i2 ++) {
       for (int i3 = 0; i3 < 3; i3 ++) {
         val = image.getVal(i, i2, i3);
         if (val < 0) { val += 256; }
         val = val % eNum;
         d[val] ++;
       }
     }
   }
   long total = biWidth * biHeight * 3;
   for (int i = 0; i < eNum; i ++) {
     System.out.println(i + " values are " + d[i] + " (" + (float)d[i] * 100 / total + "%)");
   }
   long var = 0;
   for (int i = 0; i < eNum; i++) {
     var += Math.abs((total / eNum) - d[i]);
   }
   System.out.println("Medium difference from average is " + var / eNum + " (" + ((float)var * 100 / total) + "%)");
  }

  /**
   * Calculates an hash code using every point of the image. Intestation is not used
   * @return The hash code
   */

  public int hashCode() {
    long res = 0;
    int mod = 0x0FFFFFFF;
    for (int i = 0; i < biWidth; i ++) {
      for (int i2 = 0; i2 < biHeight; i2++) {
        for (int i3 = 0; i3 < 3; i3++) {
          res += image.getIntVal(i, i2, i3) * (i + 1) * (i2 + 1) * (i3 + 1);
        }
      }
    }
    return (int)res;
  }

    /*
     * Sets use of the zig zag visit order in RLE and Huffman layer compression.
     * @param zz True to activate zig zag order, false to deactivate it
     */

  public void setZigzag(boolean zz) {
    zigzag = zz;
  }

    /*
     * Sets the maximum number of filters to be used for compression
     * @param fn The maximum number of used filters
     */

  /*public void setFilterNum (int fn) {
    //filterNum = fn;
  }*/

    /*
     * Sets the maximum number of color filters to be used for compression
     * @param cfn The maximum number of used color filters
     */

  /*public void setColorFilterNum (int cfn) {
    //colorFilterNum = cfn;
  }*/

    /**
     * Sets the number of used pixels in filter and color filter determination.
     * Points used will be one every n for the specified argument.
     * @param fs The number of point from where one point will be considered
     * for filter determination
     */

  public void setFilterStep(int fs) {
    filterStep = fs;
  }

    /**
     * Sets the dimension of a filtering zone. Setting values other than 8 will cause 
     * problems
     * @param zd The side of the square representing a filtering zone, in pixels
     */

  public void setZoneDim(int zd) {
    filterZoneDim = zd;
    filterZoneDimBits = bitMatrix.log2(zd - 1) + 1;
  }

    /**
     * Sets the dimension of a color filtering zone.
     * @param czd The side of the square representing a color filtering zone,
     * in pixels
     */

  public void setColorZoneDim(int czd) {
    colorFilterZoneDim = czd;
    colorFilterZoneDimBits = bitMatrix.log2(czd - 1) + 1;
  }

  private void bitprint(byte b) {
    System.out.print((b >= 0 ? b : b + 256) + " - ");
    if (b >= 0 && b < 10) {System.out.print(" "); }
    if (b >= 0 && b < 100) {System.out.print(" "); }
    for (int i = 0; i < 8; i++) {
      System.out.print(bitMatrix.mod(b,1) + " ");
      b = (byte)(b >>> 1);
    }
    System.out.println();
  }

    /**
     * Returns the value of a specified color filter for a point.
     * @param x The horizontal coordinate of the point
     * @param y The vertical coordinate of the point
     * @param z The color coordinate of the point
     * @param cf The number of the color filter to use
     * @return The estimated value
     */

  public byte colorFilter(int x, int y, int z, int cf) {
    if (cf == 0) { return cFilterPrec(x, y, z); } else
      if (cf == 2) { return cFilterFirst(x, y, z); } else
         if (cf == 5) { return cFilter1(x, y, z); } else
         if (cf == 6) { return cFilter10(x, y, z); } else
            if (cf == 7) { return cFilter20(x, y, z); } else
               if (cf == 8) { return cFilter21(x, y, z); } else
                   if (cf == 3) { return cFilter3(x, y, z); } else
                      if (cf == 4) { return cFilter31(x, y, z); } else

      return 0;
  }

    /**
     * Returns the value of the color filter for a point associated
     * with a point
     * @param x The horizontal coordinate of the point
     * @param y The vertical coordinate of the point
     * @param z The color coordinate of the point
     * @return The estimated value
     */

  public byte colorFilter(int x, int y, int z) {
    int xZones = (int)(((biWidth - 1) >> colorFilterZoneDimBits) + 1);
    int zoneRef = (int)((x >> colorFilterZoneDimBits) + xZones * (y >> colorFilterZoneDimBits));
    return colorFilter(x, y, z, colorLineFilter[zoneRef]);
  }

  private byte cFilterPrec(int x, int y, int z) {
    if (z == 0) { return 0; } else
      return (byte)image.getCurVal(/*x, y, z*/ - 1);
  }

  private byte cFilterFirst(int x, int y, int z) {
    if (z == 0) { return 0; } else
      return (byte)image.getCurVal(/*x, y,*/ -z);
  }

  private byte cFilter1(int x, int y, int z) {
    if (z == 1) { return 0; } else {
      return (byte)image.getCurVal(1 - z);
    }
  }

  private byte cFilter10(int x, int y, int z) {
    if (z == 0 | z == 2) { return 0; } else
      return (byte)image.getCurVal(/*x, y,*/ -1);
  }

  private byte cFilter20(int x, int y, int z) {
    if (z == 0 | z == 1) { return 0; } else
      return (byte)image.getCurVal(/*x, y,*/ -2);
  }

  private byte cFilter21(int x, int y, int z) {
    if (z == 0 | z == 1) { return 0; } else
      return (byte)image.getCurVal(/*x, y,*/ -1);
  }

  private byte cFilter3(int x, int y, int z) {
    if (z == 2) { return 0; } else
      return (byte)image.getCurVal(/*x, y,*/ 2 - z);
  }

  private byte cFilter31(int x, int y, int z) {
    if (z == 2) { return 0; } else
      if (z == 1) { return (byte)image.getCurVal(1); }
         else {
           return (byte)(image.getCurVal(1) + image.getCurVal(2));
         }
  }

  private byte cFilterHigh(int x, int y, int z) {
    if (z == 0) { return 0; } else
      if (z == 1) { return cFilterPrec(x, y, z); } else {
        int v0 = getIntVal(x, y, 0);
        return (byte) ((v0 + getIntVal(x, y, 1)) >> 1);
      }
  }


  /**
   * Activates or deactivates the console. While console is set to true, during compression and
   * decompression a detailed log of the algorithm's operations will be wrote on standard output.
   * @param c The new value of console.
   */

  public static void setConsole(boolean c) {
    console = c;
  }

  private static void println() {
    if (console) { System.out.println(); }
  }

  private static void println(String arg) {
    if (console) { System.out.println(arg); }
  }

  private static void print(String arg) {
    if (console) { System.out.print(arg); }
  }

    /**
     * Returns the first order entropy of a group of simbols given their frequencies
     * @param simbolsFreq The frequencies of the simbols
     * @return The simbols' entropy
     */

  public static double entropy(int[] simbolsFreq) {
    double e = 0;
    long tot = 0;
    double res = 0;
    for (int i = 0; i < simbolsFreq.length; i ++) {
      tot += simbolsFreq[i];
    }
    double[] relFreq = new double[simbolsFreq.length];
    for (int i = 0; i < simbolsFreq.length; i++) {
      if (simbolsFreq[i] > 0) {
        relFreq[i] = (double)tot / simbolsFreq[i];
      }
    }
    for (int i = 0; i < relFreq.length; i ++) {
      if (relFreq[i] > 0 && relFreq[i] != 1) {
        double esum = Math.log(relFreq[i]) / Math.log(2);
        e = e + esum / relFreq[i];
      }
    }
    return e;
  }

  public void writePpmImage(String filename) {
    try {
      OutputStream out = new BufferedOutputStream(new FileOutputStream(filename), 8096);
      int xaxis = 0;
      int yaxis = 0;
      int caxis = 0;
      int i = 0;
      int wroteBytes = 0;
      out.write("P6\n".getBytes());
      out.write((biWidth + " " + biHeight + "\n").getBytes());
      out.write("255\n".getBytes());
      while (yaxis < biHeight) {
        try {
          out.write(image.getVal(xaxis, yaxis, 2 - caxis));
          wroteBytes ++;
        } catch (Exception e) {
          e.printStackTrace();
        }
        caxis ++;
        i++;
        if (caxis == 3) {
          caxis = 0;
          xaxis++;
          if (xaxis == biWidth) {
            xaxis = 0;
            yaxis++;
          }
        }
      }
      out.close();
    } catch (Exception e) {e.printStackTrace();}
  }

  private void writeLayers() {
    OutputStream out = null;
    try {
      for (int i = 0; i < 3; i++) {
        out = new BufferedOutputStream(new FileOutputStream("Layer " + i + ".lay"));
        for (int i2 = 0; i2 < biWidth; i2++) {
          for (int i3 = 0; i3 < biHeight; i3++) {
            out.write(image.getVal(i2, i3, i));
          }
        }
        out.close();
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}

