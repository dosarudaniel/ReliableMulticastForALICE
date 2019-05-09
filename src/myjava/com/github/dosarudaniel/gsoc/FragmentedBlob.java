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

	/*
	 * Manual deserialization of a serialisedFragmentedBlob
	 * 
	 */
	public FragmentedBlob(byte[] serialisedFragmentedBlob) {
		byte[] fragmentOffset_byte_array = Arrays.copyOfRange(serialisedFragmentedBlob, 0, 4);
		byte[] key_byte_array = Arrays.copyOfRange(serialisedFragmentedBlob, 4, 6);
		byte[] packetType_byte_array = Arrays.copyOfRange(serialisedFragmentedBlob, 6, 7);
		byte[] uuid_byte_array = Arrays.copyOfRange(serialisedFragmentedBlob, 7, 23);

		ByteBuffer wrapped = ByteBuffer.wrap(fragmentOffset_byte_array);
		this.fragmentOffset = wrapped.getInt();
		this.key = new String(key_byte_array);
		this.objectUuid = UUIDUtils.getUuid(uuid_byte_array);
		this.packetType = (packetType_byte_array[0] == 0 ? PACKET_TYPE.METADATA : PACKET_TYPE.DATA);
		this.payloadChecksum = Arrays.copyOfRange(serialisedFragmentedBlob, 24, 40);
		this.payload = Arrays.copyOfRange(serialisedFragmentedBlob, 40, serialisedFragmentedBlob.length);
	}

	public int getFragmentOffset() {
		return this.fragmentOffset;
	}

	public void setFragmentOffset(int fragmentOffset) {
		this.fragmentOffset = fragmentOffset;
	}

	// manual serialization
	public byte[] toBytes() throws IOException {
		byte[] fragmentOffset_byte_array = ByteBuffer.allocate(4).putInt(fragmentOffset).array();
		// 0 -> METADATA
		// 1 -> DATA
		byte pachetType_byte = (byte) (this.packetType == PACKET_TYPE.METADATA ? 0 : 1);
		byte[] packetType_byte_array = new byte[1];
		packetType_byte_array[0] = pachetType_byte;

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			// 1. 4 bytes, fragment Offset
			out.write(fragmentOffset_byte_array);
			// 2. 2 bytes, key
			out.write(key.getBytes(Charset.forName("UTF-8")));
			// 3. 1 byte, packet type or flags - to be decided
			out.write(packetType_byte_array);
			// 4. 16 bytes, uuid
			out.write(UUIDUtils.getBytes(this.objectUuid));
			// 5. 16 bytes, payload checksum
			out.write(this.payloadChecksum);
			// 6. unknown number of bytes - the real data to be transported
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
