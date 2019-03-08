
import java.util.Timer;

/**
 * @author dosarudaniel@gmail.com
 * @since 2019-03-07
 *
 */
public class TestSender {
	static final int TIME_INTERVAL_SECONDS = 10;

	/**
	 * Schedules a sender to send a multicast message every TIME_INTERVAL_SECONDS
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		Timer timer = new Timer();
		// java TestSender <IP> <PORT_NUMBER>
		// Ex: java TestSender 230.0.0.0 5000
		timer.schedule(new Sender(args[0], Integer.parseInt(args[1])), 0, TIME_INTERVAL_SECONDS * 1000);
	}
}
