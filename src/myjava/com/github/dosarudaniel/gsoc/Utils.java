package myjava.com.github.dosarudaniel.gsoc;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {
	public final static int PACKET_MAX_SIZE = 65536;
	public final static String CHARSET = "UTF-8";
	static final String AB = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	// Fragment Offset: 0 ........ 3
	// Packet Type: 4
	// UUID: 5 ........ 20
	// blobPayloadLength: 21 ....... 24
	// payloadChecksum: 25........ 40
	// keyLength: 41 ....... 42
	// key: 43 ....... 43+x-1
	// payload: 43+x ..... 43+x+y-1
	// packetChecksum: 43+x+y ... 58+x+y

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
			+ SIZE_OF_UUID + SIZE_OF_BLOB_PAYLOAD_LENGTH + SIZE_OF_PAYLOAD_CHECKSUM + SIZE_OF_KEY_LENGTH;

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

	public static byte[] calculateChecksum(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		mDigest.update(data);
		return mDigest.digest();
	}

	public static UUID getUuid(byte[] bytes) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		long firstLong = bb.getLong();
		long secondLong = bb.getLong();
		return new UUID(firstLong, secondLong);
	}

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
	public static String generateRandomString(int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			int randomNumber = ThreadLocalRandom.current().nextInt(0, AB.length());
			sb.append(AB.charAt(randomNumber));
		}

		return sb.toString();
	}
}