
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
/**
 * This class is responsible for MD5 encrypting the data files.
 * 
 * @author Anurag Malik, Sahil Jasrotia
 */
public class Encryptor {

	static MessageDigest digest;

	public static BigInteger encryptSHA(String data) {
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Encryptor failed @ initiation.");
			e.printStackTrace();
		}
		byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
		return new BigInteger(1, hash);
	}

}
