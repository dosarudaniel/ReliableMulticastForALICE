package test.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import myjava.com.github.dosarudaniel.gsoc.Blob;
import myjava.com.github.dosarudaniel.gsoc.FragmentedBlob;
import myjava.com.github.dosarudaniel.gsoc.Utils;

// TODO, posibil de integrat in Receiver sau Sender
public class MulticastServer {
    private static Logger logger = Logger.getLogger("MulticastServer");
    static final int MIN_LEN = 50;
    static final int MAX_LEN = 130;

    Map<UUID, Blob> inFlight = new HashMap<>(); // uuid <-> Blob fragmentat, zona de tranzitie pana la primirea completa
						// a tuturor fragmentelor
    Map<String, Blob> currentCacheContent = new HashMap<>(); // Blob-uri complete

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {

	if (args.length != 2) {
	    String usage = "Usage:\n";
	    usage += "\tjava -cp bin test.com.github.dosarudaniel.gsoc.MulticastServer ";
	    usage += "<IP> <PORT_NUMBER> ";
	    System.out.println(usage);
	    return;
	}

	String ip_address = args[0];
	int portNumber = Integer.parseInt(args[1]);

	Handler fh = new FileHandler("%t/ALICE_MulticastServer_log");
	Logger.getLogger("").addHandler(fh);

	byte[] buf = new byte[Utils.PACKET_MAX_SIZE];

	Blob blobReceived = new Blob();

	try (MulticastSocket socket = new MulticastSocket(portNumber)) {
	    InetAddress group = InetAddress.getByName(ip_address);
	    socket.joinGroup(group);
	    int number = -1;
	    while (true) {
		// Receive object
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		logger.log(Level.INFO, "Receive..");
		socket.receive(packet);

		FragmentedBlob fragmentedBlob = new FragmentedBlob(buf, packet.getLength());

//		System.out.println("Received:" + new String(fragmentedBlob.getPayload()));

		// choose the blob to put the fragmentedBlob
		if (blobReceived.getKey().equals("")) {
//		    System.out.println("Received key:" + fragmentedBlob.getKey());
//		    System.out.println(" payload :" + new String(fragmentedBlob.getPayload()).substring(0, 3));

		    blobReceived.setKey(fragmentedBlob.getKey());
		    blobReceived.setUuid(fragmentedBlob.getUuid());
		}

		blobReceived.addFragmentedBlob(fragmentedBlob);

		if (blobReceived.isComplete()) {
		    // System.out.println(blobReceived);
		    int indexOfSpace = new String(blobReceived.getPayload()).indexOf(' ');
		    String seqNumber = new String(blobReceived.getPayload()).substring(0, indexOfSpace);
		    int intSeqNumber = Integer.parseInt(seqNumber);
		    if (intSeqNumber != number + 1) {
			// System.out.println("---- " + intSeqNumber + " vs " + Integer.toString(number
			// + 1));
			System.out.println("-- You lost Blob nr " + Integer.toString(number + 1));
		    }
		    number = intSeqNumber;
		    System.out.println("Blob nr : " + seqNumber + " is complete.");
		    blobReceived = new Blob();
		}

	    }
	    // socket.leaveGroup(group);
	}

    }

    // Thread incompleteBlobRecovery = new Thread();
    // daca inFlight contine obiecte care nu au fost atinse de mai mult de X millis
    // => recovery procedure pentru ele
}
