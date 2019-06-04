package myjava.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BurstSender extends TimerTask {
    private static Logger logger;
    private String ip_address;
    private int portNumber;
    private int payloadLength;
    private int rate;
    public static int nrPacketsSent = 0;

    private Thread thread = new Thread(new Runnable() {
	@Override
	public void run() {
	    int oldNrPacketsSent = 0;
	    while (true) {
		try {
		    Thread.sleep(1000);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
		logger.log(Level.INFO, "Sent " + (BurstSender.nrPacketsSent - oldNrPacketsSent)
			+ " packets per second. \n" + "Total " + BurstSender.nrPacketsSent);
		oldNrPacketsSent = BurstSender.nrPacketsSent;
	    }
	}
    });

    public static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public BurstSender(String ip_address, int portNumber, int payloadLength, int rate)
	    throws SecurityException, IOException {
	super();
	this.rate = rate;
	this.ip_address = ip_address;
	this.portNumber = portNumber;
	this.payloadLength = payloadLength;
	System.out.println("--- " + this.getClass().getCanonicalName());
	logger = Logger.getLogger(this.getClass().getCanonicalName());

	Handler fh = new FileHandler("%t/ALICE_MulticastBurstSender_log");
	Logger.getLogger(this.getClass().getCanonicalName()).addHandler(fh);

    }

    @Override
    public void run() {
	String payload = randomString(this.payloadLength);

	byte[] packet = payload.getBytes(Charset.forName(Utils.CHARSET));

	long milis = 1000 / this.rate;
	int nanos = (1_000_000_000 / this.rate) % 1_000_000;

	System.out.println(milis + " ms " + nanos + " ns");

	try (DatagramSocket socket = new DatagramSocket()) {
	    InetAddress group = InetAddress.getByName(this.ip_address);
	    DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, group, this.portNumber);
	    this.thread.start();

	    while (true) {
		Thread.sleep(milis, nanos);
		socket.send(datagramPacket);
		nrPacketsSent++;
	    }
	} catch (SocketException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
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

}
