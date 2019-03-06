import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

public class Sender extends TimerTask {
	private DatagramSocket socket;
	private InetAddress group;
	private byte[] buf;
	private String ip_address;
	private int portNumber;

	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	static final int MIN_LEN = 50;
	static final int MAX_LEN = 130;

	public Sender(String ip_address, int portNumber) {
		super();
		this.ip_address = ip_address;
		this.portNumber = portNumber;
	}

	@Override
	public void run() {
		int randomNumber = ThreadLocalRandom.current().nextInt(MIN_LEN, MAX_LEN);
		Blob blob = null;
		try {
			blob = new Blob(randomString(randomNumber));
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			sendMulticast(blob);
			System.out.println("Data sent:" + blob.getData());
		} catch (NoSuchAlgorithmException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendMulticast(Blob blob) throws IOException, NoSuchAlgorithmException {
		socket = new DatagramSocket();
		group = InetAddress.getByName(ip_address);
		buf = blob.getData().getBytes();

		DatagramPacket packet = new DatagramPacket(buf, buf.length, group, portNumber);
		socket.send(packet);
		socket.close();
	}

	String randomString(int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			int randomNumber = ThreadLocalRandom.current().nextInt(0, AB.length());
			sb.append(AB.charAt(randomNumber));
		}

		return sb.toString();
	}
}
