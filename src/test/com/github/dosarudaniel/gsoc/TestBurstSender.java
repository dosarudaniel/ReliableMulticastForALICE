/**
 * test.com.github.dosarudaniel.gsoc provides the classes necessary to test the
 * Sender and Receiver from the myjava.com.github.dosarudaniel.gsoc package
 */
package test.com.github.dosarudaniel.gsoc;

import java.io.IOException;

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
	if (args.length != 5) {
	    String usage = args.length + "Usage:\n";
	    usage += "\tjava -cp bin test.com.github.dosarudaniel.gsoc.TestBurstSender ";
	    usage += "<IP>  <PORT_NUMBER>  ";
	    usage += "<MAX_PAYLOAD_SIZE>  <RATE>  <TIME_TO_RUN>";
	    System.out.println(usage);
	    return;
	}

	BurstSender bS = new BurstSender(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]),
		Integer.parseInt(args[3]), Integer.parseInt(args[4]));
	bS.work();
    }
}
