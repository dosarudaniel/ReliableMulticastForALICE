package myjava.com.github.dosarudaniel.gsoc;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumUtils {

	public static byte[] calculateChecksum(byte[] data) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		mDigest.update(data);
		return mDigest.digest();
	}
}
