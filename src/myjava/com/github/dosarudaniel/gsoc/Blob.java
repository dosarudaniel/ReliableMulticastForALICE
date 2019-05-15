/**
 * myjava.com.github.dosarudaniel.gsoc provides the classes necessary to send/
 * receive multicast messages which contains Blob object with random length,
 * random content payload.
 */
package myjava.com.github.dosarudaniel.gsoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Blob class - the structure of the object sent via multicast messages
 *
 * @author dosarudaniel@gmail.com
 * @since 2019-03-07
 *
 */

public class Blob {
	public enum PACKET_TYPE {
		METADATA, DATA;
	}

	// to be decided if this is necessary
	ArrayList<FragmentedBlob> blobFragmentsArrayList;
	ArrayList<byte[]> blobByteRange;

	private PACKET_TYPE packetType;
	private UUID uuid;
	private int payloadLength;
	private byte[] payloadChecksum;
	private short keyLength;
	private String key;
	private byte[] payload;
	private byte[] packetChecksum;

	/**
	 * Parameterized constructor - creates a Blob object that contains a payload and
	 * a checksum. The checksum is the sha1 of the payload.
	 *
	 * @param payload - The data string
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public Blob(String payload) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		this.payload = payload.getBytes(Charset.forName(Utils.CHARSET));
		this.payloadChecksum = Utils.calculateChecksum(this.payload);
	}

	public Blob(byte[] payload, PACKET_TYPE packetType, String key) throws NoSuchAlgorithmException {
		this.payload = payload;
		this.payloadChecksum = Utils.calculateChecksum(this.payload);
		this.packetType = packetType;
		this.key = key;
		this.keyLength = (short) key.length();
	}

	// va fi chemata de serverul UDP, pe masura ce primeste, deserializeaza un
	// fragment si vede carui Blob ii apartine
	public void notifyFragment(FragmentedBlob fragmentedBlob)
			throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException {
		addFragmentedBlob(fragmentedBlob);
	}

	/**
	 * Sends multicast message that contains the serialized version of a
	 * fragmentedBlob
	 *
	 * @param packet          - serialized fragmented Blob to send
	 * @param destinationIp   - Destination IP address (multicast)
	 * @param destinationPort - Destination port number
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static void sendFragmentMulticast(byte[] packet, String destinationIp, int destinationPort)
			throws IOException, NoSuchAlgorithmException {
		try (DatagramSocket socket = new DatagramSocket()) {
			InetAddress group = InetAddress.getByName(destinationIp);
			DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, group, destinationPort);
			socket.send(datagramPacket);
		}
	}

	public void send(String targetIp, int port) throws NoSuchAlgorithmException, IOException {
		byte[] packetBlob = this.toBytes();
		byte[] packetHeader = new byte[Utils.FRAGMENTED_BLOB_HEADER_LENGHT];

//		for (FragmentedBlob fragmentedBlob : this.blobFragmentsArrayList) {
//			sendFragmentMulticast(fragmentedBlob.toBytes(), targetIp, port);
//		}
	}

	// manual serialization
	public byte[] toBytes() throws IOException, NoSuchAlgorithmException {
		byte[] blobPayloadLength_byte_array = ByteBuffer.allocate(4).putInt(this.payloadLength).array();
		// 0 -> METADATA
		// 1 -> DATA
		byte pachetType_byte = (byte) (this.packetType == PACKET_TYPE.METADATA ? 0 : 1);
		byte[] packetType_byte_array = new byte[1];
		packetType_byte_array[0] = pachetType_byte;

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			// 1. 1 byte, packet type or flags - to be decided
			out.write(packetType_byte_array);

			// 2. 16 bytes, uuid
			out.write(Utils.getBytes(this.uuid));

			// 3. 4 bytes, blob payload Length
			out.write(blobPayloadLength_byte_array);

			// 4. ? bytes, key
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

	public boolean isComplete() {
		// TODO
		return true;
	}

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

			FragmentedBlob fragmentedBlob = new FragmentedBlob(fragmentedPayload, maxPayloadSize * i, this.packetType,
					this.key, this.uuid);
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

	public PACKET_TYPE getPachetType() {
		return this.packetType;
	}

	public void setPachetType(PACKET_TYPE packetType) {
		this.packetType = packetType;
	}

	public UUID getUuid() {
		return this.uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
}
