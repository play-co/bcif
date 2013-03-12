package bcif;

/**
 * <p> The class represents a three dimensional byte matrix. Some methods are offered to permit
 * a sequencial visit and gain in efficency. </p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class byteMatrix {

  private byte [] matrix = null;
  private int width = 0;
  private int height = 0;
  private int depth = 0;
  private int dpw = 0;
  private int cur = 0;

    /**
     * The method creates a new byte matrix with the specified dimensions
     * @param width The width of the matrix
     * @param height The height of the matrix
     * @param depth The depth of the matrix
     */

  public byteMatrix(int width, int height, int depth) {
    this.width = width;
    this.height = height;
    this.depth = depth;
    dpw = depth * width;
    matrix = new byte[width * height * depth];
  }

    /**
     * Gets a specified value of the matrix
     * @param w The width of the value to extract
     * @param h The height of the value to extract
     * @param d The depth of the value to extract
     * @return The requested value
     */

  public byte getVal(int w, int h, int d) {
    try {
      return matrix[h * dpw + w * depth + d];
    }
    catch (ArrayIndexOutOfBoundsException ex) {
      System.out.println(w + " " + h + " " + d);
      throw ex;
    }
  }

    /**
     * Gets a specified value of the matrix as an integer
     * @param w The width of the value to extract
     * @param h The height of the value to extract
     * @param d The depth of the value to extract
     * @return The requested value
     */

  public int getIntVal(int w, int h, int d) {
    int val = matrix[h * dpw + w * depth + d];
    return (val < 0 ? val + 256 : val);
  }

    /**
     * Sets a specified value of the matrix converting the given int to a byte
     * @param w The width of the value to set
     * @param h The height of the value to set
     * @param d The depth of the value to set
     * @param val The value to set
     */

  public void setVal(int w, int h, int d, int val) {
    matrix[h * dpw + w * depth + d] = (byte)val;
  }

    /**
     * Sets a specified value of the matrix
     * @param w The width of the value to set
     * @param h The height of the value to set
     * @param d The depth of the value to set
     * @param val The value to set
     */

  public void setVal(int w, int h, int d, byte val) {
    matrix[h * dpw + w * depth + d] = val;
  }

    /**
     * Sums a value to a specified byte of the matrix in mod 256
     * @param w The width of the value to change
     * @param h The height of the value to change
     * @param d The depth of the value to change
     * @param val The value to sum
     */

  public void sumVal(int w, int h, int d, int val) {
    int insp = h * dpw + w * depth + d;
    int cval = matrix[insp];
    matrix[insp] = (byte)(cval + val);
  }

    /**
     * Sums a value to the current matrix byte
     * @param val The value to sum
     */

  public void sumCurVal(int val) {
    matrix[cur] = (byte)(matrix[cur] + val);
  }

    /**
     * Sums a value to the byte of a specified distance from the current
     * @param val The value to sum
     * @param n The difference of the position of the value to modify
     * and the current byte
     */

  public void sumCurVal(int val, int n) {
    matrix[cur + n] = (byte)(matrix[cur + n] + val);
  }

    /**
     * Sets the cursor to a specified point
     * @param w The width of the new point of the cursor
     * @param h The height of the new point of the cursor
     * @param d The depth of the new point of the cursor
     */

  public void setPoint(int w, int h, int d) {
    cur = h * dpw + w * depth + d;
  }

    /**
     * Returns the value of the current point of the matrix
     * @return TZhe current value of the byte
     */

  public int getCurVal() {
    return matrix[cur];
  }
  
    /**
     * Returns the value of the current point of the matrix
     * @return TZhe current value of the byte
     */

  public byte getByteCurVal() {
    return matrix[cur];
  }  

  public byte getByteCurVal(int i) {
    return matrix[cur + i];
  }  
  
  public byte getNextByte() {
    return matrix[cur++];
  }  
  
  
    /**
     * Transforms a byte to an unsigned representation
     * @param b The byte to transform
     * @return The integer unsigned representation
     */

  public int toInt(byte b) {
    return b < 0 ? b + 256 : b;
  }

    /**
     * Returns the value immediately under the current one
     * @return The value immediately under the current one
     */

  public byte getLowVal() {
    return matrix[cur - dpw];
  }

    /**
     * Returns the value immediately at the left of the current one
     * @return The value immediately at the left of the current one
     */

  public byte getLeftVal() {
    return matrix[cur - depth];
  }

    /**
     * Returns the value immediately at the left of and under the current one
     * @return The value immediately at the left of and under the current one
     */

  public byte getLowLeftVal() {
    return matrix[cur - depth - dpw];
  }

  public byte getRightDownVal() {
    return matrix[cur + depth - dpw];
  }

    /**
     * Returns the value immediately at the left of and over the current one
     * @return The value immediately at the left of and over the current one
     */

  public byte getUpLeftVal() {
    return matrix[cur - depth + dpw];
  }

    /**
     * Gets the current value represented as a positive integer
     * @return The current value represented as a positive integer
     */

  public int getIntCurVal() {
    int val = matrix[cur];
    return (val >= 0 ? val : val + 256);
  }

    /**
     * Gets a value with a specified distance from the current
     * @param n The difference between the position of
     * the desired value and the current
     * @return The requested value
     */

  public int getCurVal(int n) {
    return matrix[cur + n];
  }

    /**
     * Sets the current byte to a specified value
     * @param v The new value of the byte
     */

  public void setCurVal(byte v) {
    matrix[cur] = v;
  }

    /**
     * Sets a byte with a specified distance from the current to a specified value
     * @param v The new value of the byte
     * @param n The difference between the point to change and the current
     */

  public void setCurVal(byte v, int n) {
    matrix[cur + n] = v;
  }

    /**
     * Advances the cursor of one position
     */

  public void nextVal() {
    cur ++;
  }

    /**
     * Takes the cursor back of one position
     */

  public void precVal() {
    cur --;
    //throw new RuntimeException();
  }

    /**
     * Advances the cursor of n positions
     * @param i The number of positions to advance
     */

  public void nextVal(int i) {
    cur += i;
  }

    /**
     * Takes the cursor back of n positions
     * @param i The number of positions to take back the cursor
     */

  public void precVal(int i) {
    cur -= i;
  }

    /**
     * Takes the cursor to the firsr position of the matrix
     */

  public void firstVal() {
    cur = 0;
  }

    /**
     * Takes the cursor to the last position of the matrix
     */

  public void lastVal() {
    cur = matrix.length - 1;
  }

    /**
     * Returns the matrix' width
     * @return The matrix' width
     */

  public int width() {
    return width;
  }

    /**
     * Returns the matrix' height
     * @return The matrix' height
     */

  public int height() {
    return height;
  }

    /**
     * Returns the matrix' depth
     * @return The matrix' depth
     */

  public int depth() {
    return depth;
  }

    /**
     * Makes a little test on the correctness o the class
     * @param args Not used
     */

  public static void main(String[] args) {
    byteMatrix bym = new byteMatrix(10, 12, 1);
    int v = 0;
    for (int i = 0; i < 10; i ++) {
      for (int i2 = 0; i2 < 12; i2 ++) {
        for (int i3 = 0; i3 < 1; i3 ++) {
          bym.setVal(i, i2, i3, v);
          v++;
          System.out.println(bym.getIntVal(i, i2, i3));
        }
      }
    }
    bym.setPoint(5 , 4, 0);
    System.out.println(bym.getCurVal());
  }
}

