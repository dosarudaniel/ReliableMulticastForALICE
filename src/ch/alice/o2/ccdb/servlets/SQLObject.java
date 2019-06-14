package ch.alice.o2.ccdb.servlets;

import java.io.File;
import java.net.InetAddress;
import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import alien.catalogue.GUID;
import alien.catalogue.GUIDUtils;
import alien.monitoring.Monitor;
import alien.monitoring.MonitorFactory;
import alien.se.SE;
import alien.se.SEUtils;
import ch.alice.o2.ccdb.Options;
import ch.alice.o2.ccdb.RequestParser;
import ch.alice.o2.ccdb.UUIDTools;
import lazyj.DBFunctions;
import lazyj.ExtProperties;
import lazyj.Format;
import lazyj.StringFactory;

/**
 * SQL backing for a CCDB/QC object
 *
 * @author costing
 * @since 2017-10-13
 */
public class SQLObject {
	private static ExtProperties config = new ExtProperties(Options.getOption("config.dir", "."), Options.getOption("config.file", "config"));

	private static final Monitor monitor = MonitorFactory.getMonitor(SQLObject.class.getCanonicalName());

	/**
	 * @return the database connection
	 */
	public static final DBFunctions getDB() {
		return new DBFunctions(config);
	}

	/**
	 * Unique identifier of an object
	 */
	public final UUID id;

	/**
	 * Path identifier
	 */
	public Integer pathId = null;

	/**
	 * String representation of the path
	 */
	public String path;

	/**
	 * Creation time, in epoch milliseconds
	 */
	public final long createTime;

	/**
	 * Starting time of the validity of this object, in epoch milliseconds. Inclusive value.
	 */
	public long validFrom = System.currentTimeMillis();

	/**
	 * Ending time of the validity of this object, in epoch milliseconds. Exclusive value.
	 */
	public long validUntil = validFrom + 1;

	/**
	 * Metadata fields set for this object
	 */
	public Map<Integer, String> metadata = new HashMap<>();

	/**
	 * Servers holding a replica of this object
	 */
	public Set<Integer> replicas = new HashSet<>();

	/**
	 * Size of the object
	 */
	public long size = -1;

	/**
	 * MD5 checksum
	 */
	public String md5 = null;

	/**
	 * Initial validity of this object (might be updated but this is to account for how long it was at the beginning)
	 */
	public long initialValidity = validUntil;

	/**
	 * Original file name that was uploaded (will be presented with the same name to the client)
	 */
	public String fileName = null;

	/**
	 * Content type
	 */
	public String contentType = null;

	/**
	 * IP address of the client that has uploaded this object
	 */
	public String uploadedFrom = null;

	/**
	 * Timestamp of the last update
	 */
	public long lastModified = System.currentTimeMillis();

	private transient boolean existing = false;
	private transient boolean tainted = false;

	/**
	 * Create an empty object
	 *
	 * @param path
	 *            object path
	 */
	public SQLObject(final String path) {
		createTime = System.currentTimeMillis();
		id = UUIDTools.generateTimeUUID(createTime, null);

		assert path != null && path.length() > 0;

		this.path = path;
	}

	/**
	 * Create a new object from a request
	 *
	 * @param request
	 * @param path
	 *            object path
	 */
	public SQLObject(final HttpServletRequest request, final String path) {
		createTime = System.currentTimeMillis();

		byte[] remoteAddress = null;

		if (request != null)
			try {
				final InetAddress ia = InetAddress.getByName(request.getRemoteAddr());

				remoteAddress = ia.getAddress();
			} catch (@SuppressWarnings("unused") final Throwable t) {
				// ignore
			}

		id = UUIDTools.generateTimeUUID(createTime, remoteAddress);

		assert path != null && path.length() > 0;

		this.path = path;
	}

	/**
	 * @param db
	 *            database row to load the fields from
	 */
	public SQLObject(final DBFunctions db) {
		final Map<String, Object> columns = db.getValuesMap();

		id = (UUID) columns.get("id");

		createTime = db.getl("createtime");
		validFrom = db.getl("validfrom");
		validUntil = db.getl("validuntil"); // read from the tsrange structure
		size = db.getl("size");
		md5 = Format.replace(db.gets("md5"), "-", "");
		initialValidity = db.getl("initialvalidity");
		fileName = db.gets("filename");
		contentType = getContentType(Integer.valueOf(db.geti("contenttype")));
		uploadedFrom = db.gets("uploadedfrom");

		pathId = Integer.valueOf(db.geti("pathId")); // should convert back to the path

		final Map<?, ?> md = (Map<?, ?>) columns.get("metadata");

		if (md != null && md.size() > 0)
			for (final Map.Entry<?, ?> entry : md.entrySet())
				metadata.put(Integer.valueOf(entry.getKey().toString()), entry.getValue().toString());

		final Array replicasObject = (Array) columns.get("replicas");

		if (replicasObject != null)
			try {
				final Integer[] r = (Integer[]) replicasObject.getArray();

				for (final Integer i : r)
					replicas.add(i);
			} catch (@SuppressWarnings("unused") final SQLException e) {
				// ignore
			}

		existing = true;
	}

	/**
	 * @param request
	 *            request details, to decorate the metadata with
	 * @return <code>true</code> if the object was successfully saved
	 */
	public boolean save(final HttpServletRequest request) {
		if (!existing || tainted) {
			if (request != null && existing)
				setProperty("UpdatedFrom", request.getRemoteHost());

			if (pathId == null)
				pathId = getPathID(path, true);

			try (DBFunctions db = getDB()) {
				final StringBuilder sb = new StringBuilder();

				String replicaArray = null;

				if (replicas.size() > 0) {
					sb.setLength(0);
					sb.append("{");

					for (final Integer replica : replicas) {
						if (sb.length() > 2)
							sb.append(',');
						sb.append(replica);
					}

					sb.append('}');

					replicaArray = sb.toString();
				}

				lastModified = System.currentTimeMillis();

				if (existing) {
					final boolean ok = db.query(
							"UPDATE ccdb SET validity=tsrange(to_timestamp(?) AT TIME ZONE 'UTC', to_timestamp(?) AT TIME ZONE 'UTC'), replicas=?::int[], contenttype=?, metadata=?::hstore, lastmodified=? WHERE id=?;",
							false, Double.valueOf(validFrom / 1000.), Double.valueOf(validUntil / 1000.), replicaArray, getContentTypeID(contentType, true), metadata, Long.valueOf(lastModified), id);

					if (ok) {
						existing = true;
						tainted = false;
						return true;
					}

					System.err.println("Update query failed for id=" + id);
				}
				else {
					initialValidity = validUntil;

					if (db.query(
							"INSERT INTO ccdb (id, pathid, validity, createTime, replicas, size, md5, initialvalidity, filename, contenttype, uploadedfrom, metadata, lastmodified) VALUES (?, ?, tsrange(to_timestamp(?) AT TIME ZONE 'UTC', to_timestamp(?) AT TIME ZONE 'UTC'), ?, ?::int[], ?, ?::uuid, ?, ?, ?, ?::inet, ?, ?);",
							false, id, pathId, Double.valueOf(validFrom / 1000.), Double.valueOf(validUntil / 1000.), Long.valueOf(createTime), replicaArray, Long.valueOf(size), md5,
							Long.valueOf(initialValidity), fileName, getContentTypeID(contentType, true), uploadedFrom, metadata, Long.valueOf(lastModified))) {
						existing = true;
						tainted = false;
						return true;
					}

					System.err.println("Insert query failed for id=" + id);
				}
			}
		}

		return false;

	}

	/**
	 * @return last modification timestamp
	 */
	public long getLastModified() {
		try {
			final Integer key = getMetadataID("LastModified", false);

			if (key != null) {
				final String md = metadata.get(key);

				if (md != null)
					return Long.parseLong(md);
			}
		} catch (@SuppressWarnings("unused") final Throwable t) {
			// ignore, the default below will
		}

		return createTime;
	}

	/**
	 * @return the folder path to this object.
	 */
	public String getFolder() {
		final int hash = Math.abs(id.hashCode() % 1000000);

		return hash % 100 + "/" + hash / 100;
	}

	/**
	 * Return the full URL to the physical representation on this replica ID
	 *
	 * @param replica
	 * @return full URL
	 */
	public String getAddress(final Integer replica) {
		String pattern = config.gets("server." + replica + ".urlPattern", null);

		if (pattern == null) {
			if (replica.intValue() == 0) {
				String hostname;

				try {
					hostname = InetAddress.getLocalHost().getCanonicalHostName();
				} catch (@SuppressWarnings("unused") final Throwable t) {
					hostname = "localhost";
				}

				pattern = "http://" + hostname + ":" + Options.getIntOption("tomcat.port", 8080) + "/download/UUID";
			}
			else {
				final SE se = SEUtils.getSE(replica.intValue());

				if (se != null) {
					pattern = se.generateProtocol();
					if (!pattern.endsWith("/"))
						pattern += "/";

					pattern += "PATH.ccdb";
				}
				else
					pattern += "alien:///alice/ccdb/FOLDER/UUID";
			}

			config.set("server." + replica + ".urlPattern", pattern);
		}

		pattern = Format.replace(pattern, "UUID", id.toString());
		pattern = Format.replace(pattern, "FOLDER", getFolder());
		pattern = Format.replace(pattern, "PATH", SE.generatePath(id.toString()));

		return pattern;
	}

	/**
	 * Get all URLs where replicas of this object can be retrieved from
	 *
	 * @return the list of URLs where the content of this object can be retrieved from
	 */
	public List<String> getAddresses() {
		final List<String> ret = new ArrayList<>(replicas.size());

		for (final Integer replica : replicas)
			ret.add(getAddress(replica));

		return ret;
	}

	/**
	 * Get the directory on the local filesystem (starting from the directory structure under {@link SQLBacked#basePath}) where this file could be located. Optionally creates the directory structure
	 * to it, for when the files have to be uploaded.
	 *
	 * @param createIfMissing
	 *            create the directory structure. Set this to <code>true</code> only from upload methods, to <code>false</code> on read queries
	 * @return the folder, if it exists or (if indicated so) could be created. Or <code>null</code> if any problem.
	 */
	public File getLocalFolder(final boolean createIfMissing) {
		final File folder = new File(SQLBacked.basePath, getFolder());

		if (!folder.exists() && createIfMissing)
			if (!folder.mkdirs())
				return null;

		if (folder.exists() && folder.isDirectory())
			return folder;

		return null;
	}

	/**
	 * Get the local file that is a representation of this object.
	 *
	 * @param createIfMissing
	 *            Whether or not this is a write operation. In this case all intermediate folders are created (if possible). Pass <code>false</code> for all read-only queries.
	 * @return the local file for this object ID. For uploads the folders are created but not the end file. For read queries the entire structure must exist and the file has to have the same size as
	 *         the database record. Will return <code>null</code> if the local file doesn't exist and/or could not be created.
	 */
	public File getLocalFile(final boolean createIfMissing) {
		final File folder = getLocalFolder(createIfMissing);

		if (folder == null)
			return null;

		final File ret = new File(folder, id.toString());

		if (createIfMissing || (ret.exists() && ret.isFile() && ret.length() == size))
			return ret;

		return null;
	}

	/**
	 * Set a metadata field of this object. {@link #save(HttpServletRequest)} should be called afterwards to actually flush
	 * this change to the persistent store.
	 *
	 * @param key
	 * @param value
	 */
	public void setProperty(final String key, final String value) {
		final Integer keyID = getMetadataID(key, true);

		if (value == null) {
			final String oldValue = metadata.remove(keyID);

			tainted = tainted || oldValue != null;
		}
		else {
			final String oldValue = metadata.put(keyID, value);

			tainted = tainted || !value.equals(oldValue);
		}
	}

	/**
	 * @return the metadata keys
	 */
	public Set<String> getPropertiesKeys() {
		final Set<String> ret = new HashSet<>(metadata.size());

		for (final Integer metadataId : metadata.keySet())
			ret.add(getMetadataString(metadataId));

		return ret;
	}

	/**
	 * Modify the expiration time of an object
	 *
	 * @param newEndTime
	 * @return <code>true</code> if the value was modified
	 */
	public boolean setValidityLimit(final long newEndTime) {
		if (newEndTime != validUntil && newEndTime > validFrom) {
			validUntil = newEndTime;
			tainted = true;
			return true;
		}

		return false;
	}

	/**
	 * Delete this entry
	 *
	 * @return <code>true</code> if the removal was successful
	 */
	public boolean delete() {
		if (existing)
			try (DBFunctions db = getDB()) {
				final String q = "DELETE FROM ccdb WHERE id='" + id.toString() + "'";

				if (!db.query(q))
					return false;

				return db.getUpdateCount() > 0;
			}

		return false;
	}

	/**
	 * @return the full path of this object
	 */
	public String getPath() {
		if (path == null)
			path = getPath(pathId);

		return path;
	}

	private static Map<String, Integer> PATHS = new HashMap<>();
	private static Map<Integer, String> PATHS_REVERSE = new HashMap<>();

	static List<Integer> getPathIDsWithPatternFallback(final RequestParser parser) {
		final Integer exactPathId = parser.wildcardMatching ? null : getPathID(parser.path, false);

		final List<Integer> pathIDs;

		if (exactPathId != null)
			pathIDs = Arrays.asList(exactPathId);
		else
			// wildcard expression ?
			if (parser.path != null && (parser.path.contains("*") || parser.path.contains("%"))) {
				pathIDs = getPathIDs(parser.path);

				parser.wildcardMatching = true;

				if (pathIDs == null || pathIDs.size() == 0)
					return null;
			}
			else
				return null;

		return pathIDs;
	}

	private static synchronized Integer getPathID(final String path, final boolean createIfNotExists) {
		Integer value = PATHS.get(path);

		if (value != null)
			return value;

		try (DBFunctions db = getDB()) {
			db.query("SELECT pathid FROM ccdb_paths WHERE path=?;", false, path);

			if (db.moveNext()) {
				value = Integer.valueOf(db.geti(1));
				PATHS.put(path, value);
				PATHS_REVERSE.put(value, path);
				return value;
			}

			if (createIfNotExists)
				if (db.query("INSERT INTO ccdb_paths (path) VALUES (?);", false, path)) {
					db.query("SELECT pathid FROM ccdb_paths WHERE path=?;", false, path);

					if (db.moveNext()) {
						value = Integer.valueOf(db.geti(1));
						PATHS.put(path, value);
						PATHS_REVERSE.put(value, path);
						return value;
					}
				}
		}

		return null;
	}

	static synchronized String removePathID(final Integer pathID) {
		final String path = PATHS_REVERSE.remove(pathID);

		if (path != null)
			PATHS.remove(path);

		return path;
	}

	private static List<Integer> getPathIDs(final String pathPattern) {
		final List<Integer> ret = new ArrayList<>();

		try (DBFunctions db = getDB()) {
			if (pathPattern.contains("%"))
				db.query("SELECT pathid FROM ccdb_paths WHERE path LIKE ?", false, pathPattern);
			else
				db.query("SELECT pathid FROM ccdb_paths WHERE path ~ ?", false, "^" + pathPattern);

			while (db.moveNext())
				ret.add(Integer.valueOf(db.geti(1)));
		}

		return ret;
	}

	private static synchronized String getPath(final Integer pathId) {
		String value = PATHS_REVERSE.get(pathId);

		if (value != null)
			return value;

		try (DBFunctions db = getDB()) {
			db.query("SELECT path FROM ccdb_paths WHERE pathId=?;", false, pathId);

			if (db.moveNext()) {
				value = db.gets(1);

				PATHS.put(value, pathId);
				PATHS_REVERSE.put(pathId, value);
			}
		}

		return value;
	}

	private static Map<String, Integer> METADATA = new HashMap<>();
	private static Map<Integer, String> METADATA_REVERSE = new HashMap<>();

	private static synchronized Integer getMetadataID(final String metadataKey, final boolean createIfNotExists) {
		Integer value = METADATA.get(metadataKey);

		if (value != null)
			return value;

		try (DBFunctions db = getDB()) {
			db.query("SELECT metadataId FROM ccdb_metadata WHERE metadataKey=?;", false, metadataKey);

			if (db.moveNext()) {
				value = Integer.valueOf(db.geti(1));
				METADATA.put(metadataKey, value);
				METADATA_REVERSE.put(value, metadataKey);
				return value;
			}

			if (createIfNotExists)
				if (db.query("INSERT INTO ccdb_metadata (metadataKey) VALUES (?);", false, metadataKey)) {
					db.query("SELECT metadataId FROM ccdb_metadata WHERE metadataKey=?;", false, metadataKey);

					if (db.moveNext()) {
						value = Integer.valueOf(db.geti(1));
						METADATA.put(metadataKey, value);
						METADATA_REVERSE.put(value, metadataKey);
						return value;
					}
				}
		}

		return null;
	}

	/**
	 * Convert from metadata primary key (integer) to the String representation of it (as users passed them in the request)
	 *
	 * @param metadataId
	 * @return the string representation of this metadata key
	 */
	public static synchronized String getMetadataString(final Integer metadataId) {
		String value = METADATA_REVERSE.get(metadataId);

		if (value != null)
			return value;

		try (DBFunctions db = getDB()) {
			db.query("SELECT metadataKey FROM ccdb_metadata WHERE metadataId=?;", false, metadataId);

			if (db.moveNext()) {
				value = db.gets(1);

				METADATA.put(value, metadataId);
				METADATA_REVERSE.put(metadataId, value);
			}
		}

		return value;
	}

	private static Map<String, Integer> CONTENTTYPE = new HashMap<>();
	private static Map<Integer, String> CONTENTTYPE_REVERSE = new HashMap<>();

	private static synchronized Integer getContentTypeID(final String contentType, final boolean createIfNotExists) {
		Integer value = CONTENTTYPE.get(contentType);

		if (value != null)
			return value;

		try (DBFunctions db = getDB()) {
			db.query("SELECT contentTypeId FROM ccdb_contenttype WHERE contentType=?", false, contentType);

			if (db.moveNext()) {
				value = Integer.valueOf(db.geti(1));
				CONTENTTYPE.put(contentType, value);
				CONTENTTYPE_REVERSE.put(value, contentType);
				return value;
			}

			if (createIfNotExists)
				if (db.query("INSERT INTO ccdb_contenttype (contentType) VALUES (?);", false, contentType)) {
					db.query("SELECT contentTypeId FROM ccdb_contenttype WHERE contentType=?;", false, contentType);

					if (db.moveNext()) {
						value = Integer.valueOf(db.geti(1));
						CONTENTTYPE.put(contentType, value);
						CONTENTTYPE_REVERSE.put(value, contentType);
						return value;
					}
				}
		}

		return null;
	}

	private static synchronized String getContentType(final Integer contentTypeId) {
		String value = CONTENTTYPE_REVERSE.get(contentTypeId);

		if (value != null)
			return value;

		try (DBFunctions db = getDB()) {
			db.query("SELECT contentType FROM ccdb_contenttype WHERE contentTypeId=?;", false, contentTypeId);

			if (db.moveNext()) {
				value = db.gets(1);

				CONTENTTYPE.put(value, contentTypeId);
				CONTENTTYPE_REVERSE.put(contentTypeId, value);
			}
		}

		return value;
	}

	/**
	 * Retrieve from the database the only object that has this object ID
	 *
	 * @param id
	 *            the requested ID. Cannot be <code>null</code>.
	 * @return the object with this ID, if it exists. Or <code>null</code> if not.
	 */
	public static final SQLObject getObject(final UUID id) {
		final long lStart = System.nanoTime();

		try {
			if (id == null)
				return null;

			try (DBFunctions db = getDB()) {
				if (!db.query("SELECT *,extract(epoch from lower(validity))*1000 as validfrom,extract(epoch from upper(validity))*1000 as validuntil FROM ccdb WHERE id=?;", false, id)) {
					System.err.println("Query execution error");
					return null;
				}

				if (db.moveNext())
					return new SQLObject(db);
			}

			return null;
		} finally {
			monitor.addMeasurement("getObject_ms", (System.nanoTime() - lStart) / 1000000.);
		}
	}

	/**
	 * @param parser
	 * @return the most recent matching object
	 */
	public static final SQLObject getMatchingObject(final RequestParser parser) {
		final long lStart = System.nanoTime();

		try {
			final Integer pathId = getPathID(parser.path, false);

			if (pathId == null)
				return null;

			final List<Object> arguments = new ArrayList<>();

			try (DBFunctions db = getDB()) {
				final StringBuilder q = new StringBuilder(
						"SELECT *,extract(epoch from lower(validity))*1000 as validfrom,extract(epoch from upper(validity))*1000 as validuntil FROM ccdb WHERE pathId=?");

				arguments.add(pathId);

				if (parser.uuidConstraint != null) {
					q.append(" AND id=?");

					arguments.add(parser.uuidConstraint);
				}

				if (parser.startTimeSet) {
					q.append(" AND to_timestamp(?) AT TIME ZONE 'UTC' <@ validity");

					arguments.add(Double.valueOf(parser.startTime / 1000.));
				}

				if (parser.notAfter > 0) {
					q.append(" AND createTime<=?");

					arguments.add(Long.valueOf(parser.notAfter));
				}

				if (parser.flagConstraints != null && parser.flagConstraints.size() > 0)
					for (final Map.Entry<String, String> constraint : parser.flagConstraints.entrySet()) {
						final String key = constraint.getKey();

						final Integer metadataId = getMetadataID(key, false);

						if (metadataId == null)
							return null;

						final String value = constraint.getValue();

						q.append(" AND metadata -> ? = ?");

						arguments.add(metadataId.toString());
						arguments.add(value);
					}

				q.append(" ORDER BY createTime DESC LIMIT 1;");

				db.query(q.toString(), false, arguments.toArray(new Object[0]));

				if (db.moveNext())
					return new SQLObject(db);

				// System.err.println("No object for:\n" + q + "\nand\n" + arguments + "\n");

				return null;
			}
		} finally {
			monitor.addMeasurement("getMatchingObject_ms", (System.nanoTime() - lStart) / 1000000.);
		}
	}

	/**
	 * @param parser
	 * @return the most recent matching object
	 */
	public static final Collection<SQLObject> getAllMatchingObjects(final RequestParser parser) {
		final long lStart = System.nanoTime();

		try {
			final List<Integer> pathIDs = getPathIDsWithPatternFallback(parser);

			if (pathIDs == null || pathIDs.isEmpty())
				return null;

			final List<SQLObject> ret = new ArrayList<>();

			try (DBFunctions db = getDB()) {
				for (final Integer pathId : pathIDs) {
					final StringBuilder q = new StringBuilder(
							"SELECT *,extract(epoch from lower(validity))*1000 as validfrom,extract(epoch from upper(validity))*1000 as validuntil FROM ccdb WHERE pathId=?");

					final List<Object> arguments = new ArrayList<>();

					arguments.add(pathId);

					if (parser.uuidConstraint != null) {
						q.append(" AND id=?");

						arguments.add(parser.uuidConstraint);
					}

					if (parser.startTimeSet) {
						q.append(" AND to_timestamp(?) AT TIME ZONE 'UTC' <@ validity");

						arguments.add(Double.valueOf(parser.startTime / 1000.));
					}

					if (parser.notAfter > 0) {
						q.append(" AND createTime<=?");

						arguments.add(Long.valueOf(parser.notAfter));
					}

					if (parser.flagConstraints != null && parser.flagConstraints.size() > 0)
						for (final Map.Entry<String, String> constraint : parser.flagConstraints.entrySet()) {
							final String key = constraint.getKey();

							final Integer metadataId = getMetadataID(key, false);

							if (metadataId == null)
								return null;

							final String value = constraint.getValue();

							q.append(" AND metadata -> ? = ?");

							arguments.add(metadataId.toString());
							arguments.add(value);
						}

					q.append(" ORDER BY createTime DESC");

					if (parser.latestFlag)
						q.append(" LIMIT 1");

					db.query(q.toString(), false, arguments.toArray(new Object[0]));

					while (db.moveNext())
						ret.add(new SQLObject(db));
				}
			}

			return ret;
		} finally {
			monitor.addMeasurement("getAllMatchingObjects_ms", (System.nanoTime() - lStart) / 1000000.);
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		sb.append("ID: ").append(id.toString()).append('\n');
		sb.append("Path: ").append(getPath()).append('\n');
		sb.append("Validity: ").append(validFrom).append(" - ").append(validUntil).append(" (").append(new Date(validFrom)).append(" - ").append(new Date(validUntil)).append(")\n");
		sb.append("Initial validity limit: ").append(initialValidity).append(" (").append(new Date(initialValidity)).append(")\n");
		sb.append("Created: ").append(createTime).append(" (").append(new Date(createTime)).append(")\n");
		sb.append("Last modified: ").append(lastModified).append(" (").append(new Date(lastModified)).append(")\n");
		sb.append("Original file: ").append(fileName).append(", size: ").append(size).append(", md5: ").append(md5).append(", content type: ").append(contentType).append('\n');
		sb.append("Uploaded from: ").append(uploadedFrom).append('\n');

		if (metadata != null && metadata.size() > 0) {
			sb.append("Metadata:\n");

			for (final Map.Entry<Integer, String> entry : metadata.entrySet())
				sb.append("  ").append(getMetadataString(entry.getKey())).append(" = ").append(entry.getValue()).append('\n');
		}

		return sb.toString();
	}

	GUID toGUID() {
		final GUID guid = GUIDUtils.getGUID(id, true);

		if (guid.exists())
			// It should not exist in AliEn, these UUIDs are created only in CCDB's space
			return null;

		guid.size = size;
		guid.md5 = StringFactory.get(md5);
		guid.owner = StringFactory.get("ccdb");
		guid.gowner = StringFactory.get("ccdb");
		guid.perm = "755";
		guid.ctime = new Date(createTime);
		guid.expiretime = null;
		guid.type = 0;
		guid.aclId = -1;

		return guid;
	}
}
