package ch.alice.o2.ccdb.webserver;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;

import ch.alice.o2.ccdb.servlets.SQLBacked;
import ch.alice.o2.ccdb.servlets.SQLBrowse;
import ch.alice.o2.ccdb.servlets.SQLDownload;
import ch.alice.o2.ccdb.servlets.SQLTruncate;

/**
 * Start an embedded Tomcat with the SQL servlet mapping (*:8080/) by default
 *
 * @author costing
 * @since 2017-10-13
 */
public class SQLBackedTomcat {

	/**
	 * @param args
	 * @throws ServletException
	 */
	public static void main(final String[] args) throws ServletException {
		EmbeddedTomcat tomcat;

		try {
			tomcat = new EmbeddedTomcat("*");
		}
		catch (final ServletException se) {
			System.err.println("Cannot create the Tomcat server: " + se.getMessage());
			return;
		}

		tomcat.addServlet(SQLDownload.class.getName(), "/download/*");
		final Wrapper browser = tomcat.addServlet(SQLBrowse.class.getName(), "/browse/*");
		browser.addMapping("/latest/*");
		tomcat.addServlet(SQLBacked.class.getName(), "/*");

		tomcat.addServlet(SQLTruncate.class.getName(), "/truncate/*");

		// Start the server
		try {
			tomcat.start();
		}
		catch (final LifecycleException le) {
			System.err.println("Cannot start the Tomcat server: " + le.getMessage());
			return;
		}

		if (tomcat.debugLevel >= 1)
			System.err.println("Ready to accept HTTP calls on " + tomcat.address + ":" + tomcat.getPort());

		tomcat.blockWaiting();
	}
}
