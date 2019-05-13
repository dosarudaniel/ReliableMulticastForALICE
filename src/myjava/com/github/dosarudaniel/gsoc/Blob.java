/**
 * myjava.com.github.dosarudaniel.gsoc provides the classes necessary to send/
 * receive multicast messages which contains Blob object with random length,
 * random content payload.
 */
package myjava.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.security.MessageDigest;
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
	private String key;
	private byte[] payloadChecksum;
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
		this.payload = payload.getBytes(Charset.forName("UTF-8"));
		this.payloadChecksum = calculateChecksum(this.payload);
	}

	public Blob(byte[] payload, PACKET_TYPE packetType, String key)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {
		this.payload = payload;
		this.payloadChecksum = calculateChecksum(this.payload);
		this.packetType = packetType;
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

	public void send(String targetIp, int port) throws NoSuchAlgorithmException, IOException {
		for (FragmentedBlob fragmentedBlob : this.blobFragmentsArrayList) {
			sendFragmentMulticast(fragmentedBlob.toBytes(), targetIp, port);
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

	public static byte[] calculateChecksum(byte[] data) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		mDigest.update(data);
		return mDigest.digest();
	}
}
