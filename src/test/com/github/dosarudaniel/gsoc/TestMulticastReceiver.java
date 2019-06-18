package test.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import myjava.com.github.dosarudaniel.gsoc.MulticastReceiver;

public class TestMulticastReceiver {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {

	if (args.length != 2) {
	    String usage = "Usage:\n";
	    usage += "\tjava -cp bin " + TestMulticastReceiver.class.getCanonicalName();
	    usage += "  <IP>  <PORT_NUMBER> ";
	    System.out.println(usage);
	    return;
	}

	MulticastReceiver multicastReceiver = new MulticastReceiver(args[0], Integer.parseInt(args[1]));
	multicastReceiver.work();

    }
}
