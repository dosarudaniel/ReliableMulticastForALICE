package ch.alice.o2.ccdb.servlets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import alien.catalogue.GUID;
import alien.catalogue.GUIDUtils;
import alien.catalogue.PFN;
import alien.catalogue.access.AccessType;
import alien.catalogue.access.AuthorizationFactory;
import alien.io.protocols.Factory;
import alien.io.protocols.Xrootd;
import alien.se.SE;
import alien.se.SEUtils;
import lazyj.DBFunctions;
import lazyj.StringFactory;

/**
 * Physical removal of files is expensive so don't make the client wait until it happens but instead return control immediately and do the physical removal asynchronously
 *
 * @author costing
 * @since 2018-06-08
 */
public class AsyncReplication extends Thread {
	private AsyncReplication() {
		// singleton
	}

	static class AsyncReplicationTarget implements Runnable {
		final SQLObject object;
		final SE se;

		public AsyncReplicationTarget(final SQLObject object, final SE se) {
			this.object = object;
			this.se = se;
		}

		@Override
		public void run() {
			final File localFile = object.getLocalFile(false);

			if (localFile == null || !localFile.exists()) {
				System.err.println("No local file to read from");
				return;
			}

			final GUID guid = object.toGUID();

			if (guid == null)
				return;

			final Integer seNumber = Integer.valueOf(se.seNumber);

			final PFN newpfn = new PFN(object.getAddress(seNumber), guid, se);

			final String reason = AuthorizationFactory.fillAccess(newpfn, AccessType.WRITE);

			if (reason != null) {
				System.err.println("Cannot get the write envelope for " + newpfn.getPFN() + ", reason is: " + reason);
				return;
			}

			final Xrootd xrootd = Factory.xrootd;

			try {
				xrootd.put(newpfn, localFile);

				try (DBFunctions db = SQLObject.getDB()) {
					db.query("update ccdb set replicas=replicas || ? where id=? and NOT ? = ANY(replicas);", false, seNumber, object.id, seNumber);
				}
			} catch (final IOException e) {
				System.err.println("Could not upload to: " + newpfn.pfn + ", reason was: " + e.getMessage());
			}
		}
	}

	@Override
	public void run() {
		while (true) {
			AsyncReplicationTarget target;
			try {
				target = asyncReplicationQueue.take();

				if (target != null)
					target.run();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private final BlockingQueue<AsyncReplicationTarget> asyncReplicationQueue = new LinkedBlockingQueue<>();

	private static AsyncReplication instance = null;

	static synchronized AsyncReplication getInstance() {
		if (instance == null) {
			instance = new AsyncReplication();
			instance.start();
		}

		return instance;
	}

	private static volatile long lastRefreshed = 0;

	private static volatile List<String> targetSEs = null;

	private static List<String> getTargetSEs() {
		if (System.currentTimeMillis() - lastRefreshed > 1000 * 60) {
			lastRefreshed = System.currentTimeMillis();

			List<String> newValue = null;

			try (DBFunctions db = SQLObject.getDB()) {
				db.query("SELECT value FROM config WHERE key='replication.ses';");

				if (db.moveNext()) {
					final StringTokenizer st = new StringTokenizer(db.gets(1), ",; \t\n\r");

					if (st.hasMoreTokens()) {
						newValue = new ArrayList<>(st.countTokens());

						while (st.hasMoreTokens())
							newValue.add(st.nextToken());
					}
				}
			}

			targetSEs = newValue;
		}

		return targetSEs;
	}

	static boolean queueDefaultReplication(final SQLObject object) {
		final List<String> targets = getTargetSEs();

		if (targets == null || targets.isEmpty())
			return true; // nothing to do, but it's expected to be ok

		boolean anyOk = false;

		for (final String seName : targets)
			if (queueMirror(object, seName))
				anyOk = true;

		return anyOk;
	}

	static boolean queueMirror(final SQLObject object, final String seName) {
		final SE se = SEUtils.getSE(seName);

		if (se != null)
			return queueMirror(object, se);

		return false;
	}

	static boolean queueMirror(final SQLObject object, final SE se) {
		return getInstance().asyncReplicationQueue.offer(new AsyncReplicationTarget(object, se));
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
					final GUID guid = GUIDUtils.getGUID(object.id, true);

					if (guid.exists())
						// It should not exist in AliEn, these UUIDs are created only in CCDB's space
						continue;

					guid.size = object.size;
					guid.md5 = StringFactory.get(object.md5);
					guid.owner = StringFactory.get("ccdb");
					guid.gowner = StringFactory.get("ccdb");
					guid.perm = "755";
					guid.ctime = new Date(object.createTime);
					guid.expiretime = null;
					guid.type = 0;
					guid.aclId = -1;

					final PFN delpfn = new PFN(object.getAddress(replica), guid, se);

					final String reason = AuthorizationFactory.fillAccess(delpfn, AccessType.DELETE);

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
