package ch.alice.o2.ccdb.webserver;

import java.io.File;
import java.util.logging.LogManager;

import javax.servlet.ServletException;

import org.apache.catalina.Globals;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Valve;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.ErrorReportValve;

import ch.alice.o2.ccdb.Options;

/**
 * Configure an embedded Tomcat instance
 *
 * @author costing
 * @since 2017-10-13
 */
public class EmbeddedTomcat extends Tomcat {

	static {
		System.setProperty(Globals.CATALINA_HOME_PROP, System.getProperty("java.io.tmpdir"));
	}

	final int debugLevel;
	final String address;
	final StandardContext ctx;

	/**
	 * @param defaultAddress
	 *            default listening address for the Tomcat server. Either "localhost" (default for testing servers) or "*" for production instances.
	 * @throws ServletException
	 */
	public EmbeddedTomcat(final String defaultAddress) throws ServletException {
		super();

		// This is to disable Tomcat from creating work directories, nothing needs to be compiled on the fly
		debugLevel = Options.getIntOption("tomcat.debug", 1);

		// Disable all console logging by default
		if (debugLevel < 2)
			LogManager.getLogManager().reset();

		address = Options.getOption("tomcat.address", defaultAddress);

		setPort(Options.getIntOption("tomcat.port", 8080));
		getConnector().setProperty("address", address);
		getConnector().setProperty("maxKeepAliveRequests", String.valueOf(Options.getIntOption("maxKeepAliveRequests", 1000)));

		getConnector().setProperty("connectionTimeout", String.valueOf(Options.getIntOption("connectionTimeout", 10000))); // clients should be quick

		getConnector().setProperty("disableUploadTimeout", "false");
		getConnector().setProperty("connectionUploadTimeout", String.valueOf(Options.getIntOption("connectionTimeout", 300000))); // 5 minutes max to upload an object

		// Add a dummy ROOT context
		ctx = (StandardContext) addWebapp("", new File(System.getProperty("java.io.tmpdir")).getAbsolutePath());

		// disable per context work directories too
		ctx.setWorkDir(".");
	}

	/**
	 * @param className
	 * @param mapping
	 * @return the newly created wrapper around the
	 */
	public Wrapper addServlet(final String className, final String mapping) {
		final Wrapper wrapper = Tomcat.addServlet(ctx, className.substring(className.lastIndexOf('.') + 1), className);
		wrapper.addMapping(mapping);
		wrapper.setLoadOnStartup(0);
		return wrapper;
	}

	@Override
	public void start() throws LifecycleException {
		super.start();
		if (getService().findConnectors()[0].getState() == LifecycleState.FAILED) {
			System.err.println("Failed to start the embedded Tomcat listening on " + address + ":" + port + ".");

			if (debugLevel < 2)
				System.err.println("Set -Dtomcat.debug=2 (or export TOMCAT_DEBUG=2) to see the logging messages from the server.");

			throw new LifecycleException("Cannot bind on " + address + ":" + port);
		}

		final StandardHost host = (StandardHost) getHost();

		for (final Valve v : host.getPipeline().getValves())
			if (v instanceof ErrorReportValve) {
				final ErrorReportValve erv = (ErrorReportValve) v;
				erv.setShowServerInfo(false);
			}
	}

	/**
	 * @return the port for the default connector
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Block forever waiting for the server to exit (will never do normally)
	 */
	public void blockWaiting() {
		getServer().await();
	}
}
