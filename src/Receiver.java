import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author daniel
 *
 */
public class Receiver {
	protected MulticastSocket socket = null;
	protected byte[] buf = new byte[256];
	private String ip_address;
	private int portNumber;

	/**
	 * @param ip_address
	 * @param portNumber
	 */
	public Receiver(String ip_address, int portNumber) {
		super();
		this.ip_address = ip_address;
		this.portNumber = portNumber;
	}

	/**
	 * This method calls receives packets until it receives a
	 * packet with "end" payload.
	 * @throws IOException
	 */
	public void run() throws IOException {
		socket = new MulticastSocket(portNumber);
		InetAddress group = InetAddress.getByName(ip_address);
		socket.joinGroup(group);
		while (true) {
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			String received = new String(
		              packet.getData(), 0, packet.getLength());
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
			System.out.println("["+timeStamp+"]Data received:" + received);
			
			if ("end".equals(packet.getData())) {
				break;
			}
		}
		socket.leaveGroup(group);
		socket.close();
	}
}
