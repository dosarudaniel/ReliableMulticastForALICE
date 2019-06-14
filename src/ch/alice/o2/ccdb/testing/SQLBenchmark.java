package ch.alice.o2.ccdb.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import ch.alice.o2.ccdb.RequestParser;
import ch.alice.o2.ccdb.servlets.SQLObject;
import lazyj.DBFunctions;
import lazyj.Format;

/**
 * Evaluate the performance of insert and query of SQL-backed CCDB objects
 *
 * @author costing
 * @since 2017-10-16
 */
public class SQLBenchmark {
	/**
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(final String[] args) throws InterruptedException {
		final long targetNoOfObjects = args.length >= 1 ? Long.parseLong(args[0]) : 100000;

		final long noThreads = args.length >= 2 ? Long.parseLong(args[1]) : 10;

		long startingCount = args.length >= 3 ? Long.parseLong(args[2]) : -1;

		if (startingCount < 0)
			try (DBFunctions db = SQLObject.getDB()) {
				db.query("SELECT count(1) FROM ccdb;");

				if (db.moveNext())
					startingCount = db.getl(1);
				else
					startingCount = 0;
			}

		System.err.println("Executing vacuum");

		try (DBFunctions db = SQLObject.getDB()) {
			db.query("VACUUM FULL ANALYZE ccdb;");
		}

		final long base = startingCount;

		final long noOfObjects = (targetNoOfObjects > startingCount && noThreads > 0) ? ((targetNoOfObjects - startingCount) / noThreads) : 0;

		final List<Thread> threads = new ArrayList<>();

		System.err.println(
				"Inserting " + (noOfObjects * noThreads) + " new objects in the database on " + noThreads + " threads, starting from " + base + " (target = " + targetNoOfObjects + " objects)");

		final long startTime = System.currentTimeMillis();

		for (long thread = 0; thread < noThreads; thread++) {
			final long localThread = thread;

			final Thread t = new Thread() {
				@Override
				public void run() {
					for (long i = 0; i < noOfObjects; i++) {
						final SQLObject obj = new SQLObject("dummy");

						obj.validFrom = (base + i + localThread * noOfObjects) * 160;
						obj.validUntil = obj.validFrom + 600000;

						obj.fileName = "some_new_detector_object.root";
						obj.contentType = "application/octet-stream";
						obj.uploadedFrom = "127.0.0.1";
						obj.size = base + localThread * noOfObjects + i;
						obj.md5 = "7e8fbee4f76f7079ec87bdc83d7d5538";

						obj.replicas.add(Integer.valueOf(1));

						obj.save(null);
					}
				}
			};

			t.start();

			threads.add(t);
		}

		// wait for all threads to finish
		for (final Thread t : threads)
			t.join();

		if (noOfObjects * noThreads > 0) {
			// print insert statistics
			System.err.println(noOfObjects * noThreads + " created in " + Format.toInterval(System.currentTimeMillis() - startTime) + " ("
					+ (double) (System.currentTimeMillis() - startTime) / (noOfObjects * noThreads) + " ms/object)");

			System.err.println((noOfObjects * noThreads * 1000.) / (System.currentTimeMillis() - startTime) + " Hz");
		}
		else
			System.err.println("Not inserting anything, just benchmarking read times");

		final Random r = new Random(System.currentTimeMillis());

		final int noQueries = 10000;

		for (final int queryThreads : new int[] { 1, 4, 8, 16, 24, 32 }) {
			final long startQueryTime = System.currentTimeMillis();

			threads.clear();

			final AtomicInteger nullObjects = new AtomicInteger(0);

			for (long thread = 0; thread < queryThreads; thread++) {
				final Thread t = new Thread() {
					@Override
					public void run() {
						for (int i = 0; i < noQueries; i++) {
							final RequestParser parser = new RequestParser(null);

							parser.path = "dummy";
							// parser.startTime = i * rangeWidth + r.nextLong() % rangeWidth;
							parser.startTime = Math.abs(r.nextLong() % ((base + noOfObjects * noThreads) * 160));
							parser.startTimeSet = true;

							final SQLObject result = SQLObject.getMatchingObject(parser);

							if (result == null)
								nullObjects.incrementAndGet();
						}
					}
				};

				t.start();

				threads.add(t);
			}

			// wait for all threads to finish
			for (final Thread t : threads)
				t.join();

			System.err.println(queryThreads + " threads: " + (noQueries * queryThreads) + " queried in " + Format.toInterval(System.currentTimeMillis() - startQueryTime) + ", equivalent to "
					+ (double) (System.currentTimeMillis() - startQueryTime) / (noQueries * queryThreads) + " ms/object and " + (double) (System.currentTimeMillis() - startQueryTime) / noQueries
					+ " ms/object/thread");

			System.err.println(nullObjects + " / " + (noQueries * queryThreads) + " results were null");
		}
	}
}
