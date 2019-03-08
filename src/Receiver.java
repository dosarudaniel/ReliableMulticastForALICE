
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * @author dosarudaniel@gmail.com
 * @since 2019-03-07
 *
 */
public class Receiver {
	final static int BUF_SIZE = 64000;
	protected MulticastSocket socket = null;
	protected byte[] buf = new byte[BUF_SIZE];
	private String ip_address;
	private int portNumber;

	/**
	 * Parameterized constructor
	 *
	 * @param ip_address
	 * @param portNumber
	 */
	public Receiver(String ip_address, int portNumber) {
		super();
		this.ip_address = ip_address;
		this.portNumber = portNumber;
	}

	/**
	 * This method calls receives packets until it receives a packet with "end"
	 * payload.
	 *
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws NoSuchAlgorithmException
	 */
	public void work() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
		this.socket = new MulticastSocket(this.portNumber);
		InetAddress group = InetAddress.getByName(this.ip_address);
		this.socket.joinGroup(group);
		while (true) {
			// Receive object
			DatagramPacket packet = new DatagramPacket(this.buf, this.buf.length);
			this.socket.receive(packet);
			// deserialize
			Blob blob = (Blob) deserialize(this.buf);
			// Print timestamp and content
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
			System.out.println("[" + timeStamp + "]Data received:" + blob.getPayload());
			if ("end".equals(blob.getPayload())) {
				break;
			}

		}

		this.socket.leaveGroup(group);
		this.socket.close();

	}

	/**
	 * This function deserializes objects
	 *
	 * @param data
	 * @return Object - The deserialized object
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);

		return is.readObject();
	}
}
