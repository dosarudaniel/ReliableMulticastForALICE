/**
 * myjava.com.github.dosarudaniel.gsoc provides the classes necessary to send/
 * receive multicast messages which contains Blob object with random length,
 * random content payload.
 */
package myjava.com.github.dosarudaniel.gsoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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

    /**
     * Parameterized constructor
     *
     * @param ip_address
     * @param portNumber
     */
    public Sender(String ip_address, int portNumber, int nrOfPacketsToBeSent, int maxPayloadSize, int keyLength,
	    int metadataLength, int payloadLength) {
	super();
	this.ip_address = ip_address;
	this.portNumber = portNumber;
	this.nrOfPacketsToBeSent = nrOfPacketsToBeSent;
	this.maxPayloadSize = maxPayloadSize;
	this.keyLength = keyLength;
	this.metadataLength = metadataLength;
	this.payloadLength = payloadLength;
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
	String metadata = randomString(this.metadataLength);

	// generate a random length, random content payload
	// randomNumber = ThreadLocalRandom.current().nextInt(MIN_LEN_DATA,
	// MAX_LEN_DATA);
	String payload = randomString(this.payloadLength);

	// generate a random length, random content key
	// randomNumber = ThreadLocalRandom.current().nextInt(MIN_LEN_KEY, MAX_LEN_KEY);
	String key = randomString(this.keyLength);

//	System.out.println("Blob key = " + key);
//	System.out.println("Blob metadata = " + metadata);
//	System.out.println("Blob payload = " + payload);
	UUID uuid = UUID.randomUUID();
	Blob blob = null;

	for (int i = 0; i < this.nrOfPacketsToBeSent; i++) {
	    String payload_with_number = Integer.toString(i) + " " + payload;
	    String metadata_with_number = Integer.toString(i) + " " + metadata;

	    try {
		blob = new Blob(metadata_with_number.getBytes(Charset.forName(Utils.CHARSET)),
			payload_with_number.getBytes(Charset.forName(Utils.CHARSET)), key, uuid);
		blob.send(this.maxPayloadSize, this.ip_address, this.portNumber);
	    } catch (NoSuchAlgorithmException | IOException e) {
		e.printStackTrace();
	    }
	    String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
	    System.out.println("[" + timeStamp + "] Blob nr: " + Integer.toString(i) + " sent.");
	}

    }

    /**
     * Sends multicast message that contains the serialized version of an object of
     * type Blob
     *
     * @param blob - Object to send
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public void sendMulticast(Blob blob) throws IOException, NoSuchAlgorithmException {
	try (DatagramSocket socket = new DatagramSocket()) {
	    InetAddress group = InetAddress.getByName(this.ip_address);
	    byte[] buf = serialize(blob);
	    DatagramPacket packet = new DatagramPacket(buf, buf.length, group, this.portNumber);
	    socket.send(packet);
	}
    }

    /**
     * Generates a random content string of length len
     *
     * @param len - Length of the randomString
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
     * @param obj - Object to be serialized
     * @return byte[] the serialized version of the object
     * @throws IOException
     */
    public static byte[] serialize(Object obj) throws IOException {
	try (ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out)) {
	    os.writeObject(obj);
	    os.flush();
	    byte[] b = out.toByteArray();
	    return b;
	}
    }
}
