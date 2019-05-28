/**
 * test.com.github.dosarudaniel.gsoc provides the classes necessary to test the
 * Sender and Receiver from the myjava.com.github.dosarudaniel.gsoc package
 */
package test.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import myjava.com.github.dosarudaniel.gsoc.Receiver;

/**
 * Test the Receiver unit
 * 
 * @author dosarudaniel@gmail.com
 * @since 2019-03-07
 *
 */
public class TestReceiver {
    /**
     * Creates a Receiver unit and calls the work method in order to receive
     * multicast messages if the message with content "end" is received, the
     * receiver stops.
     * 
     * @param args multicastIpAddress and portNumber
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NoSuchAlgorithmException
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
	// java TestReceiver <IP> <PORT_NUMBER>
	// Ex: java TestReceiver 230.0.0.0 5000
	Receiver receiver = new Receiver(args[0], Integer.parseInt(args[1]));
	receiver.work();
    }
}
