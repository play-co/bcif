package bcif;

/**
 * <p> Interface representing a predictive filtering function. </p>
 * @author Stefano Brocchi
 * @version 1.0 beta
 */

public interface filterer {

  public byte filter(byte left, byte low, byte ll, byte lr);

}