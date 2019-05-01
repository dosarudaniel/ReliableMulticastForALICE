/**
 * myjava.com.github.dosarudaniel.gsoc provides the classes necessary to send/
 * receive multicast messages which contains Blob object with random length,
 * random content payload.
 */
package myjava.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

/**
 * Blob class - the structure of the object sent via multicast messages
 *
 * @author dosarudaniel@gmail.com
 * @since 2019-03-07
 *
 */

public class Blob implements Serializable {
	public enum PACHET_TYPE {
		METADATA, DATA;
	}

	private static final long serialVersionUID = 1L;
	private static final int CHECKSUM_SIZE = 16;

	// private int fragmentOffset;
	private String key;
	private UUID objectUUID;
	private PACHET_TYPE pachetType;
	private byte[] payload;
	private byte[] payloadChecksum;

	/**
	 * Parameterized constructor - creates a Blob object that contains a payload and
	 * a checksum. The checksum is the sha1 of the payload.
	 *
	 * @param payload - The data string
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public Blob(String payload) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		this.payload = payload.getBytes(Charset.forName("UTF-8"));
		this.payloadChecksum = calculateChecksum(this.payload);
	}

	public Blob(byte[] payload, PACHET_TYPE pachetType, String key)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		this.payload = payload;
		this.payloadChecksum = calculateChecksum(this.payload);
		this.pachetType = pachetType;
		this.key = key;
	}

	/**
	 * Returns the payload and checks if the checksum is correct.
	 *
	 * @return String - The payload of a Blob object
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 * @throws IOException                  - if checksum failed
	 */
	public byte[] getPayload() throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException {
		if (!Arrays.equals(this.payloadChecksum, calculateChecksum(this.payload))) {
			throw new IOException("Checksum failed!");
		}
		return this.payload;
	}

	/**
	 * Calculates the sha1 checksum for a certain string data
	 *
	 * @param data used to generate checksum
	 * @return payloadChecksum
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public static byte[] calculateChecksum(byte[] data) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		mDigest.update(data);
		return mDigest.digest();
	}

//	public int getFragmentOffset() {
//		return this.fragmentOffset;
//	}
//
//	public void setFragmentOffset(int fragmentOffset) {
//		this.fragmentOffset = fragmentOffset;
//	}

	/**
	 * Assemble a Blob by adding one FragmentedBlob to it
	 *
	 * @param fragmentedBlob
	 * @return void
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public void addFragmentedBlob(FragmentedBlob fragmentedBlob)
			throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException {
		byte[] fragmentedPayload = fragmentedBlob.getPayload();
		int fragmentOffset = fragmentedBlob.getFragmentOffset();
		System.arraycopy(fragmentedPayload, 0, this.payload, fragmentOffset, fragmentedPayload.length);
	}

	/**
	 * Fragments a Blob into multiple FragmentedBlobs
	 *
	 * @param maxPayloadSize
	 * @return ArrayList<FragmentedBlob>
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public ArrayList<FragmentedBlob> fragmentBlob(int maxPayloadSize)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		ArrayList<FragmentedBlob> blobFragments = new ArrayList<>();
		int numberOfFragments = this.payload.length / maxPayloadSize;
		int lastFragmentPayloadLength = this.payload.length % maxPayloadSize;

		for (int i = 0; i < numberOfFragments; i++) {
			byte[] fragmentedPayload = null;
			if (i == numberOfFragments - 1) {
				System.arraycopy(this.payload, maxPayloadSize * i, fragmentedPayload, 0, lastFragmentPayloadLength);
			} else {
				System.arraycopy(this.payload, maxPayloadSize * i, fragmentedPayload, 0, maxPayloadSize);
			}

			FragmentedBlob fragmentedBlob = new FragmentedBlob(fragmentedPayload, maxPayloadSize * i, this.pachetType,
					this.key);
			blobFragments.add(fragmentedBlob);
		}

		return blobFragments;
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public PACHET_TYPE getPachetType() {
		return this.pachetType;
	}

	public void setPachetType(PACHET_TYPE pachetType) {
		this.pachetType = pachetType;
	}

	public UUID getObjectUUID() {
		return this.objectUUID;
	}

	public void setObjectUUID(UUID objectUUID) {
		this.objectUUID = objectUUID;
	}
}
