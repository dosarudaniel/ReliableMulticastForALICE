package myjava.com.github.dosarudaniel.gsoc;
import java.io.IOException;
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
	 * Parameterized constructor - creates a Blob object and it calculates its
	 * checksum
	 *
	 * @param payload
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public Blob(String payload) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		this.payload = payload;
		this.checksum = calculateChecksum(payload);
	}

	/**
	 * Returns the payload and checks if the checksum is correct. Prints an error if
	 * the object is corrupt
	 *
	 * @return String - The payload of a Blob object
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 * @throws IOException - if checksum failed
	 */
	public String getPayload() throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException {
		byte[] checksum1 = calculateChecksum(this.payload);
		if (!Arrays.equals(this.checksum, checksum1)) {
			throw new IOException("Checksum failed!");
		}
		return this.payload;
	}

	/**
	 * Calculates the sha1 checksum for a certain string data
	 *
	 * @param data
	 * @return byte[] - the checksum
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public static byte[] calculateChecksum(String data) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		mDigest.update(data.getBytes("utf8"));
		return mDigest.digest();
	}
}
