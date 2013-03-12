package bcif;

/**
 * <p> Class for the reading of values encoded through a simple Huffman tree. </p>
 * @author Stefano Brocchi
 * @version 1.0
 */

public class standardHuffmanReader implements huffmanReader {

  private HTree ht = null;

  public standardHuffmanReader(HTree tree) {
    ht = tree;
    tree.createLookup();
  }

  public int readVal(bitReader br) {
    return ht.readEfVal(br);
  }

  public int readValnPrint(bitReader br) {
    return ht.readEfVal(br);
  }

}