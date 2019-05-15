package myjava.com.github.dosarudaniel.gsoc;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Utils {
	public final static int FRAGMENTED_BLOB_HEADER_LENGHT = 43;
	public final static int PACKET_MAX_SIZE = 65536;
	public final static String CHARSET = "UTF-8";
	
	static final String AB = "0123456789abcdefghijklmnopqrstuvwxyz";

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