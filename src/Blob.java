import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Blob class
 *
 * @author dosarudaniel@gmail.com
 * @since 2019-03-07
 *
 */
public class Blob implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	static final int CHECKSUM_SIZE = 16;
	private String payload;
	private byte[] checksum;

	/**
	 * Parameterized constructor - creates a Blob object and it Calculates its
	 * checksum
	 *
	 * @param payload
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public Blob(String payload) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		this.payload = payload;
		this.checksum = CalculateChecksum(payload);
	}

	/**
	 * Returns the payload and checks if the checksum is correct. Prints an error if
	 * the object is corrupt
	 *
	 * @return String - The payload of a Blob object
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public String getPayload() throws NoSuchAlgorithmException, UnsupportedEncodingException {
		byte[] checksum1 = CalculateChecksum(this.payload);
		if (Arrays.toString(this.checksum).equals(Arrays.toString(checksum1)) == false) {
			System.err.println("Checksum failed!");
		}
		return this.payload;
	}

	/**
	 * Calculates the checksum for a certain string data
	 *
	 * @param data
	 * @return byte[] -  the checksum
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public static byte[] CalculateChecksum(String data) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return sha1(data);
	}

	/**
	 * Calculates the sha1 code for a string
	 *
	 * @param data
	 * @return byte[] The SHA1 hash for the payload data
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	static byte[] sha1(String data) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		mDigest.update(data.getBytes("utf8"));
		return mDigest.digest();
	}
}
