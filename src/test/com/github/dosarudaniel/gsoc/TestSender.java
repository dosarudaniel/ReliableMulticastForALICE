/**
 * test.com.github.dosarudaniel.gsoc provides the classes necessary to test the
 * Sender and Receiver from the myjava.com.github.dosarudaniel.gsoc package
 */
package test.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.util.Timer;

import myjava.com.github.dosarudaniel.gsoc.Sender;

/**
 * Test the Sender unit by scheduling a send multicast action every
 * TIME_INTERVAL_SECONDS
 * 
 * @author dosarudaniel@gmail.com
 * @since 2019-03-07
 *
 */
public class TestSender {
    static final int TIME_INTERVAL_SECONDS = 60;

    /**
     * Schedules a sender to send a multicast message every TIME_INTERVAL_SECONDS
     *
     * @param args multicastIpAddress and portNumber
     * @throws IOException
     * @throws SecurityException
     * @throws NumberFormatException
     */
    public static void main(String[] args) throws NumberFormatException, SecurityException, IOException {

	if (args.length != 7) {
	    String usage = "Usage:\n";
	    usage += "\tjava -cp bin test.com.github.dosarudaniel.gsoc.TestSender ";
	    usage += "<IP> <PORT_NUMBER> ";
	    usage += "<FRAGMENT_MAX_PAYLOAD_SIZE> ";
	    usage += "<KEY_LENGTH> <METADATA_LENGTH> <PAYLOAD_LENGTH> <NR_OF_PACKETS_TO_BE_SENT>\n\n";
	    usage += "Example:\n";
	    usage += "\tjava -cp bin test.com.github.dosarudaniel.gsoc.TestSender 230.0.0.0 5000 512 50 150 1024 100\n";
	    usage += "\t |\n";
	    usage += "\t --> Sends 100 packets with payload size 1024 B, metadata size 150 B, key length 50 B to the multicast address 230.0.0.0 on port 5000. ";
	    System.out.println(usage);
	    return;
	}

	Timer timer = new Timer();
	// java TestSender <IP> <PORT_NUMBER> <NR_OF_PACKETS_TO_BE_SENT>
	// <MAX_PAYLOAD_SIZE>
	// Ex: java TestSender 230.0.0.0 5000 200 1024

	Sender sender = new Sender(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]),
		Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]),
		Integer.parseInt(args[6]));
	sender.work();
    }
}
