package test.com.github.dosarudaniel.gsoc;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;

import ch.alice.o2.ccdb.servlets.Memory;
import ch.alice.o2.ccdb.servlets.MemoryBrowse;
import ch.alice.o2.ccdb.webserver.EmbeddedTomcat;
import myjava.com.github.dosarudaniel.gsoc.MulticastReceiver;

public class TestMulticastReceiver {

    public static String ip = "";
    public static int portNumber = -1;

    public static Thread startMulticastReceiverThread = new Thread(new Runnable() {
	@Override
	public void run() {
	    MulticastReceiver multicastReceiver = new MulticastReceiver(ip, portNumber);
	    try {
		multicastReceiver.work();
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    });

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {

	if (args.length != 2) {
	    String usage = "Usage:\n";
	    usage += "\tjava -cp bin " + TestMulticastReceiver.class.getCanonicalName();
	    usage += "  <IP>  <PORT_NUMBER> ";
	    System.out.println(usage);
	    return;
	}

	ip = args[0];
	portNumber = Integer.parseInt(args[1]);

	EmbeddedTomcat tomcat;

	try {
	    tomcat = new EmbeddedTomcat("localhost");
	} catch (final ServletException se) {
	    System.err.println("Cannot create the Tomcat server: " + se.getMessage());
	    return;
	}

	final Wrapper browser = tomcat.addServlet(MemoryBrowse.class.getName(), "/browse/*");
	browser.addMapping("/latest/*");
	tomcat.addServlet(Memory.class.getName(), "/*");

	try {
	    tomcat.start();

	} catch (final LifecycleException le) {
	    System.err.println("Cannot start the Tomcat server: " + le.getMessage());
	    return;
	}

	if (tomcat.debugLevel >= 1)
	    System.err.println("Ready to accept HTTP calls on " + tomcat.address + ":" + tomcat.getPort()
		    + ", file repository base path is: " + Memory.basePath);

	startMulticastReceiverThread.start();

	tomcat.blockWaiting(); // inainte de asta run multicastReceiver

	try {
	    startMulticastReceiverThread.join();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}
    }
}
