package bcif;

/**
 * <p> Class to read comrpessed values encoded through two Huffman trees and where sequences
 * of 0s are run length encoded. </p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class zeroHuffmanReader implements huffmanReader {

  private HTree ht = null;
  private HTree aux = null;
  private HTree curht = null;
  private int zeros = 0;
  private int maxZeroSeq = 128;
  private boolean after = false;

  public zeroHuffmanReader(HTree tree, HTree aux, int mzs) {
    ht = tree;
    this.aux = aux;
    maxZeroSeq = mzs;
    curht = ht;
    ht.createLookup();
    aux.createLookup();
  }

  public int readVal(bitReader br) {
    if (zeros > 0) {
      zeros --;
      return 0;
    } else {
      int cur = curht.readEfVal(br);
      curht = ht;
      if (cur < 256) {
        return cur;
      } else {
        zeros = cur - 255;
        int vgt = 0;
        if (zeros == maxZeroSeq) {
          vgt = br.readVbit(8);
          zeros += vgt;
        } else {
          curht = aux; 
        }
        zeros --;
        after = true;
        return 0;
      }
    }
  }

  public int readValnPrint(bitReader br) {
    return readVal(br);
  }

}
