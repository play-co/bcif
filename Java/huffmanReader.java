package bcif;

/**
 * <p> Interface representing a reader of values encoded through Huffman trees.</p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public interface huffmanReader {

  public int readVal(bitReader br);

  public int readValnPrint(bitReader br);

}