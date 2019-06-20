/**
 * myjava.com.github.dosarudaniel.gsoc provides the classes necessary to send/
 * receive multicast messages which contains Blob object with random length,
 * random content payload.
 */
package myjava.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;

import ch.alice.o2.ccdb.servlets.Local;
import ch.alice.o2.ccdb.servlets.LocalBrowse;
import ch.alice.o2.ccdb.webserver.EmbeddedTomcat;

/**
 * Sender unit which generates and sends new objects at fixed time intervals
 * (10s) and at the same time print the current time and message content on the
 * screen
 *
 * @author dosarudaniel@gmail.com
 * @since 2019-03-07
 *
 */
public class Sender {
    private Map<String, Blob> blobMap = new HashMap<>();

    private SingletonLogger singletonLogger = new SingletonLogger();
    private Logger logger = this.singletonLogger.getLogger();

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

  //   public Thread recoveryThread = new Thread(new Runnable() {
  //
	// @Override
	// public void run() {
	//     EmbeddedTomcat tomcat;
  //
	//     try {
	// 	tomcat = new EmbeddedTomcat("localhost");
	//     } catch (final ServletException se) {
	// 	System.err.println("Cannot create the Tomcat server: " + se.getMessage());
	// 	return;
	//     }
  //
	//     final Wrapper browser = tomcat.addServlet(LocalBrowse.class.getName(), "/browse/*");
	//     browser.addMapping("/latest/*");
	//     tomcat.addServlet(Local.class.getName(), "/*");
  //
	//     // Start the server
	//     try {
	// 	tomcat.start();
	//     } catch (final LifecycleException le) {
	// 	System.err.println("Cannot start the Tomcat server: " + le.getMessage());
	// 	return;
	//     }
  //
	//     if (tomcat.debugLevel >= 1)
	// 	System.err.println("Ready to accept HTTP calls on " + tomcat.address + ":" + tomcat.getPort()
	// 		+ ", file repository base path is: " + Local.basePath);
  //
	//     tomcat.blockWaiting();
	// }
  //   });

    private Thread counterThread = new Thread(new Runnable() {
	private SingletonLogger singletonLogger2 = new SingletonLogger();
	private Logger logger2 = this.singletonLogger2.getLogger();

	@Override
	public void run() {
	    int oldNrPacketsSent = 0;
	    while (Sender.counterRunning) {
		try {
		    Thread.sleep(1000);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
		if (Sender.nrPacketsSent - oldNrPacketsSent > 0) {
		    this.logger2.log(Level.INFO, "Sent " + (Sender.nrPacketsSent - oldNrPacketsSent)
			    + " packets per second. \n" + "Total " + Sender.nrPacketsSent);

		    oldNrPacketsSent = Sender.nrPacketsSent;
		}
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
    }

    /**
     * Creates an object with a random length, random content payload. Calls the
     * sendMulticast method every (default 10) seconds. Prints timestamp and the
     * payload that was sent.
     *
     */

    public void work() {
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

	this.counterThread.start();
//	this.recoveryThread.start();
	counterRunning = true;
	for (int i = 0; i < this.nrOfPacketsToBeSent; i++) {

	    nrPacketsSent++;
//	    String payload_with_number = Integer.toString(i) + " " + payload;
//	    String metadata_with_number = Integer.toString(i) + " " + metadata;
	    // logger.log(Level.INFO, "Sending packet nr " + i);
	    try {
		blob = new Blob(metadata.getBytes(Charset.forName(Utils.CHARSET)),
			payload.getBytes(Charset.forName(Utils.CHARSET)), key, uuid);
		//System.out.println(blob);
		blobMap.put(key, blob);
		blob.send(this.ip_address, this.portNumber);

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
	    this.counterThread.join();
	  //  this.recoveryThread.join();
	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	// System.out.println(blobMap);

    }
}
