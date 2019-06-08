/**
 * myjava.com.github.dosarudaniel.gsoc provides the classes necessary to send/
 * receive multicast messages which contains Blob object with random length,
 * random content payload.
 */
package myjava.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.NoSuchAlgorithmException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Receiver class waits for DatagramPacket from the Sender and prints the
 * timestamp and the content of the packet is the checksum is correct
 *
 * @author dosarudaniel@gmail.com
 * @since 2019-03-07
 *
 */
public class Receiver {
    SingletonLogger singletonLogger = new SingletonLogger();
    Logger logger = this.singletonLogger.getLogger();
    private final static int BUF_SIZE = 65536;
    private byte[] buf = new byte[BUF_SIZE];
    private String ip_address;
    private int portNumber;

    public static int nrPacketsReceived = 0;

    private Thread thread = new Thread(new Runnable() {
	SingletonLogger singletonLogger2 = new SingletonLogger();
	Logger logger2 = this.singletonLogger2.getLogger();

	@Override
	public void run() {
	    int oldNrPacketsReceived = 0;
	    while (true) {
		try {
		    Thread.sleep(1000);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
		if (Receiver.nrPacketsReceived - oldNrPacketsReceived > 0) {
		    this.logger2.log(Level.INFO, "Received " + (Receiver.nrPacketsReceived - oldNrPacketsReceived)
			    + " packets per second. \n" + "Total " + Receiver.nrPacketsReceived);
		    oldNrPacketsReceived = Receiver.nrPacketsReceived;
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
    public Receiver(String ip_address, int portNumber) throws SecurityException, IOException {
	super();
	this.ip_address = ip_address;
	this.portNumber = portNumber;

	logger = Logger.getLogger(this.getClass().getCanonicalName());
	Handler fh = new FileHandler("%t/ALICE_MulticastReceiver_log");
	Logger.getLogger(this.getClass().getCanonicalName()).addHandler(fh);
    }

    /**
     * Receive multicast packets Opens a multicast socket and it sets that socket to
     * a multicast group. Receives continuously DatagramPackets through the
     * multicast DatagramSocket Every DatagramPacket is deserialized and by calling
     * the getPayload method every message's checksum is verified. The receiver
     * prints the current timestamp and the content of the message if the message
     * with the content "end" is received, the Receiver unit stops.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws Exception
     */
    public void work() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
	try (MulticastSocket socket = new MulticastSocket(this.portNumber)) {
	    InetAddress group = InetAddress.getByName(this.ip_address);
	    socket.joinGroup(group);
	    int nr_packets_received = 0;
	    DatagramPacket packet = new DatagramPacket(this.buf, this.buf.length);
	    this.thread.start();
	    while (true) {
		// Receive object
		socket.receive(packet);
		this.nrPacketsReceived++;
	    }
	} catch (Exception e) {
	    this.logger.log(Level.SEVERE, "Could not create a MulticastSocket.");
	    e.printStackTrace();
	}
    }

    // Create a new thread that reads once per second nrPacketsReceived variable
    // syso(nrPacketsReceived) and reset it: nrPacketsReceived = 0
}
