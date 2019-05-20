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
	ArrayList<byte[]> blobByteRange_data;
	ArrayList<byte[]> blobByteRange_metadata;

	private UUID uuid;
	// private int payloadLength; // <-- payload.length
	// private int metadataLength; // <-- metadata.legth
	private byte[] payloadAndMetadataChecksum;
	// private short keyLength; //<- key.length()
	private String key;
	private byte[] metadata;
	private byte[] payload;

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
		this.payloadAndMetadataChecksum = Utils.calculateChecksum(this.payload);
	}

	public Blob(byte[] payload, PACKET_TYPE packetType, String key) throws NoSuchAlgorithmException {
		this.payload = payload;
		this.payloadAndMetadataChecksum = Utils.calculateChecksum(this.payload);
		this.key = key;
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

	/*
	 * 
	 * */
	public void send(String targetIp, int port) throws NoSuchAlgorithmException, IOException {
		// Build the header
		// purple color from this presentation: (Packet structure slide - currently nr
		// 3)
		// https://docs.google.com/presentation/d/1NXMBqXNdzLBOgGuXfYXW8AR1c3fJt8gD8OTJwlwKJk8/edit?usp=sharing
		byte[] commonHeader = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER - Utils.SIZE_OF_FRAGMENT_OFFSET
				- Utils.SIZE_OF_PAYLOAD_CHECKSUM];

		// Alternative:
//		for (FragmentedBlob fragmentedBlob : this.blobFragmentsArrayList) {
//			sendFragmentMulticast(fragmentedBlob.toBytes(), targetIp, port);
//		}
	}

	// manual serialization
	public byte[] toBytes() throws IOException, NoSuchAlgorithmException {
		byte[] blobPayloadLength_byte_array = ByteBuffer.allocate(4).putInt(payload.length).array();
		byte[] keyLength_byte_array = ByteBuffer.allocate(2).putShort((short) key.length()).array();// putShort(this.key).array();

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			// 1. 16 bytes, uuid
			out.write(Utils.getBytes(this.uuid));

			// 2. 4 bytes, blob payload Length
			out.write(blobPayloadLength_byte_array);

			// 3. Metadata length

			// 4. 16 bytes, (payload + metadata) checksum
			out.write(this.payloadAndMetadataChecksum);

			// 5. key length
			out.write(keyLength_byte_array);

			// 6. ? bytes, key
			out.write(this.key.getBytes(Charset.forName("UTF-8")));

			// 7. unknown number of bytes - metadata
			out.write(this.metadata);

			// 8. unknown number of bytes - the real data to be transported
			out.write(this.payload);

			// No need for packet checksum since I do not have any fragmentation here ?
//			// 9. 16 bytes, packet checksum
//			out.write(Utils.calculateChecksum(out.toByteArray()));

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
	 * @throws IOException
	 */
	public void fragmentBlob(int maxPayloadSize) throws NoSuchAlgorithmException, IOException {
		int numberOfMetadataFragments = this.metadata.length / maxPayloadSize;
		int numberOfPayloadFragments = this.payload.length / maxPayloadSize;
		int i = 0;
		byte[] fragmentedPayload;
		for (i = 0; i < numberOfMetadataFragments; i++) {
			fragmentedPayload = new byte[maxPayloadSize];
			System.arraycopy(this.metadata, maxPayloadSize * i, fragmentedPayload, 0, maxPayloadSize);
			blobByteRange_metadata.add(fragmentedPayload);
		}
		// put the remaining bytes from metadata
		fragmentedPayload = new byte[this.metadata.length - maxPayloadSize*i];
		System.arraycopy(this.metadata, maxPayloadSize * i, fragmentedPayload, 0, this.metadata.length - maxPayloadSize*i);
		blobByteRange_metadata.add(fragmentedPayload);

		
		for (i = 0; i < numberOfPayloadFragments; i++) {
			fragmentedPayload = new byte[maxPayloadSize];
			System.arraycopy(this.payload, maxPayloadSize * i, fragmentedPayload, 0, maxPayloadSize);
			blobByteRange_data.add(fragmentedPayload);
		}
		// put the remaining bytes from the payload
		fragmentedPayload = new byte[this.payload.length - maxPayloadSize*i];
		System.arraycopy(this.payload, maxPayloadSize * i, fragmentedPayload, 0, this.payload.length - maxPayloadSize*i);
		blobByteRange_data.add(fragmentedPayload);
		
		
		// Idea: fragment this Blob directly into multiple serialized fragmentedBlobs and put this in a different function
		// ^TODO^
//		// int lastFragmentPayloadLength = this.payload.length % maxPayloadSize;
//		byte[] fragmentOffset_byte_array;// = ByteBuffer.allocate(4).putInt(this.fragmentOffset).array();
//		/*
//		 * 
//		 * fragment metadata
//		 * 
//		 */
//		byte[] commonHeader = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER - Utils.SIZE_OF_FRAGMENT_OFFSET
//				- Utils.SIZE_OF_PAYLOAD_CHECKSUM];
//
//		// 0 -> METADATA
//		// 1 -> DATA
//		byte pachetType_byte = (byte) 0; // METADATA
//		byte[] packetType_byte_array = new byte[1];
//		packetType_byte_array[0] = pachetType_byte;
//
//		byte[] blobMetadataLength_byte_array = ByteBuffer.allocate(4).putInt(this.metadata.length).array();
//		byte[] keyLength_byte_array = ByteBuffer.allocate(2).putShort((short) this.key.length()).array();
//
//		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//			// 2. 1 byte, packet type or flags - to be decided
//			out.write(packetType_byte_array);
//
//			// 3. 16 bytes, uuid
//			out.write(Utils.getBytes(this.uuid));
//
//			// 4. 4 bytes, blob metadata Length
//			out.write(blobMetadataLength_byte_array);
//
//			// 5. 2 bytes, keyLength
//			out.write(keyLength_byte_array);
//
//			commonHeader = out.toByteArray();
//		}
//
//
//		/*
//		 * 
//		 * fragment data
//		 * 
//		 */
//		commonHeader = new byte[Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER - Utils.SIZE_OF_FRAGMENT_OFFSET
//				- Utils.SIZE_OF_PAYLOAD_CHECKSUM];
//
//		// 0 -> METADATA
//		// 1 -> DATA
//		pachetType_byte = (byte) 1; // DATA
//		packetType_byte_array = new byte[1];
//		packetType_byte_array[0] = pachetType_byte;
//
//		byte[] blobPayloadLength_byte_array = ByteBuffer.allocate(4).putInt(this.payload.length).array();
//		keyLength_byte_array = ByteBuffer.allocate(2).putShort((short) this.key.length()).array();
//
//		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//			// 2. 1 byte, packet type or flags - to be decided
//			out.write(packetType_byte_array);
//
//			// 3. 16 bytes, uuid
//			out.write(Utils.getBytes(this.uuid));
//
//			// 4. 4 bytes, blob payload Length
//			out.write(blobPayloadLength_byte_array);
//
//			// 5. 2 bytes, keyLength
//			out.write(keyLength_byte_array);
//
//			commonHeader = out.toByteArray();
//		}

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
}
