package myjava.com.github.dosarudaniel.gsoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {
    public final static int PACKET_MAX_SIZE = 65536;
    public final static String CHARSET = "UTF-8";
    public final static String CHECKSUM_TYPE = "MD5";
    static final String AB = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    // Field size (in bytes)
    public final static int SIZE_OF_FRAGMENT_OFFSET = 4;
    public final static int SIZE_OF_PACKET_TYPE = 1;
    public final static int SIZE_OF_UUID = 16;
    public final static int SIZE_OF_BLOB_PAYLOAD_LENGTH = 4;
    public final static int SIZE_OF_KEY_LENGTH = 2;
    public final static int SIZE_OF_PAYLOAD_CHECKSUM = 16;
    // public final static int SIZE_OF_KEY = ???;
    // public final static int SIZE_OF_PAYLOAD = ???;
    public final static int SIZE_OF_PACKET_CHECKSUM = 16;

    public final static int SIZE_OF_FRAGMENTED_BLOB_HEADER = SIZE_OF_FRAGMENT_OFFSET + SIZE_OF_PACKET_TYPE
	    + SIZE_OF_UUID + SIZE_OF_BLOB_PAYLOAD_LENGTH + SIZE_OF_KEY_LENGTH + SIZE_OF_PAYLOAD_CHECKSUM;
    public final static int SIZE_OF_FRAGMENTED_BLOB_HEADER_AND_TRAILER = SIZE_OF_FRAGMENTED_BLOB_HEADER
	    + SIZE_OF_PACKET_CHECKSUM;

    // Fragment Offset:-- 0 ........ 3
    // Packet Type: ----- 4
    // UUID: ------------ 5 ........ 20
    // blobPayloadLength: 21 ....... 24
    // keyLength:-------- 25 ....... 26
    // payloadChecksum:-- 27........ 42
    // key: ------------- 43 ....... 43+x-1
    // payload: --------- 43+x ..... 43+x+y-1
    // packetChecksum: -- 43+x+y ... 58+x+y

    // Start indexes of the fields in the serialized byte[]
    public final static int FRAGMENT_OFFSET_START_INDEX = 0;
    public final static int PACKET_TYPE_START_INDEX = FRAGMENT_OFFSET_START_INDEX + SIZE_OF_FRAGMENT_OFFSET;
    public final static int UUID_START_INDEX = PACKET_TYPE_START_INDEX + SIZE_OF_PACKET_TYPE;
    public final static int BLOB_PAYLOAD_LENGTH_START_INDEX = UUID_START_INDEX + SIZE_OF_UUID;
    public final static int KEY_LENGTH_START_INDEX = BLOB_PAYLOAD_LENGTH_START_INDEX + SIZE_OF_BLOB_PAYLOAD_LENGTH;
    public final static int PAYLOAD_CHECKSUM_START_INDEX = KEY_LENGTH_START_INDEX + SIZE_OF_KEY_LENGTH;
    public final static int KEY_START_INDEX = PAYLOAD_CHECKSUM_START_INDEX + SIZE_OF_PAYLOAD_CHECKSUM;
    // public final static int PAYLOAD_START_INDEX = KEY_START_INDEX + SIZE_OF_KEY
    // (unknown);
    // public final static int PACKET_CHECKSUM_START_INDEX = PAYLOAD_START_INDEX +
    // SIZE_OF_PAYLOAD (unknown);

    public static class Pair {
	public int first;
	public int second;

	public Pair(int first, int second) {
	    this.first = first;
	    this.second = second;
	}

	@Override
	public String toString() {
	    return "(" + Integer.toString(this.first) + "," + Integer.toString(this.second) + ")";
	}
    }

    public static class PairComparator implements Comparator<Pair> {
	@Override
	public int compare(Pair p1, Pair p2) {
	    return p1.first - p2.first;
	}
    }

    /**
     * Calculates the CHECKSUM_TYPE checksum of the byte[] data Default
     * CHECKSUM_TYPE - MD5
     * 
     * @param byte[] - data
     * @return byte[] - the checksum of the data
     */
    public static byte[] calculateChecksum(byte[] data) throws NoSuchAlgorithmException {
	MessageDigest mDigest = MessageDigest.getInstance(CHECKSUM_TYPE);
	mDigest.update(data);
	return mDigest.digest();
    }

    /**
     * Converts a byte[] into uuid
     * 
     * @param byte[] - the serialized uuid
     * @return uuid - the deserialized UUID
     */
    public static UUID getUuid(byte[] bytes) {
	ByteBuffer bb = ByteBuffer.wrap(bytes);
	long firstLong = bb.getLong();
	long secondLong = bb.getLong();
	return new UUID(firstLong, secondLong);
    }

    /**
     * Converts an uuid into byte[]
     * 
     * @param uuid - An UUID
     * @return byte[] - the serialized uuid
     */
    public static byte[] getBytes(UUID uuid) {
	ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
	bb.putLong(uuid.getMostSignificantBits());
	bb.putLong(uuid.getLeastSignificantBits());
	return bb.array();
    }

    /**
     * Generates a random content string of length len
     * 
     * @param len - Length of the randomString
     * @return String - A random content string of length len
     */
    public static String randomString(int len) {
	StringBuilder sb = new StringBuilder(len);
	for (int i = 0; i < len; i++) {
	    int randomNumber = ThreadLocalRandom.current().nextInt(0, AB.length());
	    sb.append(AB.charAt(randomNumber));
	}

	return sb.toString();
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

    /**
     * Serializes a Map of metadata <key, value> into byte[]
     *
     * @param metadata - Map<String, String> with metadata key, values
     * @return byte array with the serialized metadata
     * @throws IOException
     */
    public static byte[] serializeMetadata(Map<String, String> metadataMap) throws IOException {
	byte[] size_array = ByteBuffer.allocate(Integer.BYTES).putInt(metadataMap.size()).array();
	byte[] key_length_array;
	byte[] value_length_array;

	try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
	    out.write(size_array);
	    String key, value;
	    Iterator<Entry<String, String>> it = metadataMap.entrySet().iterator();
	    while (it.hasNext()) {
		Map.Entry<String, String> pair = it.next();
		key = pair.getKey();
		value = pair.getValue();

		key_length_array = ByteBuffer.allocate(Integer.BYTES).putInt(key.length()).array();
		value_length_array = ByteBuffer.allocate(Integer.BYTES).putInt(value.length()).array();
		out.write(key_length_array);
		out.write(key.getBytes());
		out.write(value_length_array);
		out.write(value.getBytes());

		it.remove(); // avoids a ConcurrentModificationException
	    }

	    // int size elements
	    // -int keyLength
	    // -String key
	    // -int valueLength
	    // -String value
	    return out.toByteArray();
	}
    }

    /**
     * Serializes a Map of metadata <key, value> into byte[]
     *
     * @param metadata - byte array with the serialized metadata
     * @return metadata - Map<String, String> with metadata key, values
     * @throws IOException
     */
    public static Map<String, String> deserializeMetadata(byte[] metadata) {
	if (metadata == null) {
	    // return empty HashMap
	    return new HashMap<>();
	}

	byte[] key_length_array;
	byte[] value_length_array;
	byte[] key_array;
	byte[] value_array;
	String key, value;
	int keyLength, valueLength;
	int index = Integer.BYTES;

	byte[] size_array = Arrays.copyOfRange(metadata, 0, index);
	ByteBuffer wrapped = ByteBuffer.wrap(size_array);
	int size = wrapped.getInt();
	Map<String, String> metadataMap = new HashMap<>();

	for (int i = 0; i < size; i++) {
	    key_length_array = Arrays.copyOfRange(metadata, index, index + Integer.BYTES);
	    index += Integer.BYTES;
	    wrapped = ByteBuffer.wrap(key_length_array);
	    keyLength = wrapped.getInt();

	    key_array = Arrays.copyOfRange(metadata, index, index + keyLength);
	    index += keyLength;
	    key = new String(key_array, StandardCharsets.UTF_8);

	    value_length_array = Arrays.copyOfRange(metadata, index, index + Integer.BYTES);
	    index += Integer.BYTES;
	    wrapped = ByteBuffer.wrap(value_length_array);
	    valueLength = wrapped.getInt();

	    value_array = Arrays.copyOfRange(metadata, index, index + valueLength);
	    index += valueLength;
	    value = new String(value_array, StandardCharsets.UTF_8);

	    metadataMap.put(key, value);
	}
	return metadataMap;
    }
}