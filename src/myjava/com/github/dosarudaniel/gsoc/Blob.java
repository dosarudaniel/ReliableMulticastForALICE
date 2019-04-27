/**
 * myjava.com.github.dosarudaniel.gsoc provides the classes necessary to send/
 * receive multicast messages which contains Blob object with random length,
 * random content payload.
 */
package myjava.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Blob class - the structure of the object sent via multicast messages
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
	private static final int CHECKSUM_SIZE = 16;

	private int fragmentIdentification;
	private int fragmentOffset;
	private String key;
	private int pachetType; // metadata or data
	private String payload;
	private byte[] checksum;

	/**
	 * Parameterized constructor - creates a Blob object that contains a payload and
	 * a checksum. The checksum is the sha1 of the payload.
	 *
	 * @param payload - The data string
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public Blob(String payload) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		this.payload = payload;
		this.checksum = calculateChecksum(payload);
	}

	/**
	 * Returns the payload and checks if the checksum is correct.
	 *
	 * @return String - The payload of a Blob object
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 * @throws IOException                  - if checksum failed
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
	 * @param data used to generate checksum
	 * @return Checksum
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public static byte[] calculateChecksum(String data) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		mDigest.update(data.getBytes("utf8"));
		return mDigest.digest();
	}

	public int getFragmentIdentification() {
		return fragmentIdentification;
	}

	public void setFragmentIdentification(int fragmentIdentification) {
		this.fragmentIdentification = fragmentIdentification;
	}

	public int getFragmentOffset() {
		return fragmentOffset;
	}

	public void setFragmentOffset(int fragmentOffset) {
		this.fragmentOffset = fragmentOffset;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getPachetType() {
		return pachetType;
	}

	public void setPachetType(int pachetType) {
		this.pachetType = pachetType;
	}
}
