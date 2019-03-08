
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author dosarudaniel@gmail.com
 * @since 2019-03-07
 *
 */
public class Sender extends TimerTask {
	private String ip_address;
	private int portNumber;

	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	static final int MIN_LEN = 50;
	static final int MAX_LEN = 130;

	/**
	 * Parameterized constructor
	 *
	 * @param ip_address
	 * @param portNumber
	 */
	public Sender(String ip_address, int portNumber) {
		super();
		this.ip_address = ip_address;
		this.portNumber = portNumber;
	}

	/**
	 * Creates an object with a random length, random content payload. Calls the
	 * sendMulticast method every (default 10) seconds. Prints timestamp and the
	 * payload that was sent.
	 *
	 */
	@Override
	public void run() {
		int randomNumber = ThreadLocalRandom.current().nextInt(MIN_LEN, MAX_LEN);
		Blob blob = null;
		String payload = randomString(randomNumber);
		try {
			blob = new Blob(payload);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		try {
			sendMulticast(blob);

			String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
			System.out.println("[" + timeStamp + "] Data sent:" + payload);
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends multicast message with a serialized version of an object of type Blob
	 *
	 * @param blob
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public void sendMulticast(Blob blob) throws IOException, NoSuchAlgorithmException {
		DatagramSocket socket = new DatagramSocket();
		InetAddress group = InetAddress.getByName(this.ip_address);
		byte[] buf = serialize(blob);
		DatagramPacket packet = new DatagramPacket(buf, buf.length, group, this.portNumber);
		socket.send(packet);
		socket.close();
	}

	/**
	 * Generates a random content string
	 *
	 * @param len
	 * @return String - A random content string of length len
	 */
	static String randomString(int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			int randomNumber = ThreadLocalRandom.current().nextInt(0, AB.length());
			sb.append(AB.charAt(randomNumber));
		}

		return sb.toString();
	}

	/**
	 * Serializes a serializable Object
	 *
	 * @param obj
	 * @return byte[] the serialized version of the object
	 * @throws IOException
	 */
	public static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out);
		os.flush();
		os.writeObject(obj);
		byte[] b = out.toByteArray();
		return b;
	}
}
