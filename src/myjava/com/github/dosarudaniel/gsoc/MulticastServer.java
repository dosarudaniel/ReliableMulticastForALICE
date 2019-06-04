package myjava.com.github.dosarudaniel.gsoc;

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

public class MulticastServer {
    private static Logger logger;
    static final int MIN_LEN = 50;
    static final int MAX_LEN = 130;

    private String ip_address;
    private int portNumber;

    private Map<UUID, Blob> inFlight; // uuid <-> Blob fragmentat, zona de tranzitie pana la primirea
				      // completa a tuturor fragmentelor
    private Map<String, Blob> currentCacheContent; // Blob-uri complete

    public MulticastServer(String ip_address, int portNumber) throws SecurityException, IOException {
	this.ip_address = ip_address;
	this.portNumber = portNumber;
	logger = Logger.getLogger(this.getClass().getCanonicalName());
	Handler fh = new FileHandler("%t/ALICE_MulticastServer_log");
	Logger.getLogger(this.getClass().getCanonicalName()).addHandler(fh);

	this.inFlight = new HashMap<>();
	this.currentCacheContent = new HashMap<>();
    }

    public void work() throws IOException, NoSuchAlgorithmException {
	byte[] buf = new byte[Utils.PACKET_MAX_SIZE];

	Blob blob = null;

	try (MulticastSocket socket = new MulticastSocket(this.portNumber)) {
	    InetAddress group = InetAddress.getByName(this.ip_address);
	    socket.joinGroup(group);

	    while (true) {
		try {
		    // Receive object
		    DatagramPacket packet = new DatagramPacket(buf, buf.length);
		    socket.receive(packet);

		    FragmentedBlob fragmentedBlob = new FragmentedBlob(buf, packet.getLength());

		    blob = this.inFlight.get(fragmentedBlob.getUuid());
		    if (blob == null) {
			blob = new Blob();
			blob.setKey(fragmentedBlob.getKey());
			blob.setUuid(fragmentedBlob.getUuid());
		    }

		    blob.addFragmentedBlob(fragmentedBlob);

		    if (blob.isComplete()) {
			// Add the complete Blob to the cache
			System.out.println("Received " + blob);
			this.currentCacheContent.put(blob.getKey(), blob);
			logger.log(Level.INFO, "Complete blob with key " + blob.getKey() + " was added to the cache.");

			// Remove the blob from inFlight
			if (this.inFlight.remove(blob.getUuid()) == null) {
			    // If you get a SMALL_BLOB this statement will be logged
			    logger.log(Level.WARNING,
				    "Complete blob " + blob.getUuid() + " was not added to the inFlight");
			}
		    } else {
			// Update inFlight
			this.inFlight.put(blob.getUuid(), blob);
			logger.log(Level.INFO, "Update inFlight blob " + blob);
		    }

		} catch (Exception e) {
		    logger.log(Level.WARNING, "Exception thrown");
		    e.printStackTrace();
		}
	    }
	    // socket.leaveGroup(group);
	}
    }

    // Thread incompleteBlobRecovery = new Thread();
    // daca inFlight contine obiecte care nu au fost atinse de mai mult de X millis
    // => recovery procedure pentru ele
}
