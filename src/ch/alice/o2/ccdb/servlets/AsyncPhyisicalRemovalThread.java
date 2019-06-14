package ch.alice.o2.ccdb.servlets;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import alien.catalogue.GUID;
import alien.catalogue.PFN;
import alien.catalogue.access.AccessType;
import alien.catalogue.access.AuthorizationFactory;
import alien.io.protocols.Factory;
import alien.io.protocols.Xrootd;
import alien.se.SE;
import alien.se.SEUtils;

/**
 * Physical removal of files is expensive so don't make the client wait until it happens but instead return control immediately and do the physical removal asynchronously
 *
 * @author costing
 * @since 2018-06-08
 */
public class AsyncPhyisicalRemovalThread extends Thread {
	private AsyncPhyisicalRemovalThread() {
		// singleton
	}

	@Override
	public void run() {
		while (true) {
			SQLObject object;
			try {
				object = asyncPhysicalRemovalQueue.take();

				if (object != null)
					deleteReplicas(object);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private final BlockingQueue<SQLObject> asyncPhysicalRemovalQueue = new LinkedBlockingQueue<>();

	private static AsyncPhyisicalRemovalThread instance = null;

	static synchronized AsyncPhyisicalRemovalThread getInstance() {
		if (instance == null) {
			instance = new AsyncPhyisicalRemovalThread();
			instance.start();
		}

		return instance;
	}

	static void queueDeletion(final SQLObject object) {
		getInstance().asyncPhysicalRemovalQueue.offer(object);
	}

	static void deleteReplicas(final SQLObject object) {
		for (final Integer replica : object.replicas)
			if (replica.intValue() == 0) {
				// local file
				final File f = object.getLocalFile(false);

				if (f != null)
					f.delete();
			}
			else {
				final SE se = SEUtils.getSE(replica.intValue());

				if (se != null) {
					final GUID guid = object.toGUID();

					if (guid == null)
						continue;

					final PFN delpfn = new PFN(object.getAddress(replica), guid, se);

					final String reason = AuthorizationFactory.fillAccess(AuthorizationFactory.getDefaultUser(), delpfn, AccessType.DELETE, true);

					if (reason != null) {
						System.err.println("Cannot get the access tokens to remove this pfn: " + delpfn.getPFN() + ", reason is: " + reason);
						continue;
					}

					final Xrootd xrd = Factory.xrootd;

					try {
						if (!xrd.delete(delpfn))
							System.err.println("Cannot physically remove this file: " + delpfn.getPFN());
					} catch (final IOException e) {
						System.err.println("Exception removing this pfn: " + delpfn.getPFN() + " : " + e.getMessage());
					}
				}
			}
	}
}
