package test.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import myjava.com.github.dosarudaniel.gsoc.MulticastServer;

public class TestMulticastServer {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {

	if (args.length != 2) {
	    String usage = "Usage:\n";
	    usage += "\tjava -cp bin test.com.github.dosarudaniel.gsoc.MulticastServer ";
	    usage += "<IP> <PORT_NUMBER> ";
	    System.out.println(usage);
	    return;
	}

	MulticastServer multicastServer = new MulticastServer(args[0], Integer.parseInt(args[1]));
	multicastServer.work();

    }
}
