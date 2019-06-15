package test.com.github.dosarudaniel.gsoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import myjava.com.github.dosarudaniel.gsoc.Blob;
import myjava.com.github.dosarudaniel.gsoc.FragmentedBlob;
import myjava.com.github.dosarudaniel.gsoc.Utils;

public class BasicTests {

    byte[] buf = new byte[Utils.PACKET_MAX_SIZE];
    static String ip = "230.0.0.0";
    static int port = 5001;

    // Preparing data to be send
    static String metadata = Utils.randomString(20);
    static String payload = Utils.randomString(40);
    static String key = Utils.randomString(6);
    static UUID uuid = UUID.randomUUID();

    static Blob blobToBeSent;

    static Thread sender = new Thread(new Runnable() {
	@Override
	public void run() {
	    // TODO Auto-generated method stub
	    try {
		blobToBeSent.send(ip, port);
	    } catch (NoSuchAlgorithmException | IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    });

    static Thread sender2 = new Thread(new Runnable() {
	@Override
	public void run() {
	    // TODO Auto-generated method stub
	    try {
		blobToBeSent.send(ip, port);
	    } catch (NoSuchAlgorithmException | IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    });

    static Thread receiver = new Thread(new Runnable() {
	@Override
	public void run() {
	    // TODO Auto-generated method stub
	    Blob blobTobeReceived = null;
	    byte[] buf = new byte[Utils.PACKET_MAX_SIZE];
	    try (MulticastSocket socket = new MulticastSocket(port)) {
		InetAddress group = InetAddress.getByName(ip);
		socket.joinGroup(group);

		// Receive object
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);
		FragmentedBlob fragmentedBlob = new FragmentedBlob(buf, packet.getLength());
		blobTobeReceived = new Blob(fragmentedBlob.getKey(), fragmentedBlob.getUuid());
		blobTobeReceived.addFragmentedBlob(fragmentedBlob);
		if (blobTobeReceived.isComplete() && blobTobeReceived.equals(blobToBeSent)) {
		    System.out.println("Test nr 1: ............... PASSED (Sending a SMALL BLOB)");
		} else {
		    System.out.println("Test nr 1: ............... FAILED (Sending a SMALL BLOB)");
		}
	    } catch (IOException | NoSuchAlgorithmException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    });

    static Thread receiver2 = new Thread(new Runnable() {
	@Override
	public void run() {
	    System.out.println("aici");
	    // TODO Auto-generated method stub
	    Blob blobTobeReceived = null;
	    byte[] buf = new byte[Utils.PACKET_MAX_SIZE];
	    try (MulticastSocket socket = new MulticastSocket(port)) {
		InetAddress group = InetAddress.getByName(ip);
		socket.joinGroup(group);

		// Receive object
		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		socket.receive(packet);
		FragmentedBlob fragmentedBlob = new FragmentedBlob(buf, packet.getLength());
		blobTobeReceived = new Blob(fragmentedBlob.getKey(), fragmentedBlob.getUuid());
		blobTobeReceived.addFragmentedBlob(fragmentedBlob);

		System.out.println("aici2");
		while (!blobTobeReceived.isComplete()) {
		    System.out.println("aici3");
		    socket.receive(packet);
		    fragmentedBlob = new FragmentedBlob(buf, packet.getLength());
		    blobTobeReceived = new Blob(fragmentedBlob.getKey(), fragmentedBlob.getUuid());
		    blobTobeReceived.addFragmentedBlob(fragmentedBlob);
		    System.out.println(blobTobeReceived);
		}

		System.out.println("aici4");
		if (blobTobeReceived.equals(blobToBeSent)) {
		    System.out.println(
			    "Test nr 2: ............... PASSED (Sending a bigger BLOB - fragmentation necessary)");
		} else {
		    System.out.println(
			    "Test nr 2: ............... FAILED (Sending a bigger BLOB - fragmentation necessary)");
		}
	    } catch (IOException | NoSuchAlgorithmException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    });

    public static void main(String[] args)
	    throws IOException, NoSuchAlgorithmException, SecurityException, InterruptedException {

	// String command = "curl -i
	// localhost:8080/Task/Detector/1/b4cdfc90-8f43-11e9-a81f-7f0000015566";

	URL url = new URL("http://localhost:8080/Task/Detector/1/b4cdfc90-8f43-11e9-a81f-7f0000015566");

	HttpURLConnection con = (HttpURLConnection) url.openConnection();
	con.setRequestProperty("Range", "bytes=0-1023");
	con.setRequestMethod("GET");

	con.setConnectTimeout(5000); // server should be fast
	con.setReadTimeout(5000);

	int status = con.getResponseCode();

	BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	String inputLine;
	StringBuffer content = new StringBuffer();
	while ((inputLine = in.readLine()) != null) {
	    content.append(inputLine);
	}

	System.out.println(content.toString());

	in.close();
    }
}
