package ch.alice.o2.ccdb.servlets;

import java.util.Vector;

import alien.monitoring.MonitoringObject;
import lazyj.DBFunctions;

/**
 * Publish monitoring information about the metadata repository
 *
 * @author costing
 * @since 2019-05-24
 */
public class SQLStatsExporter implements MonitoringObject {
	@Override
	public void fillValues(final Vector<String> paramNames, final Vector<Object> paramValues) {
		try (DBFunctions db = SQLObject.getDB()) {
			db.query("SELECT split_part(path,'/',1), sum(object_count), sum(object_size) FROM ccdb_paths INNER JOIN ccdb_stats USING(pathid) GROUP BY 1;");

			long totalCount = 0;
			long totalSize = 0;

			while (db.moveNext()) {
				final String path = db.gets(1);
				final long pathObjectCount = db.getl(2);
				final long pathObjectSize = db.getl(3);

				totalCount += pathObjectCount;
				totalSize += pathObjectSize;

				paramNames.add(path + "_count");
				paramNames.add(path + "_size");

				paramValues.add(Double.valueOf(pathObjectCount));
				paramValues.add(Double.valueOf(pathObjectSize));
			}

			paramNames.add("_TOTALS__count");
			paramNames.add("_TOTALS__size");

			paramValues.add(Double.valueOf(totalCount));
			paramValues.add(Double.valueOf(totalSize));
		}
	}
}
