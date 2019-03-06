
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author daniel
 *
 */
public class Blob {
	private String data;
	private String checksum;

	public Blob(String data) throws NoSuchAlgorithmException {
		this.data = data;
		this.checksum = CalculateChecksum(data);
	}

	/**
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public String getData() throws NoSuchAlgorithmException {
		String checksum = CalculateChecksum(data);
		if (this.checksum.equals(checksum) == false) {
			System.err.println("Checksum failed!");
		}
		return data;
	}

	/**
	 * @param data
	 * @throws NoSuchAlgorithmException
	 */
	public void setData(String data) throws NoSuchAlgorithmException {
		this.data = data;
		this.checksum = CalculateChecksum(data);
	}

	/**
	 * @param data
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public String CalculateChecksum(String data) throws NoSuchAlgorithmException {
		return sha1(data);
	}

	/**
	 * @param data
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	static String sha1(String data) throws NoSuchAlgorithmException {
		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		byte[] result = mDigest.digest(data.getBytes());
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < result.length; i++) {
			sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
		}

		return sb.toString();
	}
}
