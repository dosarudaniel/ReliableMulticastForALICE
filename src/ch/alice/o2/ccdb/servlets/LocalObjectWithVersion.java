package ch.alice.o2.ccdb.servlets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import alien.catalogue.GUIDUtils;
import lazyj.cache.ExpirationCache;

/**
 * Handle all interactions with a local file, including the metadata access (backed by a .properties file with the same base name as the referenced file)
 *
 * @author costing
 * @since 2017-09-21
 */
public class LocalObjectWithVersion implements Comparable<LocalObjectWithVersion> {
	/**
	 * Start of validity interval. Cannot be modified at a later time.
	 */
	final long startTime;

	/**
	 * End of validity interval. Can be modified by {@link #setValidityLimit(long)}
	 */
	long endTime;

	/**
	 * Object creation time
	 */
	private long createTime = -1;

	/**
	 * File on disk with the blob content of this object
	 */
	final File referenceFile;

	/**
	 * Metadata keys and values, stored in an additional <i>{@link #referenceFile}.properties</i> file next to the blob object.
	 */
	private Properties objectProperties = null;

	/**
	 * Whether or not the metadata is tainted and should be updated on disk
	 */
	boolean taintedProperties = false;

	/**
	 * Whether or not the end time was fully inferred already, from reading the .properties file if necessary.
	 */
	private boolean completeEndTime = true;

	private final static ExpirationCache<String, Long> validityInterval = Local.hasMillisecondSupport() ? null : new ExpirationCache<>(65536);

	/**
	 * Create a local object based on a local file.
	 * 
	 * @param startTime
	 * @param entry
	 */
	public LocalObjectWithVersion(final long startTime, final File entry) {
		this.startTime = startTime;
		this.endTime = entry.lastModified();
		this.referenceFile = entry;

		completeEndTime = Local.hasMillisecondSupport();

		if (this.endTime <= this.startTime)
			this.endTime = this.startTime + 1;
	}

	/**
	 * @return the unique identifier of this object
	 */
	public String getID() {
		return referenceFile.getName();
	}

	/**
	 * @return the start of validity interval, in epoch millis. Interval contains this value.
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @return the end of validity interval, in epoch millis. Interval excludes this value.
	 */
	public long getEndTime() {
		if (completeEndTime)
			return this.endTime;

		// we can't trust the filesystem to keep accurate modification times, we'll have to load it from the metadata
		final String refFilePath = this.referenceFile.getAbsolutePath();

		final Long previouslyCachedValue = validityInterval.get(refFilePath);

		if (previouslyCachedValue != null) {
			this.endTime = previouslyCachedValue.longValue();
		}
		else {
			final String validUntil = getProperty("ValidUntil");

			if (validUntil != null && validUntil.length() > 0) {
				try {
					this.endTime = Long.parseLong(validUntil);
				}
				catch (@SuppressWarnings("unused") final NumberFormatException nfe) {
					System.err.println("Invalid timestamp format for " + refFilePath);
				}
			}

			if (this.endTime <= this.startTime)
				this.endTime = this.startTime + 1;

			validityInterval.put(refFilePath, Long.valueOf(this.endTime), 1000 * 60 * 60);
		}

		completeEndTime = true;

		return this.endTime;
	}

	/**
	 * @return the size of the binary content
	 */
	public long getSize() {
		return referenceFile.length();
	}

	/**
	 * @param key
	 * @return the metadata value for this key, or <code>null</code> if not found
	 */
	public String getProperty(final String key) {
		loadProperties();

		return objectProperties.getProperty(key);
	}

	/**
	 * @param key
	 * @param defaultValue
	 * @return the value of this key, either from the metadata or falling back to the default value if not set
	 */
	public String getProperty(final String key, final String defaultValue) {
		loadProperties();

		return objectProperties.getProperty(key, defaultValue);
	}

	/**
	 * @param key
	 * @param value
	 * @return <code>true</code> if the content was modified
	 */
	public boolean setProperty(final String key, final String value) {
		loadProperties();

		final Object oldValue = objectProperties.setProperty(key, value);

		final boolean changed = oldValue == null || !value.equals(oldValue.toString());

		taintedProperties = taintedProperties || changed;

		return changed;
	}

	/**
	 * @return all the metadata keys defined for this object
	 */
	public Set<Object> getPropertiesKeys() {
		loadProperties();

		return objectProperties.keySet();
	}

	/**
	 * @return the original file name, as uploaded by the client.
	 */
	public String getOriginalName() {
		return getProperty("OriginalFileName", referenceFile.getName());
	}

	/**
	 * @param flagConstraints
	 * @return <code>true</code> if this object matches the given constraints
	 */
	public boolean matches(final Map<String, String> flagConstraints) {
		if (flagConstraints.isEmpty())
			return true;

		loadProperties();

		search: for (final Map.Entry<String, String> entry : flagConstraints.entrySet()) {
			final String key = entry.getKey().trim();
			final String value = entry.getValue().trim();

			final String metaValue = objectProperties.getProperty(key);

			if (metaValue != null) {
				if (!metaValue.equals(value))
					return false;

				continue;
			}

			// fall back to searching for the key in case-insensitive mode

			for (final Map.Entry<Object, Object> e : objectProperties.entrySet()) {
				final String otherKey = e.getKey().toString();

				if (otherKey.equalsIgnoreCase(key)) {
					if (e.getValue().toString().equals(value))
						return false;

					continue search;
				}
			}

			// the required key was not found even in case-insensitive mode
			return false;
		}

		// all required keys matched
		return true;
	}

	/**
	 * @return relative path to the folder storing this file
	 */
	public String getPath() {
		return referenceFile.getPath().substring(Local.basePath.length());
	}

	/**
	 * @param referenceTime
	 * @return <code>true</code> if the reference time falls between start time (inclusive) and end time (exclusive).
	 */
	public boolean covers(final long referenceTime) {
		return this.startTime <= referenceTime && getEndTime() > referenceTime;
	}

	private void loadProperties() {
		if (objectProperties == null) {
			objectProperties = new Properties();

			try (FileReader reader = new FileReader(referenceFile.getAbsolutePath() + ".properties")) {
				objectProperties.load(reader);
			}
			catch (@SuppressWarnings("unused") final IOException e) {
				// .properties file is missing
			}
		}
	}

	/**
	 * Save the metadata to a persistent file on disk.
	 * 
	 * @param remoteAddress
	 * @throws IOException
	 */
	void saveProperties(final String remoteAddress) throws IOException {
		if (objectProperties != null && taintedProperties) {
			objectProperties.setProperty("Last-Modified", String.valueOf(System.currentTimeMillis()));

			if (remoteAddress != null)
				objectProperties.setProperty("UpdatedFrom", remoteAddress);

			try (OutputStream os = new FileOutputStream(referenceFile.getPath() + ".properties")) {
				objectProperties.store(os, null);
			}
		}
	}

	/**
	 * @return the end of validity interval as set by the initial upload. In epoch millis.
	 */
	public long getInitialValidity() {
		if (objectProperties != null) {
			String s = objectProperties.getProperty("InitialValidityLimit");

			if (s != null)
				return Long.parseLong(s);
		}

		return getEndTime();
	}

	/**
	 * @return the last time when the object data or metadata was modified. In epoch millis.
	 */
	public long getLastModified() {
		if (objectProperties != null) {
			String s = objectProperties.getProperty("Last-Modified");

			if (s != null)
				return Long.parseLong(s);
		}

		return -1;
	}

	/**
	 * @return when the object was first uploaded. In epoch millis.
	 */
	public long getCreateTime() {
		if (createTime > 0)
			return createTime;

		try {
			createTime = Long.parseLong(getProperty("CreateTime"));
		}
		catch (@SuppressWarnings("unused") NumberFormatException | NullPointerException ignore) {
			try {
				final UUID uuid = UUID.fromString(referenceFile.getName());
				createTime = GUIDUtils.epochTime(uuid);
				return createTime;
			}
			catch (@SuppressWarnings("unused") final Throwable t) {
				// if everything else fails use as last resort the start time of the interval, normally these two are the same
				return startTime;
			}
		}

		return createTime;
	}

	@Override
	public int compareTo(final LocalObjectWithVersion o) {
		final long diff = o.getCreateTime() - this.getCreateTime();

		if (diff < 0)
			return -1;

		if (diff > 0)
			return 1;

		return 0;
	}

	/**
	 * @param endTime
	 *            the new end validity interval
	 */
	public void setValidityLimit(final long endTime) {
		this.endTime = endTime;
		this.completeEndTime = true;
		referenceFile.setLastModified(endTime);
		setProperty("ValidUntil", String.valueOf(endTime));

		if (validityInterval != null)
			validityInterval.put(referenceFile.getAbsolutePath(), Long.valueOf(this.endTime), 1000 * 60 * 60);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		sb.append("ID: ").append(getID()).append('\n');
		sb.append("Path: ").append(getPath()).append('\n');
		sb.append("Validity: ").append(getStartTime()).append(" - ").append(getEndTime()).append(" (").append(new Date(getStartTime())).append(" - ").append(new Date(getEndTime())).append(")\n");
		sb.append("Initial validity limit: ").append(getInitialValidity()).append(" (").append(new Date(getInitialValidity())).append(")\n");
		sb.append("Created: ").append(createTime).append(" (").append(new Date(createTime)).append(")\n");
		sb.append("Last modified: ").append(getLastModified()).append(" (").append(new Date(getLastModified())).append(")\n");
		sb.append("Original file: ").append(getOriginalName()).append(", size: ").append(getSize()).append(", md5: ").append(getProperty("Content-MD5"));
		sb.append(", content type: ").append(getProperty("Content-Type", "application/octet-stream")).append('\n');
		sb.append("Uploaded from: ").append(getProperty("UploadedFrom")).append('\n');

		if (objectProperties != null && objectProperties.size() > 0) {
			sb.append("Metadata:\n");

			for (final Object key : objectProperties.keySet())
				sb.append("  ").append(key.toString()).append(" = ").append(objectProperties.getProperty(key.toString())).append('\n');
		}

		return sb.toString();
	}
}