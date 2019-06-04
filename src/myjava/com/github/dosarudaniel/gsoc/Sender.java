/**
 * myjava.com.github.dosarudaniel.gsoc provides the classes necessary to send/
 * receive multicast messages which contains Blob object with random length,
 * random content payload.
 */
package myjava.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * Sender unit which generates and sends new objects at fixed time intervals
 * (10s) and at the same time print the current time and message content on the
 * screen
 * 
 * @author dosarudaniel@gmail.com
 * @since 2019-03-07
 *
 */
public class Sender extends TimerTask {
    private static Logger logger;

    private String ip_address;
    private int portNumber;

    public static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public static final int MIN_LEN_KEY = 20;
    public static final int MAX_LEN_KEY = 100;
    public static final int MIN_LEN_METADATA = 1000;
    public static final int MAX_LEN_METADATA = 3000;
    public static final int MIN_LEN_DATA = 30_000;
    public static final int MAX_LEN_DATA = 10_0000;

    private int keyLength;
    private int metadataLength;
    private int payloadLength;

    private int maxPayloadSize;
    private int nrOfPacketsToBeSent;

    public static int nrPacketsSent = 0;
    public static boolean counterRunning = false;

    private Thread thread = new Thread(new Runnable() {
	@Override
	public void run() {
	    int oldNrPacketsSent = 0;
	    while (Sender.counterRunning) {
		try {
		    Thread.sleep(1000);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
//		logger.log(Level.INFO, "Sent " + (Sender.nrPacketsSent - oldNrPacketsSent) + " packets per second. \n"
//			+ "Total " + Sender.nrPacketsSent);
		oldNrPacketsSent = Sender.nrPacketsSent;
	    }
	}
    });

    /**
     * Parameterized constructor
     *
     * @param ip_address
     * @param portNumber
     * @throws IOException
     * @throws SecurityException
     */
    public Sender(String ip_address, int portNumber, int maxPayloadSize, int keyLength, int metadataLength,
	    int payloadLength, int nrOfPacketsToBeSent) throws SecurityException, IOException {
	super();
	this.ip_address = ip_address;
	this.portNumber = portNumber;
	this.nrOfPacketsToBeSent = nrOfPacketsToBeSent;
	this.maxPayloadSize = maxPayloadSize;
	this.keyLength = keyLength;
	this.metadataLength = metadataLength;
	this.payloadLength = payloadLength;
	logger = Logger.getLogger(this.getClass().getCanonicalName());
	Handler fh = new FileHandler("%t/ALICE_MulticastSender_log");
	Logger.getLogger(this.getClass().getCanonicalName()).addHandler(fh);
    }

    /**
     * Creates an object with a random length, random content payload. Calls the
     * sendMulticast method every (default 10) seconds. Prints timestamp and the
     * payload that was sent.
     *
     */
    @Override
    public void run() {
	// generate a random length, random content metadata
	// int randomNumber = ThreadLocalRandom.current().nextInt(MIN_LEN_METADATA,
	// MAX_LEN_METADATA);

	String metadata = Utils.randomString(this.metadataLength);

	// generate a random length, random content payload
	// randomNumber = ThreadLocalRandom.current().nextInt(MIN_LEN_DATA,
	// MAX_LEN_DATA);
	String payload = Utils.randomString(this.payloadLength);

	// generate a random length, random content key
	// randomNumber = ThreadLocalRandom.current().nextInt(MIN_LEN_KEY, MAX_LEN_KEY);
	String key = Utils.randomString(this.keyLength);

//	System.out.println("Blob key = " + key);
//	System.out.println("Blob metadata = " + metadata);
//	System.out.println("Blob payload = " + payload);
	UUID uuid = UUID.randomUUID();
	Blob blob = null;

	this.thread.start();
	counterRunning = true;
	for (int i = 0; i < this.nrOfPacketsToBeSent; i++) {

	    nrPacketsSent++;
//	    String payload_with_number = Integer.toString(i) + " " + payload;
//	    String metadata_with_number = Integer.toString(i) + " " + metadata;
	    // logger.log(Level.INFO, "Sending packet nr " + i);
	    try {
		blob = new Blob(metadata.getBytes(Charset.forName(Utils.CHARSET)),
			payload.getBytes(Charset.forName(Utils.CHARSET)), key, uuid);
		blob.send(this.maxPayloadSize, this.ip_address, this.portNumber);

	    } catch (NoSuchAlgorithmException | IOException e) {
		e.printStackTrace();
	    }
	    payload = Utils.randomString(this.payloadLength);
	    metadata = Utils.randomString(this.metadataLength);
	    key = Utils.randomString(this.keyLength);
	    uuid = UUID.randomUUID();
	}

	counterRunning = false;
	try {
	    this.thread.join();
	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

    }
}
