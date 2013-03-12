package bcif;

/**
 * <p> Class representing a reader from Huffman codes where the Huffman tree has one only
 * node. The only possible value is read without reading any bits from the input. </p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class oneValHuffmanReader implements huffmanReader {

  private int val = 0;

  public oneValHuffmanReader(int val) {
    this.val = val;
  }

  public int readVal(bitReader br) {
    return val;
  }

  public int readValnPrint(bitReader br) {
    return val;
  }

}