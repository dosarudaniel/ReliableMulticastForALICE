package myjava.com.github.dosarudaniel.gsoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

import myjava.com.github.dosarudaniel.gsoc.Blob.PACKET_TYPE;

public class FragmentedBlob {
	private static final int CHECKSUM_SIZE = 16;

	private int fragmentOffset;
	private String key;
	private UUID objectUuid;
	private PACKET_TYPE packetType;
	private byte[] payloadChecksum;
	private byte[] payload;

	public FragmentedBlob(String payload) {
	}

	public FragmentedBlob(byte[] payload, int fragmentOffset, PACKET_TYPE packetType, String key, UUID objectUuid)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		this.fragmentOffset = fragmentOffset;
		this.key = key;
		this.objectUuid = objectUuid;
		this.packetType = packetType;
		this.payloadChecksum = calculateChecksum(payload);
		this.payload = payload;
	}

	public FragmentedBlob(byte[] serialisedFragmentedBlob) {
		// deserializare manuala
		// TODO

	}

	public int getFragmentOffset() {
		return this.fragmentOffset;
	}

	public void setFragmentOffset(int fragmentOffset) {
		this.fragmentOffset = fragmentOffset;
	}

	// serializare manuala
	public byte[] toBytes() throws IOException {
		byte[] fragmentOffset_byte = ByteBuffer.allocate(4).putInt(fragmentOffset).array();
		// 0 -> METADATA
		// 1 -> DATA
		byte pachetType_byte = (byte) (this.packetType == PACKET_TYPE.METADATA ? 0 : 1);
		byte[] packetType_byte_array = new byte[1];

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			// 1. fragment Offset
			out.write(fragmentOffset_byte);
			// 2. key
			out.write(key.getBytes(Charset.forName("UTF-8")));

			packetType_byte_array[0] = pachetType_byte;
			out.write(packetType_byte_array);

			out.write(UUIDUtils.getBytes(this.objectUuid));

			out.write(this.payloadChecksum);
			out.write(this.payload);

			byte[] pachet = out.toByteArray();
			System.out.println(Arrays.toString(pachet));
			return pachet;
		}
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public UUID getObjectUUID() {
		return objectUuid;
	}

	public void setObjectUUID(UUID objectUuid) {
		this.objectUuid = objectUuid;
	}

	public PACKET_TYPE getPachetType() {
		return packetType;
	}

	public void setPachetType(PACKET_TYPE pachetType) {
		this.packetType = pachetType;
	}

	public byte[] getPayloadChecksum() {
		return payloadChecksum;
	}

	public void setPayloadChecksum(byte[] payloadChecksum) {
		this.payloadChecksum = payloadChecksum;
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

	public void setPayload(byte[] payload) {
		this.payload = payload;
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
}
