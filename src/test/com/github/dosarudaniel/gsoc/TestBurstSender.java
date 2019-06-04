/**
 * test.com.github.dosarudaniel.gsoc provides the classes necessary to test the
 * Sender and Receiver from the myjava.com.github.dosarudaniel.gsoc package
 */
package test.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.util.Timer;

import myjava.com.github.dosarudaniel.gsoc.BurstSender;

/**
 * Test the BurstSender unit by scheduling a send NR_OF_PACKETS_TO_BE_SENT
 * multicast messages every TIME_INTERVAL_SECONDS
 * 
 * @author dosarudaniel@gmail.com
 * @since 2019-03-07
 *
 */
public class TestBurstSender {
    static final int TIME_INTERVAL_SECONDS = 60;

    /**
     * Schedules a BurstSender to send NR_OF_PACKETS_TO_BE_SENT multicast messages
     * every TIME_INTERVAL_SECONDS
     *
     * @param args multicastIpAddress and portNumber
     * @throws IOException
     * @throws SecurityException
     * @throws NumberFormatException
     */
    public static void main(String[] args) throws NumberFormatException, SecurityException, IOException {
	if (args.length != 4) {
	    String usage = args.length + "Usage:\n";
	    usage += "\tjava -cp bin test.com.github.dosarudaniel.gsoc.TestBurstSender ";
	    usage += "<IP>  <PORT_NUMBER>  ";
	    usage += "<MAX_PAYLOAD_SIZE>  <RATE>";
	    System.out.println(usage);
	    return;
	}

	Timer timer = new Timer();

	timer.schedule(new BurstSender(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]),
		Integer.parseInt(args[3])), 0, TIME_INTERVAL_SECONDS * 1000L);
    }
}
