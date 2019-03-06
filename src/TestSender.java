import java.util.Timer;

public class TestSender {
	static final int TIME_INTERVAL_SECONDS = 10;

	public static void main(String[] args) {
		Timer timer = new Timer();
		timer.schedule(new Sender("230.0.0.0", 4446), 0, TIME_INTERVAL_SECONDS * 1000);
	}
}
