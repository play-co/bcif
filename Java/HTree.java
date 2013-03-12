package bcif;

/**
 * <p>The HTree class creates, reads and writes Huffman trees that treat integer values.</p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class HTree {

  private HTree left = null;
  private HTree right = null;
  private int val = 0;
  private int freq = 0;
  private int keyN = 1;
  private int[] codes = null;
  private int[] codeBits = null;
  private boolean createdByBits = false;
  private int lkey[] = null;
  private int lleft[] = null;
  private int lright[] = null;
  
  public void createLookup() {
  	lleft = new int[1024];
  	lright = new int[1024];
  	lkey = new int[1024];
  	recCreateLookup(lkey, lleft, lright, 0);
  }
  
  public int[] recCreateLookup(int[] k, int[] l, int[] r, int free) {  	
  	int myKey = free;
  	free ++;
  	if (left == null) {  		
  		k[myKey] = val;
  	} else {
  		k[myKey] = -1;
  		int[] res = left.recCreateLookup(k, l, r, free);
  		l[myKey] = res[0];
  		free = res[1];
  		res = right.recCreateLookup(k, l, r, free);
  		r[myKey] = res[0];
  		free = res[1];
  	}
  	return new int[] {myKey, free};
  }

  public int readEfVal(bitReader bm) {
  	int cur = 0;
  	while (lkey[cur] == -1) {
  		if (bm.readBit() == 0) {
  			cur = lleft[cur];
  		} else {
  			cur = lright[cur];
  		}
  	}
  	return lkey[cur];
  }
  
  /**
   * Creates an Huffman tree with specified left and right sons
   * @param l The left son
   * @param r The right son
   */

  public HTree(HTree l, HTree r) {
    left = l;
    right = r;
    freq = l.freq + r.freq;
    keyN = l.keyN + r.keyN;
  }

  /**
   * Creates an Huffman tree as a leaf specifing the contained value and its frequency
   * @param v The value in the leaf
   * @param f The frequency of that value
   */

  public HTree(int v, int f) {
    val = v;
    freq = f;
  }

  /**
   * Creates an optimal Huffman tree from an array of given values and the relative frequencies. To get
   * the optimal coding use then getCodes()
   * @param f The frequences of the values
   * @return The optimal huffman tree
   */

  public static HTree buildHTree (int[] f) {
    return buildHTree(f, 0);
  }

  public static int[] readBits(bitReader br, int valNum) {
    int[] res = new int[valNum];
    int prec = 0;
    boolean exitf = false;
    int cur = 0;
    int max = br.readVbit(3);
    for (int i = 0; i < valNum; i++) {
      if (! exitf) {
        cur = br.readVbit();
        if (cur > 0 && br.readBit() == 1) { cur = - cur; }
        cur = prec + cur;
        if (cur > max) { res[i] = -1; } else { res[i] = cur; }
        prec =  cur;
      } else {
        res[i] = -1;
      }
      if (cur == 0) {
        exitf = true;
      }
    }
    return res;
  }

  public void writeBits(bitWriter bw) {
    int prec = 0;
    int min = 0;
    boolean exitf = false;
    int max = -1;
    for (int i = 0; i < codeBits.length & ! exitf; i++) {
      if (codeBits[i] > max) { max = codeBits[i]; }
    }
    bw.writeVbit(max, 3);
    for (int i = 0; i < codeBits.length & ! exitf; i++) {
      int cur = codeBits[i];
      if (cur == 0) { exitf = true; } else if (cur == -1) { cur = max + 1; }
      int diff = cur - prec;
      if (diff < 0) { diff = - diff; min = 1; } else { min = 0; }
      bw.writeVbit(diff);
      if (diff > 0) { bw.writeBit(min); }
      prec = cur;
    }
  }

  public static HTree readHTreeFromBits(bitReader br, int valNum) {
    int[] b = readBits(br, valNum);
    return buildHTreeFromBits(b);
  }

  public void writeHTreeFromBits(bitWriter bw) {
    if (! createdByBits) { throw new RuntimeException("Huffman tree not created by bits of values"); } else {
      writeBits(bw);
    }
  }

  public static HTree buildHTreeFromBits (int[] codeBits) {
    int n = codeBits.length;
    HTree[] hts = new HTree[n];
    int[] b = new int[n];
    int treeNum = 0;
    int maxBits = 0;
    boolean exitf = false;
    for (int i = 0; i < n & ! exitf; i++) {
      if (codeBits[i] > 0) {
        hts[i] = new HTree(i, 1 << codeBits[i]);
        treeNum ++;
        if (codeBits[i] > maxBits) { maxBits = codeBits[i]; }
      } else if (codeBits[i] == 0) {
        exitf = true;
        hts[0] = new HTree(i, 1);
      }
      b[i] = codeBits[i];
    }
    if (exitf) {
      hts[0].codeBits = codeBits;
      hts[0].keyN = n;
      hts[0].createdByBits = true;
      return hts[0];
    } else {
      for (int i = maxBits; i >= 0 && treeNum > 0; i--) {
        HTree first = null;
        int firstInd = 0;
        for (int i2 = 0; i2 < n; i2 ++) {
          if (hts[i2] != null & b[i2] == i) {
            if (first == null) {
              first = hts[i2];
              firstInd = i2;
            } else {
              hts[firstInd] = new HTree(first, hts[i2]);
              hts[i2] = null;
              b[firstInd] --;
              b[i2] = -1;
              first = null;
            }
          }
        }
      }
    }
    int firstTree = 0;
    if (treeNum == 0) {
      hts[0] = new HTree(0,1);
    }
    while (hts[firstTree] == null) { firstTree ++; }
    hts[firstTree].codeBits = codeBits;
    hts[firstTree].keyN = n;
    hts[firstTree].createdByBits = true;
    return hts[firstTree];
  }

  /**
   * Creates an optimal Huffman tree from an array of given values and the relative frequencies. To get
   * the optimal coding use then getCodes(). This method also imposes a minimum frequency to consider
   * for simbols with frequency equal to zero
   * @param f The frequences of the values
   * @param minFreq The minimum frequency
   * @return The optimal huffman tree
   */

  public static HTree buildHTree (int[] f, int minFreq) {
    int n = f.length;
    int[] v = new int[n];
    for (int i = 1; i < n; i++) {
      v[i] = i;
    }
    HTree[] res = new HTree[v.length];
    for (int i = 0; i < v.length; i++) {
      if (f[i] < minFreq) { f[i] = minFreq; }
      res[i] = new HTree(v[i], f[i]);
    }
    int min1 = 0;
    int min2 = 0;
    int minInd1 = 0;
    int minInd2 = 0;
    int mergeInd = 0;
    int lastInd = 0;
    for (int i = v.length - 1; i > 0; i--) {
      if (res[0].freq < res[1].freq) {
        min1 = res[0].freq;
        min2 = res[1].freq;
        minInd1 = 0;
        minInd2 = 1;
      } else {
        min1 = res[1].freq;
        min2 = res[0].freq;
        minInd1 = 1;
        minInd2 = 0;
      }
      for (int i2 = 2; i2 < i + 1; i2 ++) {
        if (res[i2].freq < min1) {
          min2 = min1;
          minInd2 = minInd1;
          min1 = res[i2].freq;
          minInd1 = i2;
        } else if (res[i2].freq < min2) {
          min2 = res[i2].freq;
          minInd2 = i2;
        }
      }
      mergeInd = Math.min(minInd1, minInd2);
      lastInd = Math.max(minInd1, minInd2);
      res[mergeInd] = new HTree(res[minInd1], res[minInd2]);
      res[lastInd] = res[i];
    }
    res[0].keyN = n;
    return res[0];
  }

  /**
   * Creates a string describing the Huffman tree. The first value of each line represents the value,
   * the second the coded value and the third the number of bits to use to write the value.
   * @return The string describing the tree
   */

  public String getString() {
    String res = "";
    if (codes == null) {getCodes();}
    for (int i = 0; i < codes.length; i++) {
      res += i + " " + codes[i] + " " + codeBits[i] + "\n";
    }
    return res;
  }

  /**
   * Creates a string describing the Huffman tree. The first value of each line represents the value,
   * the second the coded value and the third the number of bits to use to write the value. Presumes that
   * the tree has been done for values in couples, and the integer used has been generated as
   * <I> (value1 << maxValue1Bits) + value2 </I>
   * @param indBits The maximum number of bits of the first value
   * @return The string describing the tree
   */

  public String getString(int indBits) {
    String res = "";
    int mod = (1 << indBits) - 1;
    if (codes == null) {getCodes();}
    for (int i = 0; i < codes.length; i++) {
      if (codeBits[i] > 0) {
        res += "(" + (i >> indBits) + "," +
            (i & mod) + ") ";
        res += codes[i] + " " + codeBits[i] + "\n";
      }
    }
    return res;
  }

  /**
   * Returns an array representing the optimal coding for the values of the tree. The values [0][0..n-1]
   * represent the original coding, the values [1][0..n-1] represent the new coding, and the values[2][0..n-1]
   * are the number of bits to use for each simbol
   * @return The optimal codes
   */

  public int[] getCodes() {
    if (codes == null) {
      codes = new int[keyN];
      codeBits = new int[keyN];
      for (int i = 0; i < keyN; i++) {
        codeBits[i] = -1;
      }
      fillCodes(codes, codeBits, 0, 0);
    }
    return codes;
  }

  public int[] getCodeBits() {
    if (codes == null) {
      codes = new int[keyN];
      codeBits = new int[keyN];
      for (int i = 0; i < keyN; i++) {
        codeBits[i] = -1;
      }
      fillCodes(codes, codeBits, 0, 0);
    }
    return codeBits;
  }

  private void fillCodes(int[] rc, int[] rcb, int bit, int pVal) {
    if (left == null) {
      rc[val] = pVal;
      rcb[val] = bit;
    } else {
      left.fillCodes(rc, rcb, bit + 1, pVal);
      right.fillCodes(rc, rcb, bit + 1, pVal + (1 << bit));
    }
  }

  /**
   * Writes sequancially a value on a kjkhlkjhkjh optimally following the Huffman tree coding
   * @param bm The bitMatrix where to write
   * @param val The value to write
   */

  public void writeVal(bitWriter bm, int val) {
    if (codes == null) {getCodes();}
    bm.fwrite(codes[val], codeBits[val]);
  }

  /**
   * Reads sequencially a value from a biytReader following the Huffman tree coding
   * @param bm The sdfgfsgs where to read
   * @return val The read value
   */

  public int readVal(bitReader bm) {
    if (left == null) { return val; } else {
      if (bm.readBit() == 0) {
        return left.readVal(bm);
      } else {
        return right.readVal(bm);
      }
    }
  }

  /**
   * Cuts the branches of the tree that have frequency equal to zero. Optimizes tree space, but if an attampt to
   * write one of these values will be done, this will generate unpredictable results an coding will be compromised.
   */

  public void cut() {
    codes = null;
    while (left != null && (left.freq == 0 || right.freq == 0)) {
      if (left.freq == 0) {
        this.freq = right.freq;
        this.val = right.val;
        this.left = right.left;
        this.right = right.right;
      } else if (right.freq == 0) {
        this.freq = left.freq;
        this.val = left.val;
        this.left = left.left;
        this.right = left.right;
      }
    }
    if (left != null) { left.cut(); }
    if (right != null) { right.cut(); }
  }

  /**
   * Writes the Huffman tree itself onto a kjsdhf. The number of bits used to write the values must be
   * specified. Regarding the dkfgndfks, sequancial access is used
   * @param bm The dfgsd where to write
   * @param bitNum The number of bits to write the original values
   */

  public void write(bitWriter bm, int bitNum) {
    if ( left == null ) {
      bm.writeBit(1);
      bm.fwrite(val, bitNum);
    } else {
      bm.writeBit(0);
      left.write(bm, bitNum);
      right.write(bm, bitNum);
    }
  }

  /**
   * Reads an Huffman tree from a dfgfdsgds. The number of bits that have been used to write the values
   * must be specified. Regarding the jhbjhbjhb, sequancial access is used
   * @param bm The kjnkjnj from where to read
   * @param bitNum The number of bits to write the original values
   * @return The Huffman tree read
   */

  public static HTree read(bitReader bm, int bitNum) {
    HTree res = null;
    if (bm.readBit() == 1) {
      res = new HTree((int)bm.fread(bitNum), 0);
    } else {
      res = new HTree(read(bm, bitNum), read(bm, bitNum));
    }
    return res;
  }

  public int getVal() {
    return val;
  }

  public boolean leaf() {
    return (left == null & right == null);
  }
}


