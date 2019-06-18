package myjava.com.github.dosarudaniel.gsoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import myjava.com.github.dosarudaniel.gsoc.Utils.Pair;

public class MulticastReceiver {
    private SingletonLogger singletonLogger = new SingletonLogger();
    private Logger logger = this.singletonLogger.getLogger();

    static final int MIN_LEN = 50;
    static final int MAX_LEN = 130;
    static final int DELTA_T = 1000;
    static final int HTTP_RESPONSE_CODE_OK = 200;
    static final int HTTP_RESPONSE_CODE_PARTIAL_CONTENT = 206;

    private String ip_address;
    private int portNumber;

    public static int nrPacketsReceived = 0;

    private Map<UUID, Blob> inFlight; // uuid <-> Blob fragmentat, zona de tranzitie pana la primirea
    // completa a tuturor fragmentelor
    private Map<String, Blob> currentCacheContent; // Blob-uri complete

    public MulticastReceiver(String ip_address, int portNumber) throws SecurityException {
	this.ip_address = ip_address;
	this.portNumber = portNumber;
	this.inFlight = new ConcurrentHashMap<>();
	this.currentCacheContent = new HashMap<>();
    }

    private Thread counterThread = new Thread(new Runnable() {
	private SingletonLogger singletonLogger2 = new SingletonLogger();
	private Logger logger2 = this.singletonLogger2.getLogger();

	@Override
	public void run() {
	    int oldNrPacketsReceived = 0;
	    while (true) {
		try {
		    Thread.sleep(1000);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}

		if (MulticastReceiver.nrPacketsReceived - oldNrPacketsReceived > 0) {
		    this.logger2.log(Level.INFO,
			    "Received " + (MulticastReceiver.nrPacketsReceived - oldNrPacketsReceived)
				    + " packets per second. \n" + "Total " + MulticastReceiver.nrPacketsReceived);
		    oldNrPacketsReceived = MulticastReceiver.nrPacketsReceived;
		}
	    }
	}
    });

    private Thread incompleteBlobRecovery = new Thread(new Runnable() {
	@Override
	public void run() {

	    for (Map.Entry<UUID, Blob> entry : inFlight.entrySet()) {
		UUID uuid = entry.getKey();
		Blob blob = entry.getValue();
		Timestamp timestamp = blob.getTimestamp();

		Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());

		if (currentTimestamp.getTime() > blob.getTimestamp().getTime() + DELTA_T) {
		    // ArrayList<Pair> metadataMissingBlocks = blob.getMissingMetadataBlocks();
		    // TODO: Metadata recovery

		    ArrayList<Pair> payloadMissingBlocks = blob.getMissingPayloadBlocks();
		    if (payloadMissingBlocks == null) {
			// Recover the entire Blob
			try {
			    URL url = new URL("http://localhost:8080/Task/Detector/1/" + blob.getUuid().toString());
			    HttpURLConnection con = (HttpURLConnection) url.openConnection();
			    con.setRequestMethod("GET");

			    con.setConnectTimeout(5000); // server should be fast (< 5 sec)
			    con.setReadTimeout(5000);

			    int status = con.getResponseCode();
			    if (status == HTTP_RESPONSE_CODE_OK) {
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer content = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
				    content.append(inputLine);
				}
				blob.addByteRange(content.toString().getBytes(),
					new Pair(0, content.toString().getBytes().length));

				in.close();
			    } else {
				// TODO retry?
			    }
			} catch (Exception e) {
			    e.printStackTrace();
			}

		    } else {
			String ranges = "";
			int i = 0;
			for (i = 0; i < payloadMissingBlocks.size() - 1; i++) {
			    ranges += payloadMissingBlocks.get(i).first;
			    ranges += "-";
			    ranges += payloadMissingBlocks.get(i).second;
			    ranges += ",";
			}
			ranges += payloadMissingBlocks.get(i).first;
			ranges += "-";
			ranges += payloadMissingBlocks.get(i).second;

			try {
			    URL url = new URL("http://localhost:8080/Task/Detector/1/" + blob.getUuid().toString());
			    HttpURLConnection con = (HttpURLConnection) url.openConnection();
			    con.setRequestProperty("Range", "bytes=" + ranges);
			    con.setRequestMethod("GET");

			    con.setConnectTimeout(5000); // server should be fast (< 5 sec)
			    con.setReadTimeout(5000);

			    int status = con.getResponseCode();
			    if (status == HTTP_RESPONSE_CODE_PARTIAL_CONTENT) {
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer content = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
				    content.append(inputLine);
				}

				// blob.setPayload(System.arraycopy(blob.getPayload(), ));
				// Parse de response and get only the byte ranges without
				// "--THIS_STRING_SEPARATES_5f8a3c40-8e7a-11e9-8e66-112233445566"

				// blob.addByteRange(content.toString().getBytes(), new Pair(0,
				// content.toString().getBytes().length));
				System.out.println(content.toString());

				in.close();
			    } else {
				// TODO: retry?
			    }

			} catch (Exception e) {
			    e.printStackTrace();
			}
		    }
		}
	    }
	}
    });

    public void work() throws IOException {
	byte[] buf = new byte[Utils.PACKET_MAX_SIZE];
	Blob blob = null;

	try (MulticastSocket socket = new MulticastSocket(this.portNumber)) {
	    InetAddress group = InetAddress.getByName(this.ip_address);
	    socket.joinGroup(group);
	    this.counterThread.start();
	    while (true) {
		try {
		    // Receive object
		    DatagramPacket packet = new DatagramPacket(buf, buf.length);
		    socket.receive(packet);

		    FragmentedBlob fragmentedBlob = new FragmentedBlob(buf, packet.getLength());
		    if (this.currentCacheContent != null) {
			if (this.currentCacheContent.remove(fragmentedBlob.getKey()) != null) {
			    this.logger.log(Level.INFO,
				    "Blob with key " + fragmentedBlob.getKey() + " was removed from the cache.");
			}
		    }
		    blob = this.inFlight.get(fragmentedBlob.getUuid());
		    if (blob == null) {
			blob = new Blob(fragmentedBlob.getKey(), fragmentedBlob.getUuid());
		    }

		    blob.addFragmentedBlob(fragmentedBlob);

		    if (blob.isComplete()) {
			System.out.println(blob);
			nrPacketsReceived++;
			// Add the complete Blob to the cache
			this.currentCacheContent.put(blob.getKey(), blob);
			this.logger.log(Level.INFO,
				"Complete blob with key " + blob.getKey() + " was added to the cache.");

			// Remove the blob from inFlight
			if (this.inFlight.remove(blob.getUuid()) == null) {
			    // If you get a SMALL_BLOB this statement will be logged
			    this.logger.log(Level.WARNING,
				    "Complete blob " + blob.getUuid() + " was not added to the inFlight");
			}
		    } else {
			// Update inFlight
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			// Update Blob timestamp
			blob.setTimestamp(timestamp);
			this.inFlight.put(blob.getUuid(), blob);
			// logger.log(Level.INFO, "Update inFlight blob " + blob);
		    }
		} catch (Exception e) {
		    // logger.log(Level.WARNING, "Exception thrown");
		    e.printStackTrace();
		}
	    }
	    // socket.leaveGroup(group);
	}
    }
}
