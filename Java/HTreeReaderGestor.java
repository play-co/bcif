package bcif;

/**
 * <p> Class that handles the various Huffman trees used during encoding.</p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public class HTreeReaderGestor {

  private huffmanReader[] readers = null;

  public HTreeReaderGestor(int hr, bitReader br) {
    readers = new huffmanReader[hr];
    for (int i = 0; i < hr; i ++) {
      int zseq = (1 << br.fread(8));
      if (zseq > 1) {
        HTree ht = HTree.readHTreeFromBits(br, 256 + zseq);
        HTree aux = HTree.readHTreeFromBits(br, 256);
        readers[i] = new zeroHuffmanReader(ht, aux, zseq);
      } else {
        HTree ht = HTree.readHTreeFromBits(br, 256);
        if (ht.leaf()) {
          readers[i] = new oneValHuffmanReader(ht.getVal());
        } else {
          readers[i] = new standardHuffmanReader(ht);
        }
      }
    }
  }

  public huffmanReader[] getReaders() {
    return readers;
  }
}