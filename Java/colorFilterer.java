package bcif;

/**
 * <p> Interface representing a predictive color filter. </p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public interface colorFilterer {

  public byte colFilter(byte c0, byte c1, byte c2, int pos);

}
