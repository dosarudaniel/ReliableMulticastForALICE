import java.io.IOException;

public class TestReceiver {
	public static void main(String[] args) throws IOException {
		Receiver receiver = new Receiver("230.0.0.0", 4446);
		receiver.run();
	}
}
