package myjava.com.github.dosarudaniel.gsoc;

import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SingletonLogger {
    private static SingletonLogger singletonLogger;
    private static Logger logger;
    private static FileHandler fh;
    private static Formatter sf;

    public SingletonLogger() {
	// Make this class a singleton
	if (singletonLogger != null) {
	    return;
	}

	// Create the log file
	try {
	    fh = new FileHandler("/tmp/ALICE_multicast_log.txt");
	} catch (Exception e) {
	    e.printStackTrace();
	}

	logger = Logger.getLogger(this.getClass().getCanonicalName());

	sf = new SimpleFormatter();
	fh.setFormatter(sf);

	logger.addHandler(fh);
	singletonLogger = this;
    }

    public Logger getLogger() {
	return singletonLogger.logger;
    }
}
