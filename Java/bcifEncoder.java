package bcif;
import java.io.*;

/**
 * Core of the BCIF compression and decompression program.
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class bcifEncoder {

  private static int maxDecisions = 64;
  private static int filterZoneDim = 8;
  private static int filterZoneDimBits = 3;
  private static int colorFilterZoneDim = 8;
  private static int colorFilterZoneDimBits = 3;
  public final static int version = 1;
  public final static int subVersion = 0;
  public final static int beta = 1;
  private static boolean hashPrint = false;
  private static final boolean writeParts = false;
  private static final int[] log2 = Const.log2;
  private static boolean advancedColorFilter = false;
  private static boolean timeVerbose = false;
  private static boolean verbose = false;

  /**
   *  Determines the context for a given value
   */
   
  public static int decide(int x, int y, int col, int[][] precInfo, int[][] info, int curFil, int curColFil, int left, int low) {
    int leftInfo = 0;
    if (x > 0) {
      leftInfo = info[col][x - 1];
    } else {
      leftInfo = precInfo[col][x];
    }
    int lowInfo = precInfo[col][x];
    int caos = log2[abs(left) + abs(low) << 1] + 1;
    if (caos > 7) {
      caos = 7;
    }
    int caos2 = 0;
    if (col > 0) {
      caos2 = info[col - 1][x];
    } else {
      caos2 = (leftInfo + lowInfo) >> 1;
    }
    int curCaos = (((caos << 2) + (leftInfo + lowInfo + caos + caos2)) >> 2);
    info[col][x] = (curCaos >> 1);
    return curCaos + (col << 4);
  }

  /**
   *  Determines the context for a given value relative to color 0
   */
   
  public static int decideSafe0(int x, int y, int[][] precInfo, int[][] info, int curFil, int curColFil, int left, int low) {
    int leftInfo = 0;
    leftInfo = info[0][x - 1];
    int lowInfo = precInfo[0][x];
    int caos = log2[abs(left) + abs(low) << 1] + 1;
    if (caos > 7) {
      caos = 7;
    }
    int caos2 = (leftInfo + lowInfo) >> 1;
    int curCaos = (((caos << 2) + (leftInfo + lowInfo + caos + caos2)) >> 2);
    info[0][x] = (curCaos >> 1);
    return curCaos;
  }

  /**
   *  Determines the context for a given value relative to color 1 or 2
   */
   
  public static int decideSafe12(int x, int y, int col, int[][] precInfo, int[][] info, int curFil, int curColFil, int left, int low) {
    int leftInfo = 0;
    leftInfo = info[col][x - 1];
    int lowInfo = precInfo[col][x];
    int caos = log2[abs(left) + abs(low) << 1] + 1;
    if (caos > 7) {
      caos = 7;
    }
    int caos2 = info[col - 1][x];
    int curCaos = (((caos << 2) + (leftInfo + lowInfo + caos + caos2)) >> 2);
    info[col][x] = (curCaos >> 1);
    return curCaos + (col << 4);
  }

  /**
   *  Encode an array of integers not greater than 12 representing the used 
   *  spatial filters
   *  @param filters The array of used filters
   *  @param bw The writer where to write the encoded filters
   */
   
  public static void writeFilters(byte[] filters, bitWriter bw) {
    int[] freqs = new int[12];
    for (int i = 0; i < filters.length; i ++) {
      freqs[filters[i]] ++;
    }
    HTree ht = HTree.buildHTree(freqs);
    ht.cut();
    ht = HTree.buildHTreeFromBits(ht.getCodeBits());
    ht.writeHTreeFromBits(bw);
    for (int i = 0; i < filters.length; i ++) {
      ht.writeVal(bw, filters[i]);
    }
  }

  
  // Experimental, advanced color filters are not used
   
   
  private static void writeAdvancedColFilters(byte[] colorFilters, bitWriter bw) {
      bw.writeBit(1);
  }

  /**
   *  Encode an array of integers not greater than 7 representing the used 
   *  spatial filters
   *  @param filters The array of used filters
   *  @param bw The writer where to write the encoded filters
   */
   
  public static void writeColFilters(byte[] colorFilters, bitWriter bw) {
    if (advancedColorFilter) {
      writeAdvancedColFilters(colorFilters, bw);
    } else {
      bw.writeBit(0);
      int[] cfreqs = new int[64];
      int cf = -1;
      for (int i = 0; i < colorFilters.length; i++) {
        if (cf == -1) { cf = colorFilters[i]; } else {
          cfreqs[ (cf << 3) + colorFilters[i]]++;
          cf = -1;
        }
      }
      if (cf > -1) {
        cfreqs[ (cf << 3)]++;
        cf = -1;
      }
      HTree cht = HTree.buildHTree(cfreqs);
      cht.cut();
      cht = HTree.buildHTreeFromBits(cht.getCodeBits());
      cht.writeHTreeFromBits(bw);
      for (int i = 0; i < colorFilters.length - 1; i += 2) {
        cht.writeVal(bw, (colorFilters[i] << 3) + colorFilters[i + 1]);
      }
      if ( (colorFilters.length & 1) == 1) {
        cht.writeVal(bw, (colorFilters[colorFilters.length - 1] << 3));
      }
    }
  }

  /**
   *  Reads from a compressed bitstream the filters used during compression
   */
   
  public static byte[] readFilters(bitReader br, int w, int h, int filterZoneDim) {
    int filterDim = (int)(((w - 1) / filterZoneDim + 1) * ((h - 1) / filterZoneDim + 1));
    byte[] res = new byte[filterDim];
    HTree ht = HTree.readHTreeFromBits(br, 12);
    ht.createLookup();
    for (int i = 0; i < filterDim; i ++) {
      res[i] = (byte)ht.readEfVal(br);
    }
    return res;
  }

  // Experimental, advanced color filters are not used
  
  private static byte[] readAdvancedColorFilters(bitReader br, int w, int h, int filterZoneDim) {
    return null;  // Not implemented
  }

  /**
   *  Reads from a compressed bitstream the color filters used during compression
   */
   
  public static byte[] readColorFilters(bitReader br, int w, int h, int filterZoneDim) {
    int type = br.readBit();
    if (type == 1) {
      return readAdvancedColorFilters(br, w, h, filterZoneDim);
    } else {
      int filterDim = (int) ( ( (w - 1) / colorFilterZoneDim + 1) *
                             ( (h - 1) / colorFilterZoneDim + 1));
      byte[] res = new byte[filterDim];
      HTree ht = HTree.readHTreeFromBits(br, 64);
      int cf = -1;
      ht.createLookup();
      for (int i = 0; i < filterDim; i++) {
        if (cf == -1) {
          cf = ht.readEfVal(br);
          res[i] = (byte) (cf >> 3);
        } else {
          res[i] = (byte) (cf & 7);
          cf = -1;
        }
      }
      return res;
    }
  }

  public static int abs(int val) {
    return val < 128 ? val : 256 - val;
  }

  /**
   *  Integer log2 of the argument, extended to return -1 if the argument is 0
   */
   
  public static int log2(int arg) {
    int res = -1;
    while (arg > 0) { arg = arg >> 1; res ++; }
    return res;
  }

  public bcifEncoder() {

  }

  private static int ccount = 0;

  public static void compressMatrix(byteMatrix image, bitWriter out, byte[] filters, byte[] colorFilters) {
    long initTime = System.currentTimeMillis();
    int filterWroteBits = out.getWroteBits();
    if (verbose) {
    	System.out.println("Filter and intestation size is " + filterWroteBits + " bits (" + (filterWroteBits >> 3) + " bytes)");
    }
    HTreeWriterGestor [] htg = new HTreeWriterGestor[maxDecisions];
    for (int i3 = 0; i3 < maxDecisions; i3 ++) {
      htg[i3] = new HTreeWriterGestor();
    }
    image.firstVal();
    int w = image.width();
    int h = image.height();
    int where = 0;
    int[][] precInfo = new int[3][w];
    int[][] info = new int[3][w];
    int curFil = 0;
    int curColFil = 0;
    int xZones = ((w - 1) >> filterZoneDimBits) + 1;
    int colxZones = ((w - 1) >> colorFilterZoneDimBits) + 1;
    int[][] low = new int[3][w];
    int[][] precLow = new int[3][w];
    OutputStream[] parts = new OutputStream[maxDecisions];  // For experimentation purpose
    if (writeParts) {
      for (int i = 0; i < maxDecisions; i++) {
        try { parts[i] = new BufferedOutputStream(new FileOutputStream("Part_" + i), 8096);  }
        catch (FileNotFoundException ex) {
        }
      }
    }                                                       // ---------------------------
    image.firstVal();
    for (int i2 = 0; i2 < h; i2 ++) {
      int[] left = new int[3];
      precLow = low;
      low = new int[3][w];
      precInfo = info;
      info = new int[3][w];
      for (int i = 0; i < w; i ++) {
        for (int i3 = 0; i3 < 3; i3 ++) {
          where = decide(i, i2, i3, precInfo, info, curFil, curColFil, left[i3], precLow[i3][i]);
          int val = image.getIntCurVal();
          if (writeParts) {
            try { parts[where].write(val); }
            catch (IOException ex1) { }
          }
          htg[where].putVal(val);
          left[i3] = val;
          low[i3][i] = val;
          image.nextVal();
        }
      }
    }
    if (writeParts) {
      for (int i = 0; i < maxDecisions; i++) {
        try { parts[i].close(); }
        catch (Exception ex) {
        }
      }
    }
    for (int i3 = 0; i3 < maxDecisions; i3 ++) {
      htg[i3].endGathering();
      htg[i3].setBitWriter(out);
      htg[i3].writeTree();
    }
    if (verbose) {
    	System.out.println("Huffman tree used bits are " + out.getWroteBits() + " (" +
                          ((out.getWroteBits() - filterWroteBits) >> 3) + " bytes) for " + 
                          maxDecisions + " trees.");
	}
    image.firstVal();
    precInfo = new int[3][w];
    info = new int[3][w];
    low = new int[3][w];
    precLow = new int[3][w];
    for (int i2 = 0; i2 < h; i2 ++) {
      int[] left = new int[3];
      precLow = low;
      low = new int[3][w];
      precInfo = info;
      info = new int[3][w];
      for (int i = 0; i < w; i ++) {
        for (int i3 = 0; i3 < 3; i3 ++) {
          image.setPoint(i, i2, i3);
          where = decide(i, i2, i3, precInfo, info, curFil, curColFil, left[i3], precLow[i3][i]);
          int val = image.getIntCurVal();
          htg[where].writeVal(val);
          left[i3] = val;
          low[i3][i] = val;
        }
      }
    }
    long execTime = System.currentTimeMillis() - initTime;
    if (timeVerbose) { System.out.println("Compression took " + execTime + " ms."); }
  }

  public static void compress(String source) {
    compress (source, source + ".bcif" + subVersion);
  }

  public static void compress(String source, String dest) {
  	compress(new BmpImage(source), dest);	
  }
  
  public static void compress(BmpImage si, String dest) {	
    long initTime = System.currentTimeMillis();
    if (hashPrint) { System.out.println("Initial hash code is " + si.hashCode()); }
    si.setZoneDim(filterZoneDim);
    si.setColorZoneDim(colorFilterZoneDim);
    si.filterDeterminate();
    si.applyFilter();
    if (hashPrint) { System.out.println("After filter hash code is " + si.hashCode()); }
    si.colorFilterDeterminate();
    si.applyColFilter();
    if (hashPrint) { System.out.println("After color filter hash code is " + si.hashCode()); }
    byteMatrix image = si.getImage();
    long filterTime = System.currentTimeMillis() - initTime;
    if (timeVerbose) { System.out.println("Filtering took " + filterTime + " ms."); }
    try {
      OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
      bitWriter bw = new bitWriter(out);
      writeBcifHeader(si, bw);      
      writeFilters(si.getZoneFilters(), bw);
      writeColFilters(si.getColorZoneFilters(), bw);
      compressMatrix(image, bw, si.getZoneFilters(), si.getColorZoneFilters());
      bw.close();
      int compressedSize = (int)(new File(dest)).length();
      long totalTime = System.currentTimeMillis() - initTime;
      if (timeVerbose) { System.out.println("Total execution time was " + totalTime + " ms."); }
      System.out.println("Image compressed to " + ((compressedSize) >> 10) + " KB");
    } catch (Exception e) { 
      System.err.println("Error caused program to abort");
    }

  }

  public static void writeBcifHeader(BmpImage image, bitWriter bw) {
  	  	
	  bw.fwrite((int)'B', 8);
	  bw.fwrite((int)'C', 8);
	  bw.fwrite((int)'I', 8);
	  bw.fwrite((int)'F', 8);
	    	  		
	  bw.fwrite(version, 16);
	  bw.fwrite(subVersion, 16);
	  bw.fwrite(beta, 8);
	  
	  int extraDataLength = 0;
	  bw.fwrite(extraDataLength, 24);
	  
	  for (int i = 0; i < extraDataLength; i++) {   // Write auxiliary data
	  	bw.fwrite(0, 8);                            // Unused for now
	  }
	  	    	  	
      bw.fwrite((int)image.getWidth(), 32);
      bw.fwrite((int)image.getHeight(), 32);  	      
      
      bw.fwrite((int)image.getResX(), 32);
      bw.fwrite((int)image.getResY(), 32);  	      
      
  }
  
  public static void readBcifHeader(bitReader br, bmpWriter bw) {
  	  	
      String bint = "";
      bint = bint + (char)br.fread(8);  	  	
      bint = bint + (char)br.fread(8);
      bint = bint + (char)br.fread(8);
      bint = bint + (char)br.fread(8);
      
      if (! bint.equals("BCIF")) {
      	throw new RuntimeException("Input file is not a valid BCIF file.");
      }
      
      int readVersion = br.fread(16);
      int readSubVersion = br.fread(16);
      int readBeta = br.fread(8);
      
      if (readVersion > version || readVersion == version && readSubVersion > subVersion) {
      	System.err.println("BCIF file version (" + readVersion + "." + readSubVersion + 
      	                   ") is newer than decoder version (" + version + "." + 
      	                   subVersion + ").");
		System.err.println("Cannot decompress file ! Program terminating.");
		throw new RuntimeException("Wrong file version: " + readVersion + "." + readSubVersion);
      }
      
	  int extraDataLength = br.fread(24);
	  
	  for (int i = 0; i < extraDataLength; i++) {   // Read auxiliary data
	  	br.fread(8);                                // Unused for now
	  }
	        
      int width = br.fread(32);
      int height = br.fread(32);
      bw.setDims(width, height);    
      
      int resX = br.fread(32);
      int resY = br.fread(32);
      bw.setRes(resX, resY);
      
      if (verbose) { System.out.println("Bcif header read, image is " + width + 
                     "x" + height + ", resolution " + resX + "x" + resY + ", BCIF version: " +
                     readVersion + "." + readSubVersion + " beta = " + readBeta);}
      
      bw.writeBmpIntest();
  }
    
    /**
     *  Decompresses a BCIF file to a BmpImage Object
     *  @param source The source BCIF file
     */
     
  public static BmpImage decompressBcifToBmpObj(String source) {
    BmpImage res = null;
    try {
      InputStream in = new BufferedInputStream(new FileInputStream(source));
      bitReader br = new bitReader(in);
      byteMatrixBmpWriter image = new byteMatrixBmpWriter();
      readBcifHeader(br, image);
      decompress(br, image);
      res = new BmpImage(image.getMatrix());
      res.setRes(image.getResX(), image.getResY());
      if (hashPrint) { System.out.println("Decompressed image hash code is " + res.hashCode()); }
    } catch (Exception ex) {
    	System.out.println("Error in the decompression process - " + ex.getMessage());
    	throw new RuntimeException(ex.getMessage()); 
    }
    return res;
  }

    /**
     *  Decompresses a BCIF file writing the output on a generic bmpWriter
     *  @param br The bitReader linked to the BCIF compressed bitstream
     *  @param image The writer where to put output data
     */
     
  public static void decompressBcifFromReaderToWriter(bitReader br, bmpWriter image) {
  	try {
  		readBcifHeader(br, image);
  		decompress(br, image);
  	} catch (Exception ex) {
    	System.out.println("Error in the decompression process - " + ex.getMessage());
    	throw new RuntimeException(ex.getMessage()); 
    }
  }
  
    /**
     *  Decompresses a BCIF file to a BMP file
     *  @param source The BCIF input file
     *  @param image The BMP output file
     */
     
  public static void decompressBcifToBmpFile(String source, String dest) {
    try {
      InputStream in = new BufferedInputStream(new FileInputStream(source));
      bitReader br = new bitReader(in);
      fileBmpWriter image = new fileBmpWriter(dest);
      readBcifHeader(br, image);
      decompress(br, image);
    } catch (Exception ex) {
    	System.out.println("Error in the decompression process - " + ex.getMessage());
    	throw new RuntimeException(ex.getMessage()); 
    }
  }

    /**
     *  Decompresses a BCIF file and visualizes it in a streamable viewer
     *  @param source The BCIF input file
     */
     
  public static void decompressBcifToStreamableViewer(String source) {
    try {
      InputStream in = new BufferedInputStream(new FileInputStream(source));
      bitReader br = new bitReader(in);
      streamableArrayBmpWriter image = new streamableArrayBmpWriter();
      image.setView(true);
      image.setParts(20);
      image.setTitle(source);
      readBcifHeader(br, image);
      decompress(br, image);
    } catch (Exception ex) {
    	System.out.println("Error in the decompression process - " + ex.getMessage());
    	throw new RuntimeException(ex.getMessage()); 
    }
  }

    /**
     *  Decompresses a BCIF file and visualizes it in a (non-streamable) viewer
     *  @param source The BCIF input file
     */
     
  public static void decompressBcifToViewer(String source) {
    try {
      InputStream in = new BufferedInputStream(new FileInputStream(source));
      bitReader br = new bitReader(in);
      arrayBmpWriter image = new arrayBmpWriter();
      image.setView(true);
      image.setTitle(source);
      readBcifHeader(br, image);
      decompress(br, image);
    } catch (Exception ex) {
    	System.out.println("Error in the decompression process - " + ex.getMessage());
    	throw new RuntimeException(ex.getMessage()); 
    }
  }

  /**
   *  Computes the index of a color filter to be applied to a point with 
   *  specified coordinates
   */
   
  private static int colorFilterOfZone(int i, int i2, int width, byte[] colorLineFilter) {
    int xZones = (int)(((width - 1) >> colorFilterZoneDimBits) + 1);
    int zoneRef = (int)((i >> colorFilterZoneDimBits) + xZones * (i2 >> colorFilterZoneDimBits));
    return colorLineFilter[zoneRef];
  }

  /**
   *  Computes the index of a filter to be applied to a point with 
   *  specified coordinates
   */

  private static int filterOfZone(int i, int i2, int width, byte[] lineFilter) {
    int xZones = (int)(((width - 1) >> filterZoneDimBits) + 1);
    int zoneRef = (int)((i >> filterZoneDimBits) + xZones * (i2 >> filterZoneDimBits));
    return lineFilter[zoneRef];
  }

  /**
   *  Method that executes the decompression of an image. Written in human readable code,
   *  it is slightly slower than the decompress method. When called, this method requires
   *  that the methods setRes, setDims and writeIntest have already been called on the
   *  bmpWriter.
   *  @param br The reader associated to the compressed bitstream.
   *  @param out The writer associated to the output of the compression.
   */
   
  public static void hdecompress(bitReader br, bmpWriter out) {
    System.out.print("Decompressing file... ");
    int width = out.getWidth();
    int height = out.getHeight();
    long initTime = System.currentTimeMillis();
    byte[] filters = readFilters(br, width, height, filterZoneDim);
    byte[] colorFilters = readColorFilters(br, width, height, colorFilterZoneDim);
    HTreeReaderGestor hrg = new HTreeReaderGestor(maxDecisions, br);
    huffmanReader[] readers = hrg.getReaders();
    int where = 0;
    int[][] precInfo = new int[3][width];
    int[][] info = new int[3][width];
    int curFil = 0;
    int curColFil = 0;
    int xZones = ((width - 1) >> filterZoneDimBits) + 1;
    int colxZones = ((width - 1) >> colorFilterZoneDimBits) + 1;
    int[][] low = new int[3][width];
    int[][] precLow = new int[3][width];
    byte[] leftFiltered = new byte[3];
    byte[][] lowFiltered = new byte[3][width];
    byte[][] precLowFiltered = new byte[3][width];
    filterGestor fg = new filterGestor();
    filterer fil = null;
    colorFilterer cfil = null;
    byte[] val = new byte[3];
    byte[] uncfval = new byte[3];
    for (int i2 = 0; i2 < height; i2 ++) {      
      int[] left = new int[3];
      int[][] temp = precLow;
      precLow = low;
      low = temp; 
      temp = info;
      precInfo = info;
      info = temp;
      byte[][] tempbyte = precLowFiltered;
      precLowFiltered = lowFiltered;
      lowFiltered = tempbyte;
      for (int i = 0; i < width; i ++) {                    // Uncompress 'raw' values
        curFil = filterOfZone(i, i2, width, filters);
        curColFil = colorFilterOfZone(i, i2, width, colorFilters);
        fil = fg.getFilter(curFil);
        cfil = fg.getColFilter(curColFil);
        for (int i3 = 0; i3 < 3; i3 ++) {
          where = decide(i, i2, i3, precInfo, info, curFil, curColFil, left[i3], precLow[i3][i]);
          left[i3] = readers[where].readVal(br);
          low[i3][i] = left[i3];
          val[i3] = (byte)left[i3];
        }
        for (int i3 = 0; i3 < 3; i3 ++) {                  // Remove color filters
          val[i3] = (byte)(val[i3] + cfil.colFilter(val[0], val[1], val[2], i3));
        }
        if (i == 0 || i == width - 1 || i2 == 0) {         // Remove standard filters
          for (int i3 = 0; i3 < 3; i3++) {                 // Edge case
            val[i3] = (byte) (val[i3] + safeFilter(i, i2, leftFiltered[i3], precLowFiltered[i3], curFil, fil, width));
            out.write(val[i3]);
            leftFiltered[i3] = val[i3];
            lowFiltered[i3][i] = val[i3];
          }
        } else {
          for (int i3 = 0; i3 < 3; i3++) {                 // General case
            val[i3] = (byte) (val[i3] + fil.filter(leftFiltered[i3], precLowFiltered[i3][i],
                              precLowFiltered[i3][i-1],precLowFiltered[i3][i+1]));
            out.write(val[i3]);
            leftFiltered[i3] = val[i3];
            lowFiltered[i3][i] = val[i3];
          }
        }
      }
    }
    System.out.println("done. ");
    long finalTime = System.currentTimeMillis() - initTime;
    if (timeVerbose) { System.out.println("Decompression took " + (finalTime) + " ms."); }
  }

  /**
   *  Method that executes the decompression of an image. Written in a non human readable 
   *  code, it is slightly faster than the hdecompress method. When called, this method 
   *  requires that the methods setRes, setDims and writeIntest have already been called on the
   *  bmpWriter.
   *  @param br The reader associated to the compressed bitstream.
   *  @param out The writer associated to the output of the compression.
   */

  public static void decompress(bitReader br, bmpWriter out) {
    try {
      System.out.print("Decompressing file... ");
      long initTime = System.currentTimeMillis();
      int width = out.getWidth();
      int height = out.getHeight();      
      byte[] filters = readFilters(br, width, height, filterZoneDim);
      byte[] colorFilters = readColorFilters(br, width, height, colorFilterZoneDim);
      HTreeReaderGestor hrg = new HTreeReaderGestor(maxDecisions, br);
      huffmanReader[] readers = hrg.getReaders();
      int where = 0;
      int[][] precInfo = new int[3][width];
      int[][] info = new int[3][width];
      int curFil = 0;
      int curColFil = 0;
      int xZones = ((width - 1) >> filterZoneDimBits) + 1;
      int colxZones = ((width - 1) >> colorFilterZoneDimBits) + 1;
      int[][] low = new int[3][width];
      int[][] precLow = new int[3][width];
      int[] left = new int[3];
      byte[] lowF0 = new byte[width];
      byte[] precLowF0 = new byte[width];
      byte[] lowF1 = new byte[width];
      byte[] precLowF1 = new byte[width];
      byte[] lowF2 = new byte[width];
      byte[] precLowF2 = new byte[width];
      byte leftF0 = 0;
      byte leftF1 = 0;
      byte leftF2 = 0;

      precLow = low;
      low = new int[3][width];
      precInfo = info;
      info = new int[3][width];
      filterGestor fg = null;
      if (advancedColorFilter) {
        //fg = new advancedFilterGestor();       Not implemented
      } else {
        fg = new filterGestor();
      }
      curFil = filterOfZone(0, 0, width, filters);
      curColFil = colorFilterOfZone(0, 0, width, colorFilters);
      filterer fil = fg.getFilter(curFil);
      colorFilterer cfil = fg.getColFilter(curColFil);
      int val0, val1, val2;
      //for (int i3 = 0; i3 < 3; i3 ++) {                   //        i == 0, i2 == 0
        where = decide(0, 0, 0, precInfo, info, curFil, curColFil, left[0], precLow[0][0]);
        val0 = readers[where].readVal(br);
        left[0] = val0;
        low[0][0] = val0;
        where = decide(0, 0, 1, precInfo, info, curFil, curColFil, left[1], precLow[1][0]);
        val1 = readers[where].readVal(br);
        left[1] = val1;
        low[1][0] = val1;
        where = decide(0, 0, 2, precInfo, info, curFil, curColFil, left[2], precLow[2][0]);
        val2 = readers[where].readVal(br);
        left[2] = val2;
        low[2][0] = val2;
        val0 = (byte)(val0 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 0));
        val1 = (byte)(val1 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 1));
        val2 = (byte)(val2 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 2));
        leftF0 = (byte)val0;
        leftF1 = (byte)val1;
        leftF2 = (byte)val2;
        lowF0[0] = leftF0;
        lowF1[0] = leftF1;
        lowF2[0] = leftF2;
        out.writeTriplet(leftF0, leftF1, leftF2);

      //}

      for (int i = 1; i < width - 1; i ++) {             //         i2 == 0
        if ((i & (filterZoneDim - 1)) == 0) {
          curFil = filterOfZone(i, 0, width, filters);
          curColFil = colorFilterOfZone(i, 0, width, colorFilters);
          fil = fg.getFilter(curFil);
          cfil = fg.getColFilter(curColFil);
        }
        //for (int i3 = 0; i3 < 3; i3 ++) {
          where = decide(i, 0, 0, precInfo, info, curFil, curColFil, left[0], precLow[0][i]);
          val0 = readers[where].readVal(br);
          left[0] = val0;
          low[0][i] = val0;
          where = decide(i, 0, 1, precInfo, info, curFil, curColFil, left[1], precLow[1][i]);
          val1 = readers[where].readVal(br);
          left[1] = val1;
          low[1][i] = val1;
          where = decide(i, 0, 2, precInfo, info, curFil, curColFil, left[2], precLow[2][i]);
          val2 = readers[where].readVal(br);
          left[2] = val2;
          low[2][i] = val2;
          val0 = (byte)(val0 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 0));
          val1 = (byte)(val1 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 1));
          val2 = (byte)(val2 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 2));
          val0 = (byte)(val0 + safeFilter(i, 0, leftF0, precLowF0[i], precLowF0[i - 1],
                                          precLowF0[i + 1], curFil, fil, width));
          val1 = (byte)(val1 + safeFilter(i, 0, leftF1, precLowF1[i], precLowF1[i - 1],
                                          precLowF1[i + 1], curFil, fil, width));
          val2 = (byte)(val2 + safeFilter(i, 0, leftF2, precLowF2[i], precLowF2[i - 1],
                                          precLowF2[i + 1], curFil, fil, width));

          leftF0 = (byte)val0;
          leftF1 = (byte)val1;
          leftF2 = (byte)val2;
          lowF0[i] = leftF0;
          lowF1[i] = leftF1;
          lowF2[i] = leftF2;
          out.writeTriplet(leftF0, leftF1, leftF2);


        //}
      }

      curFil = filterOfZone(width - 1, 0, width, filters);           // i2 == 0, i = width - 1
      curColFil = colorFilterOfZone(width - 1, 0, width, colorFilters);
      fil = fg.getFilter(curFil);
      cfil = fg.getColFilter(curColFil);

      //for (int i3 = 0; i3 < 3; i3 ++) {
        where = decide(width - 1, 0, 0, precInfo, info, curFil, curColFil, left[0], precLow[0][width - 1]);
        val0 = readers[where].readVal(br);
        left[0] = val0;
        low[0][width - 1] = val0;
        where = decide(width - 1, 0, 1, precInfo, info, curFil, curColFil, left[1], precLow[1][width - 1]);
        val1 = readers[where].readVal(br);
        left[1] = val1;
        low[1][width - 1] = val1;
        where = decide(width - 1, 0, 2, precInfo, info, curFil, curColFil, left[2], precLow[2][width - 1]);
        val2 = readers[where].readVal(br);
        left[2] = val2;
        low[2][width - 1] = val2;
        val0 = (byte)(val0 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 0));
        val1 = (byte)(val1 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 1));
        val2 = (byte)(val2 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 2));
        val0 = (byte)(val0 + safeFilter(width - 1, 0, leftF0, precLowF0[width - 1], precLowF0[width - 2],
                                        (byte)0, curFil, fil, width));
        val1 = (byte)(val1 + safeFilter(width - 1, 0, leftF1, precLowF1[width - 1], precLowF1[width - 2],
                                        (byte)0, curFil, fil, width));
        val2 = (byte)(val2 + safeFilter(width - 1, 0, leftF2, precLowF2[width - 1], precLowF2[width - 2],
                                        (byte)0, curFil, fil, width));

        leftF0 = (byte)val0;
        leftF1 = (byte)val1;
        leftF2 = (byte)val2;
        lowF0[width - 1] = leftF0;
        lowF1[width - 1] = leftF1;
        lowF2[width - 1] = leftF2;
        out.writeTriplet(leftF0, leftF1, leftF2);


      //}

      for (int i2 = 1; i2 < height; i2 ++) {             // i2 > 0
        left = new int[3];
        precLow = low;
        low = new int[3][width];
        precInfo = info;
        info = new int[3][width];
        precLowF0 = lowF0;
        lowF0 = new byte[width];
        precLowF1 = lowF1;
        lowF1 = new byte[width];
        precLowF2 = lowF2;
        lowF2 = new byte[width];
        curFil = filterOfZone(0, i2, width, filters);
        curColFil = colorFilterOfZone(0, i2, width, colorFilters);
        fil = fg.getFilter(curFil);
        cfil = fg.getColFilter(curColFil);

        //for (int i3 = 0; i3 < 3; i3 ++) {                //    i == 0
        where = decide(0, i2, 0, precInfo, info, curFil, curColFil, left[0], precLow[0][0]);
        val0 = readers[where].readVal(br);
        left[0] = val0;
        low[0][0] = val0;
        where = decide(0, i2, 1, precInfo, info, curFil, curColFil, left[1], precLow[1][0]);
        val1 = readers[where].readVal(br);
        left[1] = val1;
        low[1][0] = val1;
        where = decide(0, i2, 2, precInfo, info, curFil, curColFil, left[2], precLow[2][0]);
        val2 = readers[where].readVal(br);
        left[2] = val2;
        low[2][0] = val2;
        val0 = (byte)(val0 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 0));
        val1 = (byte)(val1 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 1));
        val2 = (byte)(val2 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 2));
        val0 = (byte)(val0 + safeFilter(0, i2, leftF0, precLowF0[0], (byte)0,
                                        precLowF0[1], curFil, fil, width));
        val1 = (byte)(val1 + safeFilter(0, i2, leftF1, precLowF1[0], (byte)0,
                                        precLowF1[1], curFil, fil, width));
        val2 = (byte)(val2 + safeFilter(0, i2, leftF2, precLowF2[0], (byte)0,
                                        precLowF2[1], curFil, fil, width));

        leftF0 = (byte)val0;
        leftF1 = (byte)val1;
        leftF2 = (byte)val2;
        lowF0[0] = leftF0;
        lowF1[0] = leftF1;
        lowF2[0] = leftF2;
        out.writeTriplet(leftF0, leftF1, leftF2);

        byte low0 = precLowF0[0];
        byte low1 = precLowF1[0];
        byte low2 = precLowF2[0];
        byte low0n = precLowF0[1];
        byte low1n = precLowF1[1];
        byte low2n = precLowF2[1];
        byte low0p = 0;
        byte low1p = 0;
        byte low2p = 0;
        //}

        for (int i = 1; i < width - 1; i ++) {            // i > 0, i2 > 0  -------------------> Parte intensa
          if ((i & (filterZoneDim - 1)) == 0) {
            curFil = filterOfZone(i, i2, width, filters);
            curColFil = colorFilterOfZone(i, i2, width, colorFilters);
            fil = fg.getFilter(curFil);
            cfil = fg.getColFilter(curColFil);
          }
          //for (int i3 = 0; i3 < 3; i3 ++) {
            where = decideSafe0(i, i2, precInfo, info, curFil, curColFil, left[0], precLow[0][i]);
            val0 = readers[where].readVal(br);
            left[0] = val0;
            low[0][i] = val0;
            where = decideSafe12(i, i2, 1, precInfo, info, curFil, curColFil, left[1], precLow[1][i]);
            val1 = readers[where].readVal(br);
            left[1] = val1;
            low[1][i] = val1;
            where = decideSafe12(i, i2, 2, precInfo, info, curFil, curColFil, left[2], precLow[2][i]);
            val2 = readers[where].readVal(br);
            left[2] = val2;
            low[2][i] = val2;
            val0 = (byte)(val0 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 0));
            val1 = (byte)(val1 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 1));
            val2 = (byte)(val2 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 2));
            low0p = low0;
            low1p = low1;
            low2p = low2;
            low0 = low0n;
            low1 = low1n;
            low2 = low2n;
            low0n = precLowF0[i + 1];
            low1n = precLowF1[i + 1];
            low2n = precLowF2[i + 1];
            leftF0 = (byte)(val0 + fil.filter(leftF0, low0, low0p, low0n));
            leftF1 = (byte)(val1 + fil.filter(leftF1, low1, low1p, low1n));
            leftF2 = (byte)(val2 + fil.filter(leftF2, low2, low2p, low2n));
            lowF0[i] = leftF0;
            lowF1[i] = leftF1;
            lowF2[i] = leftF2;
            out.writeTriplet(leftF0, leftF1, leftF2);

          //}
        }

        curFil = filterOfZone(width - 1, i2, width, filters);
        curColFil = colorFilterOfZone(width - 1, i2, width, colorFilters);
        fil = fg.getFilter(curFil);
        cfil = fg.getColFilter(curColFil);

        //for (int i3 = 0; i3 < 3; i3 ++) {              //     i == width - 1
          where = decide(width - 1, i2, 0, precInfo, info, curFil, curColFil, left[0], precLow[0][width - 1]);
          val0 = readers[where].readVal(br);
          left[0] = val0;
          low[0][width - 1] = val0;
          where = decide(width - 1, i2, 1, precInfo, info, curFil, curColFil, left[1], precLow[1][width - 1]);
          val1 = readers[where].readVal(br);
          left[1] = val1;
          low[1][width - 1] = val1;
          where = decide(width - 1, i2, 2, precInfo, info, curFil, curColFil, left[2], precLow[2][width - 1]);
          val2 = readers[where].readVal(br);
          left[2] = val2;
          low[2][width - 1] = val2;
          val0 = (byte)(val0 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 0));
          val1 = (byte)(val1 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 1));
          val2 = (byte)(val2 + cfil.colFilter((byte)val0, (byte)val1, (byte)val2, 2));
          val0 = (byte)(val0 + safeFilter(width - 1, i2, leftF0, precLowF0[width - 1], precLowF0[width - 2],
                                          (byte)0, curFil, fil, width));
          val1 = (byte)(val1 + safeFilter(width - 1, i2, leftF1, precLowF1[width - 1], precLowF1[width - 2],
                                          (byte)0, curFil, fil, width));
          val2 = (byte)(val2 + safeFilter(width - 1, i2, leftF2, precLowF2[width - 1], precLowF2[width - 2],
                                          (byte)0, curFil, fil, width));

          leftF0 = (byte)val0;
          leftF1 = (byte)val1;
          leftF2 = (byte)val2;
          lowF0[width - 1] = leftF0;
          lowF1[width - 1] = leftF1;
          lowF2[width - 1] = leftF2;
          out.writeTriplet(leftF0, leftF1, leftF2);

        //}

      }
      System.out.println("done. ");
      long finalTime = System.currentTimeMillis() - initTime;
      br.close();
      out.close();
      if (timeVerbose) { System.out.println("Decompression took " + (finalTime) + " ms."); }
    } catch (Exception e) { 
      System.err.println("Error caused decompression process to abort: " + e.getMessage());
    }
  }

  public static byte safeFilter(int x, int y, byte left, byte[] low, int curFil, filterer fil, int width) {
    byte ll = 0;
    byte lr = 0;
    if (x == 0) {
      ll = 0;
    } else {
      ll = low[x - 1];
    }
    if (x == width - 1) {
      lr = 0;
    } else {
      lr = low[x + 1];
    }
    return safeFilter(x, y, left, low[x], ll, lr, curFil, fil, width);
  }

  public static byte safeFilter(int x, int y, byte left, byte low, byte ll, byte lr, int curFil, filterer fil, int width) {
    byte res = 0;
    if (x > 0 && x < width - 1 && y == 0) {     // On the lower side
      if (curFil != 1) {
        res = left;
      } else {
        res = 0;
      }
    } else if (x == 0 && y > 0) {               // On the left side
      if (curFil != 1 && curFil != 4 && curFil != 7 && curFil != 11) {
        res = (curFil == 1) ? 0 : low;
      } else {
        res = fil.filter(left, low, ll, lr);
      }
    } else if (x == width - 1 && y > 0) {       // On the right side
      if (curFil == 7 || curFil == 10 || curFil == 11) {
        res = (curFil == 1) ? 0 : left;
      } else {
        res = fil.filter(left, low, ll, lr);
      }
    } else if (x == 0 && y == 0) {              // Lower left corner
        res = 0;
    } else if (x == width - 1 && y == 0) {      // Lower right corner
      if (curFil != 1) {
        res = left;
      } else {
        res = 0;
      }
    } else {                                    // Other locations
      res = fil.filter(left, low, ll, lr);
    }
    return res;
  }

  public static int subVersion() {
    return subVersion;
  }

  /**
   *  Compares two images, printing the first difference on standard output
   */
   
  public static void checkDiff(BmpImage im1, BmpImage im2) {
    byteMatrix one = im1.getImage();
    byteMatrix two = im2.getImage();
    boolean exitf = false;
    for (int y = 0; ! exitf && y < im1.getHeight(); y++) {
      for (int x = 0; ! exitf && x < im1.getWidth(); x++) {
        for (int c = 0; c < 3; c++) {
          if (one.getVal(x, y, c) != two.getVal(x, y, c)) {
            System.out.println("\n Difference in " + x + " " + y + " " + c);
            System.out.println(one.getVal(x, y, c) + " != " + two.getVal(x, y, c));
            exitf = true;
          }
        }
      }
    }
  }

  /**
   *  Gets the current subversion of the encoder
   *  @return The encoder subversion
   */

  public static int subversion() {
    return subVersion;
  }

  /**
   *  Gets the current version of the encoder
   *  @return The encoder version
   */
   
  public static int version() {
    return version;
  }

  /**
   *  Imposes that the comrpession and decompression procedures print an hash 
   *  code for the image
   *  @param True to print the hashcode during encoding or decoding
   */
   
  public static void setHashPrint(boolean hp) {
    hashPrint = hp;
  }

  /**
   *  Compares two files, printing on standard output their differences 
   *  @return True if the files are identical
   */
   
  public static boolean fileCompare(String file1, String file2) {
  	boolean res = true;
  	try {
  	   InputStream f1 = new BufferedInputStream(new FileInputStream(file1));
  	   InputStream f2 = new BufferedInputStream(new FileInputStream(file2));
  	   int readbyte1 = f1.read();
  	   int readbyte2 = f2.read();
  	   int pos = 0;
  	   while (readbyte1 != -1 || readbyte2 != -1) {
		 if (readbyte1 != readbyte2) {
		 	System.out.println("Different bytes in position " + pos + ": " +
		 	                    readbyte1 + " != " + readbyte2);
			res = false;		 	                    
		 }
  	   	 readbyte1 = f1.read();
  	     readbyte2 = f2.read();  	   	
  	     pos ++;
  	   }
    } catch (Exception e) { e.printStackTrace(); }
    return res;
  }
  
  public static void memoryReport() {
  	System.gc();
  	System.out.println("Free memory: " + Runtime.getRuntime().freeMemory() / 1024 + " KB");
  	System.out.println("Max  memory: " + Runtime.getRuntime().maxMemory() / 1024 + " KB");
  	System.out.println("Used memory: " + Runtime.getRuntime().totalMemory() / 1024 + " KB");
  	System.out.println();
  }
  
  /**
   * Tests compression and decompression on a specified BMP file. Creates
   * an output file named {inputfile}_java_out.bmp and compares it with the original.
   * @args The input file, without the .BMP extension, as the first and only argument
   */
  
  public static void main(String[] args) {
  	String image = "";
  	if (args.length > 0) {
  		image = args[0];
  	} else {
  		image = "../Images_java/lena";          // My test image
  		if (! (new File(image + ".bmp").exists())) {
  			image = "";
  			System.err.println("No image specified !");
  		}
  	}
    compress(image + ".bmp", image + ".bcif");
    
    //decompressBcifToStreamableViewer(image + ".bcif15");            // Alternative test 1
    
    decompressBcifToBmpFile(image + ".bcif", image + "_java_out.bmp");
    BmpImage newimage = new BmpImage(image + "_java_out.bmp");
    
    //BmpImage newimage = decompressBcifToBmpObj(image + ".bcif15");  // Alternative test 2
    //newimage.writeImage(image + "_java_out.bmp");
    
    BmpImage oldimage = new BmpImage(image + ".bmp");
    System.out.println("Initial has code : " + (oldimage.hashCode()));
    System.out.println("Final has code   : " + (newimage.hashCode()));
    	           
	//System.out.println();	      // Compare Bmp headers; fileCompare is anyway 
	//oldimage.printBmpInfo();	  // a stronger control         
	//newimage.printBmpInfo();
	
	if (fileCompare(image + ".bmp", image + "_java_out.bmp")) {
		System.out.println("Input and output files have been compared, and result identical.");
	} else {
		System.out.println("Differences found between input and output file.");		
	}
  }
}
