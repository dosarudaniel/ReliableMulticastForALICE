package test.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import myjava.com.github.dosarudaniel.gsoc.Blob;
import myjava.com.github.dosarudaniel.gsoc.FragmentedBlob;
import myjava.com.github.dosarudaniel.gsoc.Sender;
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
		int key_length = 4;
		int BUF_SIZE = Sender.MAX_PAYLOAD_SIZE + Utils.SIZE_OF_FRAGMENTED_BLOB_HEADER_AND_TRAILER + key_length;
		byte[] buf = new byte[BUF_SIZE];

		Blob blobReceived = new Blob();

		try (MulticastSocket socket = new MulticastSocket(portNumber)) {
			InetAddress group = InetAddress.getByName(ip_address);
			socket.joinGroup(group);
			while (true) {
				// Receive object
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);

				System.out.println("packet received, length " + buf.length + "== " + Arrays.toString(buf));
				FragmentedBlob fragmentedBlob = new FragmentedBlob(buf);

				// chose the blob to put the fragmentedBlob
				if (blobReceived.getKey().equals("")) {
					blobReceived.setKey(fragmentedBlob.getKey());
					blobReceived.setUuid(fragmentedBlob.getUuid());
				}

				blobReceived.addFragmentedBlob(fragmentedBlob);

				// Print timestamp and content
//				String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
//				//String payload = "";// new String(blob.getPayload(), "UTF-8");
//				System.out.println("[" + timeStamp + "]Data received:" + payload);
//				if ("end".equals(payload)) {
//					break;
//				}
//				System.out.println("[" + timeStamp + "] Received blob : with payload "
//						+ Arrays.toString(blobReceived.getMetadata()));

//				if (blobReceived.isComplete()) {
//					break;
//				}

			}
			// socket.leaveGroup(group);
		}

	}

	// Thread incompleteBlobRecovery = new Thread();
	// daca inFlight contine obiecte care nu au fost atinse de mai mult de X millis
	// => recovery procedure pentru ele
}
