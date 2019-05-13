package myjava.com.github.dosarudaniel.gsoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

import myjava.com.github.dosarudaniel.gsoc.Blob.PACKET_TYPE;

public class FragmentedBlob {

	private int fragmentOffset;
	private PACKET_TYPE packetType;
	private UUID uuid;
	private int blobPayloadLength; // Total length of the Blob's payload
	private String key;
	private byte[] payloadChecksum;
	private byte[] payload;
	private byte[] packetChecksum;

	public FragmentedBlob(String payload) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		this.payload = payload.getBytes(Charset.forName("UTF-8"));
		this.payloadChecksum = Utils.calculateChecksum(this.payload);
	}

	public FragmentedBlob(byte[] payload, int fragmentOffset, PACKET_TYPE packetType, String key, UUID objectUuid)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		this.fragmentOffset = fragmentOffset;
		this.key = key;
		this.uuid = uuid;
		this.packetType = packetType;
		this.payloadChecksum = Utils.calculateChecksum(payload);
		this.payload = payload;
	}

	/*
	 * Manual deserialization of a serialisedFragmentedBlob
	 * 
	 */
	public FragmentedBlob(byte[] serialisedFragmentedBlob) {
		byte[] fragmentOffset_byte_array = Arrays.copyOfRange(serialisedFragmentedBlob, 0, 4);
		byte[] packetType_byte_array = Arrays.copyOfRange(serialisedFragmentedBlob, 4, 5);
		byte[] uuid_byte_array = Arrays.copyOfRange(serialisedFragmentedBlob, 5, 19);
		byte[] blobPayloadLength_byte_array = Arrays.copyOfRange(serialisedFragmentedBlob, 19, 23);
		byte[] key_byte_array = Arrays.copyOfRange(serialisedFragmentedBlob, 19, 23);

		ByteBuffer wrapped = ByteBuffer.wrap(fragmentOffset_byte_array);
		this.fragmentOffset = wrapped.getInt();
		this.key = new String(key_byte_array);
		this.uuid = Utils.getUuid(uuid_byte_array);
		this.packetType = (packetType_byte_array[0] == 0 ? PACKET_TYPE.METADATA : PACKET_TYPE.DATA);
		this.payloadChecksum = Arrays.copyOfRange(serialisedFragmentedBlob, 24, 40);
		this.payload = Arrays.copyOfRange(serialisedFragmentedBlob, 40, serialisedFragmentedBlob.length);
		this.packetChecksum = Arrays.copyOfRange(serialisedFragmentedBlob, 24, 40);
	}

	public int getFragmentOffset() {
		return this.fragmentOffset;
	}

	public void setFragmentOffset(int fragmentOffset) {
		this.fragmentOffset = fragmentOffset;
	}

	// manual serialization
	public byte[] toBytes() throws IOException, NoSuchAlgorithmException {
		byte[] fragmentOffset_byte_array = ByteBuffer.allocate(4).putInt(this.fragmentOffset).array();
		byte[] blobPayloadLength_byte_array = ByteBuffer.allocate(4).putInt(this.blobPayloadLength).array();
		// 0 -> METADATA
		// 1 -> DATA
		byte pachetType_byte = (byte) (this.packetType == PACKET_TYPE.METADATA ? 0 : 1);
		byte[] packetType_byte_array = new byte[1];
		packetType_byte_array[0] = pachetType_byte;

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			// 1. 4 bytes, fragment Offset
			out.write(fragmentOffset_byte_array);

			// 2. 1 byte, packet type or flags - to be decided
			out.write(packetType_byte_array);

			// 3. 16 bytes, uuid
			out.write(Utils.getBytes(this.uuid));

			// 4. 4 bytes, blob payload Length
			out.write(blobPayloadLength_byte_array);

			// 5. ? bytes, key
			out.write(this.key.getBytes(Charset.forName("UTF-8")));

			// 5. 16 bytes, payload checksum
			out.write(this.payloadChecksum);

			// 6. unknown number of bytes - the real data to be transported
			out.write(this.payload);

			// 7. 16 bytes, packet checksum
			out.write(Utils.calculateChecksum(out.toByteArray()));

			return out.toByteArray();
		}
	}

	public String getKey() {
		return this.key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public UUID getUuid() {
		return this.uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public PACKET_TYPE getPachetType() {
		return this.packetType;
	}

	public void setPachetType(PACKET_TYPE pachetType) {
		this.packetType = pachetType;
	}

	public byte[] getPayloadChecksum() {
		return this.payloadChecksum;
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
		if (!Arrays.equals(this.payloadChecksum, Utils.calculateChecksum(this.payload))) {
			throw new IOException("Checksum failed!");
		}
		return this.payload;
	}

	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	@Override
	public String toString() {
		String output = "";
		if (this.packetType == PACKET_TYPE.METADATA) {
			output += "Metadata ";
		} else {
			output += "Data ";
		}
		output += "fragmentedBlob with \n";
		output += "fragmentOffset = " + Integer.toString(this.fragmentOffset) + "\n";
		output += "key = " + this.key + "\n";
		output += "uuid = " + this.uuid.toString() + "\n";
		output += "payloadChecksum = " + new String(this.payloadChecksum) + "\n";
		output += "payload = " + new String(this.payload) + "\n";
		output += "packetChecksum = " + new String(this.packetChecksum) + "\n";

		return output;
	}
}
