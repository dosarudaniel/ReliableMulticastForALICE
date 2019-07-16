package ch.alice.o2.ccdb.webserver;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;

import ch.alice.o2.ccdb.servlets.Memory;
import ch.alice.o2.ccdb.servlets.MemoryBrowse;

/**
 * Start an embedded Tomcat with the local servlet mapping
 * (localhost:8080/Local/) by default
 *
 * @author costing
 * @since 2017-09-26
 */
public class MemoryEmbeddedTomcat {

    /**
     * @param args
     * @throws ServletException
     */
    public static void main(final String[] args) throws ServletException {
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

	// Start the server
	try {
	    tomcat.start();
	} catch (final LifecycleException le) {
	    System.err.println("Cannot start the Tomcat server: " + le.getMessage());
	    return;
	}

	if (tomcat.debugLevel >= 1)
	    System.err.println("Ready to accept HTTP calls on " + tomcat.address + ":" + tomcat.getPort()
		    + ", file repository base path is: " + Memory.basePath);

	tomcat.blockWaiting();
    }
}
