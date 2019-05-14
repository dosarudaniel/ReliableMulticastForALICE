package test.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import myjava.com.github.dosarudaniel.gsoc.Blob;
import myjava.com.github.dosarudaniel.gsoc.Blob.PACKET_TYPE;
import myjava.com.github.dosarudaniel.gsoc.FragmentedBlob;
import myjava.com.github.dosarudaniel.gsoc.Utils;

// TODO, posibil de integrat in Receiver sau Sender
public class MulticastServer {
	static final int MIN_LEN = 50;
	static final int MAX_LEN = 130;
	
	Map<UUID, Blob> inFlight = new HashMap<>(); // uuid <-> Blob fragmentat, zonă de tranzitie până la primirea completă
												// a tuturor fragmentelor
	Map<String, Blob> currentCacheContent = new HashMap<>(); // Blob-uri complete

	public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
		String ip_address = args[0];
		int portNumber = Integer.parseInt(args[1]);
		int BUF_SIZE = 65536;
		byte[] buf = new byte[BUF_SIZE];

		String payload = Utils.generateRandomString(ThreadLocalRandom.current().nextInt(MIN_LEN, MAX_LEN));
		String key = "te";
		UUID uuid = UUID.randomUUID();
		FragmentedBlob fBlob = new FragmentedBlob(payload.getBytes(Charset.forName("UTF-8")), 257, PACKET_TYPE.DATA,
				key, uuid);
		fBlob.toBytes();

		System.out.println(fBlob);
		System.out.println(new FragmentedBlob(fBlob.toBytes()));

		try (MulticastSocket socket = new MulticastSocket(portNumber)) {
			InetAddress group = InetAddress.getByName(ip_address);
			socket.joinGroup(group);
			while (true) {
				// Receive object
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);

				FragmentedBlob fragmentedBlob = new FragmentedBlob(buf);

				// Print timestamp and content
//				String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
//				//String payload = "";// new String(blob.getPayload(), "UTF-8");
//				System.out.println("[" + timeStamp + "]Data received:" + payload);
//				if ("end".equals(payload)) {
//					break;
//				}

			}
			//socket.leaveGroup(group);
		}

		// while (true) {
//			FragmentedBlob fragmentedBlob = ByteToFragmentedBlob(pachet);

//		    Blob blob = updateInFlight(toFragment(receive());
//		
//		    invalidateCache(blob.getKey());
//		    if (blob.isComplete()){
//		
//		        moveToCache(blob);
//		    }
		// }
	}

	// Thread incompleteBlobRecovery = new Thread();
	// daca inFlight contine obiecte care nu au fost atinse de mai mult de X millis
	// => recovery procedure pentru ele
}
