import java.io.Serializable;
import java.math.BigInteger;

/**
 * This class represents a hash node in the merkel tree implementation
 * 
 * @author Anurag Malik, Sahil Jasrotia
 */
class HashNode implements Serializable {
	private static final long serialVersionUID = 1L;
	Integer fileHash;
	BigInteger dataHash;

	public HashNode(Integer fileHash, BigInteger dataHash) {
		this.fileHash = fileHash;
		this.dataHash = dataHash;
	}
}