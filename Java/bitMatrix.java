package bcif;
import java.io.*;
/**
 * <p>The bitMatrix class represents a matrix of single bits. It can be used as a common
 * matrix or to read or write values sequencially; in this case a cursor keeps trace of where the
 * operations take place. The class contains optimized methods to write values with the minimum
 * usage of space. For reasons of efficence there are few controls on the input parameters, so methods
 * of this class must be used with care.</p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class bitMatrix {

  private long width = 0;
  private long height = 0;
  private byte[] matrix = {};
  private long count = 0;
  private long wroteBits = 0;
  private int lasty = -1;
  private int lastyres = -1;
  private int lastVal = -1;
  private int lastPoint = -1;

  /**
   * Creates a bitMatrix of determinated height and width
   * @param width The matrix' width
   * @param height The matrix' height
   */

  public bitMatrix(long width, long height) {
    this.width = width;
    this.height = height;
    matrix = new byte[(int)(((width + 1) * (height + 1)) >> 3) + 1];
  }

  /**
   * Reads a bitMatrix from an input stream
   * @param in The input stream where to read
   * @param numBytes The maximum number of bytes to read
   */

  public bitMatrix(InputStream in, int numBytes) {
    try {
      matrix = new byte[numBytes];
      width = in.read(matrix, 0, numBytes) << 3;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  /**
   * Creates a copy of an existing bitMatrix
   * @param source The bitMatrix to copy
   */

  public bitMatrix(bitMatrix source) {
    width = source.width;
    height = source.height;
    count = source.count;
    wroteBits = source.wroteBits;
    matrix = new byte[source.matrix.length];
    System.arraycopy(source.matrix, 0, matrix, 0, matrix.length);
  }

  /**
   * Creates a bitMatrix making the xor of two existing matrixes. The two sources must be the same size.
   * @param source The first bitMatrix source
   * @param source2 The second bitMatrix source
   */

  public bitMatrix(bitMatrix source, bitMatrix source2) {           // Xor delle due matrici
    width = source.width;
    height = source.height;
    count = source.count;
    wroteBits = source.wroteBits;
    matrix = new byte[source.matrix.length /*(int)((width + 1) * (height + 1) / 8) + 1*/];
    int minLen = Math.min(source.matrix.length, source.matrix.length);
    for (int i = 0; i < minLen; i++) {
      matrix[i] = (byte)(source.matrix[i] ^ source2.matrix[i]);
    }
  }


  private void check(int x, int y) {
    if (x > width || y > height || x < 0 || y < 0) {
      throw new RuntimeException("Index out of range: " + x + "," + y + " on " + width + "," + height);
    }
  }

  /**
   * Calculates an hash code of the object using every value in the matrix
   * @return The hash code
   */

  public int hashCode() {
    int res = 0;
    int mod = 1 << 30;
    int forlim = (int)(((width + 1) * (height + 1)) >> 3) + 1;
    for (int i = 0; i < forlim; i ++) {
      int v = matrix[i];
      v = v < 0 ? v + 256 : v;
      res = mod(res + (v << (i & 15)), 30) ;
    }
    return res;
  }


  /**
   * Sets a bit of the matrix to the specified value
   * @param x The orizzontal coordinate of the value
   * @param y The vertical coordinate of the value
   * @param val The new value of the bit. Admitted values are 0 and 1.
   */

  public void setBit(int x, int y, int val) {
    //check(x,y);
    if (y != lasty) { lasty = y; lastyres = (int)(width * y); }
      int insPoint = (lastyres + x) >> 3;
      int insOffset = (lastyres + x) & 7;
      if (val == 0) {
          matrix[insPoint] = (byte)(matrix[insPoint] & ((1 << insOffset) ^ 255));
      }
      else {
          matrix[insPoint] = (byte)(matrix[insPoint] | (1 << insOffset));
      }

  }


  /**
   * Sets a bit of the matrix to the specified value
   * @param x The orizzontal coordinate of the value
   * @param y The vertical coordinate of the value
   * @param val The new value of the bit. Admitted values are 0 and 1.
   */

  public void setBit(int x, int y, byte val) {
      //check(x,y);
      if (y != lasty) { lasty = y; lastyres = (int)(width * y); }
      int insPoint = (lastyres + x) >> 3;
      int insOffset = (lastyres + x) & 7;
      if (val == 0) {
          matrix[insPoint] = (byte)(matrix[insPoint] & ((1 << insOffset) ^ 255));
      }
      else {
          matrix[insPoint] = (byte)(matrix[insPoint] | (1 << insOffset));
      }

  }


  /**
   * Sets a line of bits of the matrix to the specified values. Used mainly for testing.
   * @param minx The orizzontal initial coordinate of the line
   * @param maxx The vertical final coordinate of the line
   * @param y The vertical coordinate of the line
   * @param val The new value of the bits. Admitted values are 0 and 1.
   */


  public void setLine(int minx, int maxx, int y, byte val) {
    for (int i = minx; i < maxx + 1; i++) {
      setBit(i, y, val);
    }
  }

  /**
   * Makes a xor of selected bit with input value
   * @param x The orizzontal initial coordinate of the line
   * @param y The verical initial coordinate of the line
   * @param val The second argument of the xor operation
   */

  public void xorBit(int x, int y, int val) {
    //check(x,y);
    if (val > 0) {
      if (y != lasty) {
        lasty = y;
        lastyres = (int) (width * y);
      }
      int insPoint = (lastyres + x) >> 3;
      int insOffset = (lastyres + x) & 7;
      matrix[insPoint] = (byte) (matrix[insPoint] ^ (1 << insOffset));
    }
  }

  /*public void xor(bitMatrix bm, int minx, int maxx, int miny, int maxy) {
    byte val = 0;
    for (int i = minx; i < maxx + 1; i ++) {
      for (int i2 = miny; i2 < maxy + 1; i2 ++) {
        xorBit(i, i2, bm.getBit(i, i2));
      }
    }
  }*/

  /**
   * Makes a xor of a rectangular portion of the bitMatrix with the same portion of another matrix. Limits
   * specified for the area are included in the wor.
   * @param bm The other bitMatrix
   * @param minx The left limit of the area
   * @param maxx The right limit of the area
   * @param miny The lower limit of the area
   * @param maxy The upper limit of the area
   */

  public void xor(bitMatrix bm, int minx, int maxx, int miny, int maxy) {
    byte val = 0;
    for (int i2 = miny; i2 < maxy + 1; i2 ++) {
      int i = minx;
      if (i2 != lasty) { lasty = i2; lastyres = (int)(width * i2); }
      long insPoint = (lastyres + minx) >> 3;
      long insOffset = (lastyres + minx) & 7;
      long finInsPoint = (lastyres + maxx) >> 3;
      long finInsOffset = (lastyres + maxx) & 7;
      int mxpu = maxx + 1;
      while (i < mxpu) {
        if ((insOffset & 7) == 0 && insPoint < finInsPoint) {
          matrix[ (int) insPoint] = (byte)(matrix[ (int) insPoint] ^ bm.matrix[ (int) insPoint]);
          i += 8;
          insPoint++;
        } else {
          insOffset++;
          xorBit(i, i2, bm.getBit(i, i2));
          i++;
          insPoint += insOffset >> 3;
          insOffset = insOffset & 7;
        }
      }
    }
  }


  /**
   * Makes a xor of the bitMatrix' values with another matrix
   * @param bm The other matrix
   */

  public void xor(bitMatrix bm) {
    byte val = 0;
    int forlim = (int)(((width + 1) * (height + 1)) >> 3) + 1;
    byte[] omatrix = bm.matrix;
    for (int i = 0; i < forlim; i ++) {
      matrix[i] = (byte)(matrix[i] ^ omatrix[i]);
    }
  }


  /**
   * Gets the number of equal bits of two bitMatrixes, or an approximation of this quantity, in a
   * given rectangular zone.
   * @param bm The other bitMatrix
   * @param minx The left limit of the area
   * @param maxx The right limit of the area
   * @param miny The lower limit of the area
   * @param maxy The upper limit of the area
   * @param p The step every witch a bit is compared. For ex. 2 means one bit every two will be compared
   * @return The number of equal bits in the area. If p is > 1, this number is an extimation
   */

  public int getEqBits(bitMatrix bm, int minx, int maxx, int miny, int maxy, int p) {
    int res = 0;
    int val0;
    int val1;
    for (int i2 = miny; i2 <= maxy; i2 ++) {
      for (int i = minx + mod((i2 - miny), 3); i <= maxx; i += p) {
        if (getBit(i, i2) == bm.getBit(i, i2)) { res ++; }
      }
    }
    return res * p;
  }

  /**
   * Gets the numbere of zero bits in the specified interval. To optimize operation time, a step may be used to
   * compare a pixel every n; in this case the result is an approximation.
   * @param minx The lower x limit of the interested area
   * @param maxx The upper x limit of the interested area
   * @param miny The lower y limit of the interested area
   * @param maxy The upper y limit of the interested area
   * @param p The used step
   * @return The number of bits equal to zero
   */

  public int getZeroBits(int minx, int maxx, int miny, int maxy, int p) {
    int res = 0;
    int val0;
    int val1;
    for (int i2 = miny; i2 <= maxy; i2 ++) {
      int forlim = 0;
      if (p > 1) { forlim = minx + mod((i2 - miny), 3); } else { forlim = minx; }
      for (int i = minx + mod((i2 - miny), 3); i <= maxx; i += p) {
        if (getBit(i, i2) == 0) { res ++; }
      }
    }
    return res * p;
  }


  /**
   * Return the requested bit
   * @param x The horizzontal coordinate of the interested bit
   * @param y The vertical coordinate of the interested bit
   * @return The value of the bit (0 or 1)
   */

  public byte getBit(int x, int y) {
      //check(x,y);
      if (lasty != y) {
        lasty = y;
        lastyres = (int)(y * width);
      }
      long insPoint = (lastyres + x) >> 3;
      long insOffset = (lastyres + x) & 7;
      lastPoint = (int)insPoint;
      lastVal = matrix[lastPoint];
      return (byte)((lastVal >>> insOffset) & 1);
  }

  /**
   * Returns a specified bit from a byte
   * @param source The byte from where to extract the bit
   * @param bit The bit to extract (0 is the less significative)
   * @return The extracted bit
   */

  public static byte exBit(int source, int bit) {
    if ((source & (1 << bit)) == 0) { return 0; } else { return 1; }
  }


  /**
   * Makes the unsigned module operation of a power of 2. In other words, extracts the less significative
   * bits of a number. Notice this is much more efficent than the % java operator
   * @param source The source number
   * @param bits The number of the bits to extract, or in other words log(2, module)
   * @return The result of the module
   */

  public static int mod(long source, int bits) {
    return (int)(source << (64 - bits) >>> (64 - bits));
  }

  /**
   * Makes the unsigned module operation of a power of 2. In other words, extracts the less significative
   * bits of a number. Notice this is much more efficent than the % java operator
   * @param source The source number
   * @param bits The number of the bits to extract, or in other words log(2, module)
   * @return The result of the module
   */

  public static int mod(int source, int bits) {
    return (source << (32 - bits) >>> (32 - bits));
  }

  /**
   * Makes the unsigned module operation of a power of 2. In other words, extracts the less significative
   * bits of a number. Notice this is much more efficent than the % java operator
   * @param source The source number
   * @param bits The number of the bits to extract, or in other words log(2, module)
   * @return The result of the module
   */

  public static int mod(byte source, int bits) {
    return (mod((int)(source >= 0 ? source : source + 256), bits));
  }

  /**
   * Returns a specified bit from a byte
   * @param source The byte from where to extract the bit
   * @param bit The bit to extract (0 is the less significative)
   * @return The extracted bit
   */

  public static byte exBit(byte source, byte bit) {
    if ((source & (1 << bit)) == 0) { return 0; } else { return 1; }
  }

  /**
   * The method return the number of one bits in the representation of the specified byte
   * @param source The target byte
   * @return The number of one bits
   */

  public static byte bitNum(byte source) {
    byte res = 0;
    for (int i = 0; i < 8; i++) {
      if ((source & 1) != 0) { res ++; }
      source = (byte)(source >>> 1);
    }
    return res;
  }

  /**
   * Inverts every value of the bitMatrix in a specified interval
   * @param minx The left bound of the interval
   * @param maxx The right bound of the interval
   * @param miny The lower bound of the interval
   * @param maxy The upper bound of the interval
   */

  public void invert(int minx, int maxx, int miny, int maxy) {
    byte val = 0;
    for (int i2 = miny; i2 < maxy + 1; i2 ++) {
      int i = minx;
      if (i2 != lasty) { lasty = i2; lastyres = (int)(width * i2); }
      long insPoint = (lastyres + minx) >> 3;
      long insOffset = (lastyres + minx) & 7;
      long finInsPoint = (lastyres + maxx) >> 3;
      long finInsOffset = (lastyres + maxx) & 7;
      int mxpu = maxx + 1;
      while (i < mxpu) {
        if ((insOffset & 7) == 0 && insPoint < finInsPoint) {
          matrix[ (int) insPoint] = (byte)(matrix[ (int) insPoint] ^ 255);
          i += 8;
          insPoint++;
        } else {
          insOffset++;
          xorBit(i, i2, 1);
          i++;
          insPoint += insOffset >> 3;
          insOffset = insOffset & 7;
        }
      }
    }
  }

  /**
   * Inverts every value of the bitMatrix
   */

  public void invert() {
    int r = (int)((width + 1) * (height + 1) / 8) + 1;
    for (int i = 0; i < r; i++) {
      matrix[i] = (byte)(255 ^ matrix[i]);
    }
  }

  /**
   * Writes sequancially a bitMatrix into another
   * @param bm The bitMatrix where to write
   */

  public void writeImage(bitMatrix bm) {
    for (int i = 0; i < width; i++) {
      for (int i2 = 0; i2 < height; i2 ++) {
        bm.writeBit(getBit(i, i2));
      }
    }
  }

  /**
   * Reads a bitMatrix from another
   * @param bm The bitMatrix where to read
   * @param width The width of the bitMatrix to read
   * @param height The width of the bitMatrix to read
   * @return The read bitMatrix
   */

  public static bitMatrix readImage(bitMatrix bm, long width, long height) {
    bitMatrix res = new bitMatrix(width, height);
    for (int i = 0; i < res.width; i++) {
      for (int i2 = 0; i2 < height; i2 ++) {
        res.setBit(i, i2, bm.readBit());
      }
    }
    return res;
  }

  /**
   * Writes a bitMatrix on the output stream from the beginning to the cursor.
   * @param out The output stream where to write
   * @return The number of wrote bytes
   */

  public int writeStream(OutputStream out) {
    int writeSize = 0;
    try {
     writeSize = (int)(count >> 3);
     if ((count & 7) > 0) { writeSize ++; }
     out.write(matrix, 0, writeSize);
   } catch (Exception e) { e.printStackTrace(); }
   return writeSize;
  }

  /**
   * Reads a value in a specified range sequancially
   * @param range The maximum value the value can have (minimum is 0)
   * @return The read value
   */

  public long read(int range) {
    return read((long)range);
  }

  /**
   * Reads a value in a specified range sequancially
   * @param range The maximum value the value can have (minimum is 0)
   * @return The read value
   */

  public long read(long range) {
    int res = 0;
    int exp = 0;
    long irange = range;
    while (range > 0 && irange - res >= 1 << exp) {
      res = res + (getBit((int)count,0) << exp);
      exp ++;
      range = range >> 1;
      count ++;
    }
    return res;
  }

  /**
   * Reads an array of values with an array of specified ranges, sequancially
   * @param range The maximum values the values can have (minimum is 0)
   * @return The read values
   */

  public int[] read(int[] range) {
    int[] res = new int[range.length];
    long trange = 1;
    long tnum = 0;
    for (int i = 0; i < range.length && range[i] > 0; i++) {
      trange = trange * (range[i] + 1);
    }
    tnum = read(trange);
    for (int i = 0; tnum > 0; i++) {
      res[i] = (int) (tnum % (range[i] + 1));
      tnum = tnum / (range[i] + 1);
    }
    return res;
  }

  /**
   * Writes an array of values with an array of specified ranges, sequancially
   * @param num The values to write
   * @param range The maximum values the values in num can have (minimum is 0)
   */

  public void write(int[] num, int[] range) {
    long tnum = num[0];
    long trange = (range[0] + 1);
    long mul = range[0];
    for (int i = 1; i < range.length && range[i] > 0; i++) {
      tnum = tnum + num[i] * trange;
      trange = trange * (range[i] + 1);
    }
    write (tnum,trange);
  }

  /**
   * Writes sequencially a number that could vary in a specified range.
   * @param num The number to write
   * @param range The maximum value the number could have (minumum is 0)
   */

  public void write(int num, int range) {
    write((long) num, (long) range);
  }

  /**
   * Writes sequencially a number that could vary in a specified range.
   * @param num The number to write
   * @param range The maximum value the number could have (minumum is 0)
   */

  public void write(long num, long range) {
    int bit = 0;
    int mul = 0;
    long rep = 0;
    long irange = range;
    while (range > 0 && irange - rep >= 1 << mul) {
      bit = (byte)(num & 1);
      setBit((int)count, 0, bit);
      count ++;
      rep = rep + (bit << mul);
      mul ++;
      num = num >> 1;
      range = range >> 1;
    }
  }

  /**
   * Writes a specified number using a certain number of bits, sequancially, from the less to the more
   * significative
   * @param num The number to write
   * @param bitNum The number of bits to use
   */

  public void fwrite(int num, int bitNum) {
    fwrite((long) num, bitNum);
  }

  /**
   * Writes a specified number using a certain number of bits, sequancially, from the less to the more
   * significative
   * @param num The number to write
   * @param bitNum The number of bits to use
   */

  public void fwrite(long num, int bitNum) {
    while (bitNum > 0) {
      setBit((int)count, 0, (int)num & 1);
      count ++;
      num = num >> 1;
      bitNum--;
    }
  }

  /**
   * Reads a number from a specified number of bits, considering the first bits less significative
   * @param bitNum The number of bits to consider
   * @return The read number
   */

  public long fread(int bitNum) {
    long res = 0;
    for (int i = 0; i < bitNum; i++) {
      res = res | (readBit() << i);
    }
    return res;
  }

    /**
     * Writes sequencially bits in the bitMatrix from an input string
     * @param bits A string of 0 and 1 characters specifying the bits to write
     */

  public void writeBits(String bits) {
    byte[] b = bits.getBytes();
    for (int i = 0; i < b.length; i++) {
      if (b[i] == 48) { writeBit(0); } else { writeBit(1); }
    }
  }

  /**
   * Writes sequencially a single bit
   * @param bit The bit to write (0 or 1)
   */

  public void writeBit(int bit) {
    setBit((int)count,0,bit);
    count ++;
  }

  /**
   * Reads sequencially a single bit
   * @return bit The read bit (0 or 1)
   */

  public byte readBit() {
    count ++;
    return getBit((int)(count - 1),0);
  }

  /**
   * Writes sequancially a value without specifing a format. Optimal when the distribution of values
   * is exponential (high values are exponecially less probable)
   * @param num The number to write
   */

  public void writeVbit(long num) {
    writeVbit(num, 0);
  }

  /**
   * Writes sequancially a value without specifing a format, but specifing a minimum number of bits.
   * necessary to write the number. Optimal when the distribution of values is exponential (high values
   * are exponecially less probable) and an inferior limit for values is known
   * @param num The number to write
   * @param initbits The minimum number of bits to use to write the number
   */

    public void writeVbit(long num, int initbits) {
	int bitNum = initbits;
	long repnum = (1 << bitNum) - 1;
	long precrep = -1;
	while (num > repnum) {
	    bitNum ++;
	    precrep = repnum;
	    repnum = repnum + (1 << bitNum);
	}
	for (int i = initbits; i < bitNum; i++) {
	    writeBit(1);
	}
	writeBit(0);
	fwrite(num - precrep - 1, bitNum);
    }

  /**
   * Reads a number written with the function writeVbit(initBits), sequancially
   * @param initBits The minimum number of bits used to write the number
   * @return The read value
   */

    public long readVbit(int initBits) {
		long res = 0;
		int bitNum = initBits;
		long precRep = -1;
		long rep = (1 << initBits) - 1;
		while (readBit() == 1) {bitNum ++; precRep = rep; rep = (rep + (1 << bitNum));}
		return precRep + 1 + fread(bitNum);
    }

    /*public long readVbit(int initBits) {
      int bitNum = 0;
      while (readBit() == 1) {bitNum ++;}
      long res = fread(initBits + bitNum);
      return res;
    }*/

  /**
   * Reads a number written with the function writeVbit(), sequancially
   * @return The read value
   */

  public long readVbit() {
    return readVbit(0);
  }

  /**
   * Generates a string describing the bits that have been written sequancially in the bitMatrix
   * @return The string representation of the interested portion of the bitMatrix
   */

  public String getString() {
    return getString(0, (int)(count - 1), 0, 0);
  }

  /**
   * Generates a string describing a rectangular portion of the bitMatrix
   * @param minx The left limit of the area
   * @param maxx The right limit of the area
   * @param miny The lower limit of the area
   * @param maxy The upper limit of the area
   * @return The string representation of the initereted area
   */

  public String getString(int minx, int maxx, int miny, int maxy) {
    String res = "";
    for (int i2 = maxy; i2 > miny - 1; i2 --) {
      for (int i = minx; i < maxx + 1; i++) {
        res += getBit(i, i2);
      }
      res += "\n";
    }
    return res;
  }

  /**
   * Brings the cursor to the beginning of the bitMatrix
   */

  public void reset() {
    count = 0;
    wroteBits = 0;
  }

  /**
   * Gets the position of the cursor. Generally, when the object is used for sequancial writing this
   * represents the actual size of what has been wrote
   * @return The cursor's position, in bits from the beginning
   */

  public long getSize() {
    return count;
  }

  /**
   * Moves the cursor to a specified position.
   * @param where The position where to move the cursor, in bits from the beginning
   */

  public void goTo(long where) {
    count = where;
    wroteBits = where;
  }

  /**
   * Returns the array of bytes representing the bit matrix. Used in the pcf alghoritm for optimization; writing
   * on this array as a stream may cause incoherence with the cursor: methods like getSize or writeStream may give
   * unpredictable response. Use only for reading or to modify existing data
   * @return The byte array representation of the bit matrix
   */

  public byte[] getMatrix() {
    return matrix;
  }

  /**
   * Sets the array of bytes representing the bit matrix. Changing this array may cause incoherence with the cursor:
   * methods like getSize or writeStream may give unpredictable response. To avoid this be sure to exchange the array
   * with another with same size
   * @param m The array to set
   */

  public void setMatrix(byte[] m) {
    matrix = m;
  }

  /**
   * Returns the matrix width
   * @return The matrix width
   */

  public long width() {
    return width;
  }

  /**
   * Returns the matrix height
   * @return The matrix height
   */

  public long height() {
    return height;
  }

  /**
   * Return the number of sequencially wrote bits since the last call of getWroteBits, goTo or reset.
   * Notice that calling this method twice returns always 0 at the second time.
   * @return The number of bits wrote
   */

  public long getWroteBits() {
   long temp = wroteBits;
   wroteBits = count;
   return count - temp;
  }

  /**
   * Return the number of sequencially wrote bits since the last call of getWroteBits, goTo or reset.
   * Notice that calling this method twice always return the same number.
   * @return The number of bits wrote
   */

  public long seeWroteBits() {
   return count - wroteBits;
  }

  /**
   * Rounds the cursor to the closest byte after it. Usefull when handling streams.
   */

  public void roundToByte() {
    if (count % 8 != 0) {
      count += 8 - count % 8;
    }
  }

  /**
   * Calculates the integer log2 of the argument, rounded down. Notice it is much faster than the call to
   * Math.log
   * @param arg The argument of the logarithm
   * @return The result of the logarithm
   */

  public static int log2(int arg) {
    int res = 0;
    while (arg > 1) { arg = arg >> 1; res ++; }
    return res;
  }

  /**
   * Creates an huffman tree for this matrix based on the probability of a zero. Not used in pcf compression, may be
   * used for a fast general-purpose compression.
   * @param znum The zero probablilty.
   * @return An huffman tree for this matrix
   */

  public static HTree htFromProbs(float znum) {
    float prob[] = getProbs(znum);
    int[] iProbs = new int[9];
    for (int i = 0; i < 9; i++) {
      iProbs[i] = Math.round(prob[i] * 1000);
    }
    int[] vals = new int[256];
    int[] freqs = new int[256];
    for (int i = 0; i < 256; i++) {
      vals[i] = i;
      freqs[i] = iProbs[bitNum((byte)i)];
    }
    HTree ht = HTree.buildHTree(freqs);
    return ht;
  }

  private static float[] getProbs (float znum) {
    float[] prob = new float[9];
    for (int i = 0; i < 9; i++) {
      prob[i] = 1;
      for (int i2 = 0; i2 < i; i2++) {
        prob[i] = prob[i] * (1 - znum);
      }
      for (int i2 = 0; i2 < 8 - i; i2++) {
        prob[i] = prob[i] * znum;
      }
    }
    return prob;
  }

  private static int[][] bestCoding(float znum) {
    int[][] res = new int[2][256];
    float[] prob = getProbs(znum);
    int[] ottimalBit = new int[9];
    int[] ottimal = new int[256];
    for (int i = 0; i < 9; i++) {
      ottimalBit[i] = (int)Math.round(-Math.log(prob[i]) / Math.log(2));
    }
    for (int i = 0; i < 256; i++) {
      ottimal[i] = ottimalBit[bitNum((byte)i)];
    }
    for (int i = 0; i < 256; i ++) {
      System.out.println(i + " " + ottimal[i]);
    }
    return res;
  }

  /**
   * Tests some functionalities of the class
   * @param args Not used
   */

  public static void main(String[] args) {
    bitMatrix bm = new bitMatrix(8,8);
    bitMatrix bm2 = new bitMatrix(8,8);
    for (int i3 = 0; i3 < 2; i3++) {
      for (int i2 = 0; i2 < 3; i2++) {
        for (int i = 0; i < 2; i++) {
          bm.write(new int[] {i, i2, i3}, new int[] {1, 2, 1});
          System.out.print("(" + i + "," + i2 + "," + i3 + ")" +
                           "   &    " + (i * 6 + i2 * 2 + i3) + "    &    "
                           + bm.getString());
          bm.reset();
        }
      }
    }
  }
}


