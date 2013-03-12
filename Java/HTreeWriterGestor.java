package bcif;

import java.util.*;

/**
 * <p> Class that handles the various Huffman trees used during decoding.</p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class HTreeWriterGestor {

  private int f[] = null;
  private int faz[] = null;
  private int tot = 0;
  private int zeros = 0;
  private int curZeros = 0;
  private Stack<Integer> zs = null;
  private int zsp = 0;
  private int simbolNum = 256;
  private int zeroSeqNum = 128;
  private int zeroPercLim = 30;
  private int keyNum = 0;
  private HTree ht = null;
  private HTree aux = null;
  private HTree curht = null;
  private bitWriter bw = null;
  private boolean after = false;
  private boolean afterZero = false;

  public HTreeWriterGestor() {
    f = new int[simbolNum + zeroSeqNum];
    faz = new int[simbolNum];
    zs = new Stack<Integer>();
  }

  public void putVal(int v) {
    tot ++;
    if (v == 0) {
      zeros ++;
      curZeros ++;
    } else {
      if (curZeros > 0) {
        zs.push(curZeros);
        if (curZeros >= zeroSeqNum) {
          f[simbolNum + zeroSeqNum - 1] ++;
        } else {
          f[simbolNum + curZeros - 1] ++;
          afterZero = true;
        }
        curZeros = 0;
      }
      if (! afterZero) {
        f[v]++;
      } else {
        faz[v] ++;
        afterZero = false;
      }
    }
  }

  public void endGathering() {
    if (curZeros > 0) {
      zs.push(curZeros);
      if (curZeros == 0) { f[0] ++; } else {
        if (curZeros >= zeroSeqNum) {
          f[simbolNum + zeroSeqNum - 1] ++;
        } else {
          f[simbolNum + curZeros - 1] ++;
        }
      }
      curZeros = 0;
    }
    int zeroPerc = 0;
    if (tot > 0) { zeroPerc = zeros * 100 / tot; }
    if (zeros < tot && (zeroPerc > zeroPercLim)) {
      keyNum = simbolNum + zeroSeqNum;
      aux = HTree.buildHTree(faz);
      aux.cut();
      aux = HTree.buildHTreeFromBits(aux.getCodeBits());
    } else {
      keyNum = simbolNum;
      int[] nf = new int[256];
      nf[0] = zeros;
      for (int i = 1; i < 256; i ++) {
        nf[i] = f[i] + faz[i];
      }
      f = nf;
    }
    ht = HTree.buildHTree(f);
    ht.cut();
    ht = HTree.buildHTreeFromBits(ht.getCodeBits());
    curht = ht;
  }

  public static void report(int[] f, int inittot, int zeros) {
    int tot = 0;
    for (int i = 0; i < f.length; i++) {
      tot += f[i];
    }
    double ent = entropy(f);
    double hent = HuffmanEntropy(f);
    System.out.println("Report ------------------------");
    System.out.println("Simbol num is " + tot);
    System.out.println("Entropy is " + ent);
    System.out.println("Huffman entropy is " + hent);
    System.out.print("Entropy limit is ");
    int el = (int)(ent * tot);
    if (el > 1024) {
      System.out.println((el >> 13) + " KB");
    } else {
      System.out.println((el >> 3) + " bytes");
    }
    System.out.print("Huffman entropy limit is ");
    int elh = (int)(hent * tot);
    if (elh > 1024) {
      System.out.println((elh >> 13) + " KB");
    } else {
      System.out.println((elh >> 3)+ " bytes");
    }
    System.out.println("Zeros are " + zeros * 100 / (inittot + 1) + " %");
  }

  public static double entropy(int[] simbolsFreq) {
    double e = 0;
    int tot = 0;
    double res = 0;
    for (int i = 0; i < simbolsFreq.length; i ++) {
      tot += simbolsFreq[i];
    }
    double[] relFreq = new double[simbolsFreq.length];
    for (int i = 0; i < simbolsFreq.length; i++) {
      relFreq[i] = (double)simbolsFreq[i] / tot;
    }
    for (int i = 0; i < relFreq.length; i ++) {
      if (relFreq[i] > 0) {
        e -= relFreq[i] * (Math.log(relFreq[i]) / Math.log(2));
      }
    }
    return e;
  }

  public static double HuffmanEntropy(int[] simbolsFreq) {
    int[] s = new int[simbolsFreq.length];
    for (int i = 0; i < simbolsFreq.length; i++) {
      s[i] = i;
    }
    HTree ht = HTree.buildHTree(simbolsFreq);
    int[] cod = ht.getCodeBits();
    long totBits = 0;
    long totVals = 0;
    for (int i = 0; i < simbolsFreq.length; i++) {
      totVals += simbolsFreq[i];
      totBits += simbolsFreq[i] * cod[i];
    }
    if (totVals == 0) { return 0; } else {
      return (double) totBits / totVals;
    }
  }

  public static int log2(int arg) {
    int res = 0;
    while (arg > 1) { arg = arg >> 1; res ++; }
    return res;
  }

  public void setBitWriter(bitWriter bw) {
    this.bw = bw;
  }

  public void writeTree() {
    bw.fwrite(log2(keyNum - simbolNum), 8);
    ht.writeHTreeFromBits(bw);
    if (log2(keyNum - simbolNum) > 1) {
      aux.writeHTreeFromBits(bw);
    }
  }

  public HTree getTree() {
    return ht;
  }

  public void writeVal(int v) {
    if (keyNum == simbolNum) {
      ht.writeVal(bw, v);
    } else {
      if (curZeros > 0) {
        curZeros --;
      } else {
        if (v == 0) {
          curZeros = zs.get(zsp);
          zsp ++;
          if (curZeros < zeroSeqNum) {
            ht.writeVal(bw, curZeros + simbolNum - 1);
            curht = aux;
          } else {
            ht.writeVal(bw, zeroSeqNum + simbolNum - 1);
            bw.writeVbit(curZeros - zeroSeqNum, 8);
          }
          curZeros --;
        } else {
          curht.writeVal(bw, v);
          curht = ht;
        }
      }
    }
  }
}
