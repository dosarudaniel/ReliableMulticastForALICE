package ch.alice.o2.ccdb.servlets;

import static ch.alice.o2.ccdb.servlets.ServletHelper.printUsage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.tomcat.util.http.fileupload.IOUtils;

import ch.alice.o2.ccdb.Options;
import ch.alice.o2.ccdb.RequestParser;
import ch.alice.o2.ccdb.UUIDTools;

/**
 * Prototype implementation of QC repository. This simple implementation is filesystem-based and targeted to local development and testing of the QC framework
 *
 * @author costing
 * @since 2017-09-20
 */
@WebServlet("/*")
@MultipartConfig
public class Local extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(Local.class.getCanonicalName());

	/**
	 * The base path of the file repository
	 */
	public static final String basePath;

	static {
		String location = Options.getOption("file.repository.location", System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "QC");

		while (location.endsWith("/"))
			location = location.substring(0, location.length() - 1);

		basePath = location;
	}

	private static String getURLPrefix(final HttpServletRequest request) {
		return request.getContextPath() + request.getServletPath();
	}

	private static boolean basePathSupportsMSTimestamps = false;

	static {
		try {
			final File testBasePath = new File(basePath);

			if (!testBasePath.exists()) {
				if (!testBasePath.mkdirs()) {
					logger.log(Level.SEVERE, "Base directory cannot be created: " + basePath);
					System.exit(1);
				}
				else
					logger.log(Level.INFO, "Base directory created: " + basePath);
			}

			if (!testBasePath.isDirectory() || !testBasePath.canWrite()) {
				logger.log(Level.WARNING, "Base directory is not writable: " + basePath + " . Existing content will be returned but you won't be able to upload new objects.");
			}

			final File f1 = File.createTempFile("timestampCheck", "tmp", new File(basePath));

			basePathSupportsMSTimestamps = true;

			for (long lTest = 1000001; lTest <= 1000003; lTest++) {
				f1.setLastModified(lTest);

				if (f1.lastModified() != lTest)
					basePathSupportsMSTimestamps = false;
			}

			f1.delete();

			if (basePathSupportsMSTimestamps)
				logger.log(Level.INFO, "Underlying filesystem of " + basePath + " supports millisecond-level resolution, trusting the last modification time of the blob files");
			else
				logger.log(Level.WARNING,
						"Underlying filesystem of " + basePath + " doesn't support millisecond-level resolution, falling back to reading the end of validity interval from the *.properties files");
		}
		catch (final IOException e) {
			logger.log(Level.SEVERE, "Cannot test the underlying filesystem of " + basePath + " for time resolution, assuming it doesn't support millisecond-level modify times", e);
		}
	}

	/**
	 * @return <code>true</code> if the filesystem where the repository is located has millisecond support for the last modified field
	 */
	public static boolean hasMillisecondSupport() {
		return basePathSupportsMSTimestamps;
	}

	@Override
	protected void doHead(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response, true);
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response, false);
	}

	private static void doGet(final HttpServletRequest request, final HttpServletResponse response, final boolean head) throws IOException {
		// list of objects matching the request
		// URL parameters are:
		// task name / detector name [ / time [ / UUID ] | [ / query string]* ]
		// if time is missing - get the last available time
		// query string example: "quality=2"

		final RequestParser parser = new RequestParser(request);

		if (!parser.ok) {
			printUsage(request, response);
			return;
		}

		final LocalObjectWithVersion matchingObject = getMatchingObject(parser);

		if (matchingObject == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "No matching objects found");
			return;
		}

		if (parser.cachedValue != null && matchingObject.referenceFile.getName().equalsIgnoreCase(parser.cachedValue.toString())) {
			response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
			return;
		}

		if (parser.uuidConstraint != null) {
			// download this explicitly requested file, unless HEAD was indicated in which case no content should be returned to the client

			setHeaders(matchingObject, response);

			if (!head)
				download(matchingObject, request, response);
			else {
				response.setContentLengthLong(matchingObject.referenceFile.length());
				response.setHeader("Content-Disposition", "inline;filename=\"" + matchingObject.getOriginalName() + "\"");
				response.setHeader("Content-Type", matchingObject.getProperty("Content-Type", "application/octet-stream"));
				response.setHeader("Accept-Ranges", "bytes");
				setMD5Header(matchingObject, response);
			}

			return;
		}

		setHeaders(matchingObject, response);

		response.sendRedirect(getURLPrefix(request) + matchingObject.referenceFile.getPath().substring(basePath.length()));
	}

	private static void setHeaders(final LocalObjectWithVersion obj, final HttpServletResponse response) {
		response.setDateHeader("Date", System.currentTimeMillis());
		response.setHeader("Valid-Until", String.valueOf(obj.getEndTime()));
		response.setHeader("Valid-From", String.valueOf(obj.startTime));
		response.setHeader("Created", String.valueOf(obj.getCreateTime()));
		response.setHeader("ETag", '"' + obj.referenceFile.getName() + '"');

		try {
			response.setDateHeader("Last-Modified", Long.parseLong(obj.getProperty("Last-Modified")));
		}
		catch (@SuppressWarnings("unused") NullPointerException | NumberFormatException ignore) {
			response.setDateHeader("Last-Modified", (obj.getCreateTime()));
		}
	}

	private static void setMD5Header(final LocalObjectWithVersion obj, final HttpServletResponse response) {
		String md5 = obj.getProperty("Content-MD5");

		try {
			if (md5 == null || md5.isEmpty())
				md5 = alien.io.IOUtils.getMD5(obj.referenceFile);
		}
		catch (@SuppressWarnings("unused") final IOException ioe) {
			// ignore IO exceptions
		}

		if (md5 != null && !md5.isEmpty())
			response.setHeader("Content-MD5", md5);
	}

	private static void download(final LocalObjectWithVersion obj, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final String range = request.getHeader("Range");

		if (range == null || range.trim().isEmpty()) {
			response.setHeader("Accept-Ranges", "bytes");
			response.setContentLengthLong(obj.referenceFile.length());
			response.setHeader("Content-Disposition", "inline;filename=\"" + obj.getOriginalName() + "\"");
			response.setHeader("Content-Type", obj.getProperty("Content-Type", "application/octet-stream"));
			setMD5Header(obj, response);

			try (InputStream is = new FileInputStream(obj.referenceFile); OutputStream os = response.getOutputStream()) {
				IOUtils.copy(is, os);
			}

			return;
		}

		// a Range request was made, serve only the requested bytes

		final long fileSize = obj.referenceFile.length();

		if (!range.startsWith("bytes=")) {
			response.setHeader("Content-Range", "bytes */" + fileSize);
			response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			return;
		}

		final StringTokenizer st = new StringTokenizer(range.substring(6).trim(), ",");

		final List<Map.Entry<Long, Long>> requestedRanges = new ArrayList<>();

		while (st.hasMoreTokens()) {
			final String s = st.nextToken();

			final int idx = s.indexOf('-');

			long start;
			long end;

			if (idx > 0) {
				start = Long.parseLong(s.substring(0, idx));

				if (start >= fileSize) {
					response.setHeader("Content-Range", "bytes */" + fileSize);
					response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, "You requested an invalid range, starting beyond the end of the file (" + start + ")");
					return;
				}

				if (idx < (s.length() - 1)) {
					end = Long.parseLong(s.substring(idx + 1));

					if (end >= fileSize) {
						response.setHeader("Content-Range", "bytes */" + fileSize);
						response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, "You requested bytes beyond what the file contains (" + end + ")");
						return;
					}
				}
				else
					end = fileSize - 1;

				if (start > end) {
					response.setHeader("Content-Range", "bytes */" + fileSize);
					response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
							"The requested range is wrong, the second value (" + end + ") should be larger than the first (" + start + ")");
					return;
				}
			}
			else
				if (idx == 0) {
					// a single negative value means 'last N bytes'
					start = Long.parseLong(s.substring(idx + 1));

					end = fileSize - 1;

					start = end - start + 1;

					if (start < 0) {
						response.setHeader("Content-Range", "bytes */" + fileSize);
						response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
								"You requested more last bytes (" + s.substring(idx + 1) + ") from the end of the file than the file actually has (" + fileSize + ")");
						start = 0;
					}
				}
				else {
					start = Long.parseLong(s);

					if (start >= fileSize) {
						response.setHeader("Content-Range", "bytes */" + fileSize);
						response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, "You requested an invalid range, starting beyond the end of the file (" + start + ")");
						return;
					}

					end = fileSize - 1;
				}

			requestedRanges.add(new AbstractMap.SimpleEntry<>(Long.valueOf(start), Long.valueOf(end)));
		}

		if (requestedRanges.size() == 0) {
			response.setHeader("Content-Range", "bytes */" + fileSize);
			response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
			return;
		}

		response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

		if (requestedRanges.size() == 1) {
			// A single byte range
			final Map.Entry<Long, Long> theRange = requestedRanges.get(0);
			final long first = theRange.getKey().longValue();
			final long last = theRange.getValue().longValue();

			final long toCopy = last - first + 1;

			response.setContentLengthLong(toCopy);
			response.setHeader("Content-Range", "bytes " + first + "-" + last + "/" + fileSize);
			response.setHeader("Content-Disposition", "inline;filename=\"" + obj.getOriginalName() + "\"");
			response.setHeader("Content-Type", obj.getProperty("Content-Type", "application/octet-stream"));

			try (RandomAccessFile input = new RandomAccessFile(obj.referenceFile, "r"); OutputStream output = response.getOutputStream()) {
				input.seek(first);
				copy(input, output, toCopy);
			}

			return;
		}

		// the content length should be computed based on the ranges and the header sizes. Or just don't send it :)
		final String boundaryString = "THIS_STRING_SEPARATES_" + UUIDTools.generateTimeUUID(System.currentTimeMillis(), null).toString();

		response.setHeader("Content-Type", "multipart/byteranges; boundary=" + boundaryString);

		final ArrayList<String> subHeaders = new ArrayList<>(requestedRanges.size());

		long contentLength = 0;

		for (final Map.Entry<Long, Long> theRange : requestedRanges) {
			final long first = theRange.getKey().longValue();
			final long last = theRange.getValue().longValue();

			final long toCopy = last - first + 1;

			final StringBuilder subHeader = new StringBuilder();

			subHeader.append("\n--").append(boundaryString);
			subHeader.append("\nContent-Type: ").append(obj.getProperty("Content-Type", "application/octet-stream")).append('\n');
			subHeader.append("Content-Range: bytes ").append(first).append("-").append(last).append("/").append(fileSize).append("\n\n");

			final String sh = subHeader.toString();

			subHeaders.add(sh);

			contentLength += toCopy + sh.length();
		}

		final String documentFooter = "\n--" + boundaryString + "--\n";

		contentLength += documentFooter.length();

		response.setContentLengthLong(contentLength);

		try (RandomAccessFile input = new RandomAccessFile(obj.referenceFile, "r"); OutputStream output = response.getOutputStream()) {
			final Iterator<Map.Entry<Long, Long>> itRange = requestedRanges.iterator();
			final Iterator<String> itSubHeader = subHeaders.iterator();

			while (itRange.hasNext()) {
				final Map.Entry<Long, Long> theRange = itRange.next();
				final String subHeader = itSubHeader.next();

				final long first = theRange.getKey().longValue();
				final long last = theRange.getValue().longValue();

				final long toCopy = last - first + 1;

				output.write(subHeader.getBytes());

				input.seek(first);
				copy(input, output, toCopy);
			}

			output.write(documentFooter.getBytes());
		}
	}

	private static void copy(final RandomAccessFile input, final OutputStream output, final long count) throws IOException {
		final byte[] buffer = new byte[4096];
		int cnt = 0;

		long leftToCopy = count;

		do {
			final int toCopy = (int) Math.min(leftToCopy, buffer.length);

			cnt = input.read(buffer, 0, toCopy);

			output.write(buffer, 0, cnt);

			leftToCopy -= cnt;
		} while (leftToCopy > 0);
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		// create the given object and return the unique identifier to it
		// URL parameters are:
		// task name / detector name / start time [/ end time] [ / flag ]*
		// mime-encoded blob is the value to be stored
		// if end time is missing then it will be set to the same value as start time
		// flags are in the form "key=value"

		final RequestParser parser = new RequestParser(request);

		if (!parser.ok) {
			printUsage(request, response);
			return;
		}

		final Collection<Part> parts = request.getParts();

		if (parts.size() == 0) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "POST request doesn't contain the data to upload");
			return;
		}

		final File folder = new File(basePath + "/" + parser.path + "/" + parser.startTime);

		if (!folder.exists())
			if (!folder.mkdirs()) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cannot create path " + folder.getAbsolutePath());
				return;
			}

		final long newObjectTime = System.currentTimeMillis();

		byte[] remoteAddress = null;

		try {
			final InetAddress ia = InetAddress.getByName(request.getRemoteAddr());

			remoteAddress = ia.getAddress();
		}
		catch (@SuppressWarnings("unused") final Throwable t) {
			// ignore
		}

		final UUID targetUUID = UUIDTools.generateTimeUUID(newObjectTime, remoteAddress);

		final Part part = parts.iterator().next();

		final File targetFile = new File(folder, targetUUID.toString());

		try (FileOutputStream fos = new FileOutputStream(targetFile)) {
			IOUtils.copy(part.getInputStream(), fos);
		}

		final LocalObjectWithVersion newObject = new LocalObjectWithVersion(parser.startTime, targetFile);

		for (final Map.Entry<String, String> constraint : parser.flagConstraints.entrySet())
			newObject.setProperty(constraint.getKey(), constraint.getValue());

		newObject.setProperty("InitialValidityLimit", String.valueOf(parser.endTime));
		newObject.setProperty("OriginalFileName", part.getSubmittedFileName());
		newObject.setProperty("Content-Type", part.getContentType());
		newObject.setProperty("UploadedFrom", request.getRemoteHost());
		newObject.setProperty("File-Size", String.valueOf(targetFile.length()));
		newObject.setProperty("Content-MD5", alien.io.IOUtils.getMD5(targetFile));

		if (newObject.getProperty("CreateTime") == null)
			newObject.setProperty("CreateTime", String.valueOf(newObjectTime));

		newObject.setValidityLimit(parser.endTime);

		newObject.saveProperties(request.getRemoteHost());

		setHeaders(newObject, response);
		response.setHeader("Location", getURLPrefix(request) + "/" + parser.path + "/" + parser.startTime + "/" + targetUUID.toString());
		response.sendError(HttpServletResponse.SC_CREATED);
	}

	@Override
	protected void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final RequestParser parser = new RequestParser(request);

		if (!parser.ok) {
			printUsage(request, response);
			return;
		}

		final LocalObjectWithVersion matchingObject = getMatchingObject(parser);

		if (matchingObject == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "No matching objects found");
			return;
		}

		for (final Map.Entry<String, String[]> param : request.getParameterMap().entrySet())
			if (param.getValue().length > 0)
				matchingObject.setProperty(param.getKey(), param.getValue()[0]);

		if (parser.endTimeSet)
			matchingObject.setValidityLimit(parser.endTime);

		matchingObject.saveProperties(request.getRemoteHost());

		setHeaders(matchingObject, response);

		response.setHeader("Location", getURLPrefix(request) + matchingObject.referenceFile.getPath().substring(basePath.length()));

		if (matchingObject.taintedProperties)
			response.sendError(HttpServletResponse.SC_NO_CONTENT);
		else
			response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
	}

	@Override
	protected void doDelete(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final RequestParser parser = new RequestParser(request);

		if (!parser.ok) {
			printUsage(request, response);
			return;
		}

		final LocalObjectWithVersion matchingObject = getMatchingObject(parser);

		if (matchingObject == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "No matching objects found");
			return;
		}

		if (!matchingObject.referenceFile.delete()) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not delete the underlying file");
			return;
		}

		final File fProperties = new File(matchingObject.referenceFile.getPath() + ".properties");
		fProperties.delete();

		response.sendError(HttpServletResponse.SC_NO_CONTENT);
	}

	private static LocalObjectWithVersion getMatchingObject(final RequestParser parser) {
		final File fBaseDir = new File(Local.basePath + "/" + parser.path);

		if (parser.startTime > 0 && parser.uuidConstraint != null) {
			// is this the full path to a file? if so then download it

			final File toDownload = new File(fBaseDir, parser.startTime + "/" + parser.uuidConstraint.toString());

			if (toDownload.exists() && toDownload.isFile())
				return new LocalObjectWithVersion(parser.startTime, toDownload);

			// a particular object was requested but it doesn't exist
			return null;
		}

		LocalObjectWithVersion mostRecent = null;

		final File[] baseDirListing = fBaseDir.listFiles((f) -> f.isDirectory());

		if (baseDirListing == null)
			return null;

		for (final File fInterval : baseDirListing)
			try {
				final long lValidityStart = Long.parseLong(fInterval.getName());

				if (parser.startTimeSet && lValidityStart < parser.startTime)
					continue;

				final File[] intervalFileList = fInterval.listFiles((f) -> f.isFile() && !f.getName().contains("."));

				if (intervalFileList == null)
					continue;

				for (final File f : intervalFileList) {
					final LocalObjectWithVersion owv = new LocalObjectWithVersion(lValidityStart, f);

					if (owv.covers(parser.startTime) && (parser.notAfter <= 0 || owv.getCreateTime() <= parser.notAfter) && owv.matches(parser.flagConstraints))
						if (mostRecent == null)
							mostRecent = owv;
						else
							if (owv.compareTo(mostRecent) < 0)
								mostRecent = owv;
				}
			}
			catch (@SuppressWarnings("unused") final NumberFormatException nfe) {
				// ignore
			}

		return mostRecent;
	}

	@Override
	protected void doOptions(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		response.setHeader("Allow", "GET, HEAD, POST, PUT, DELETE, OPTIONS");
		response.setHeader("Accept-Ranges", "bytes");

		final RequestParser parser = new RequestParser(request);

		if (!parser.ok)
			return;

		final LocalObjectWithVersion matchingObject = getMatchingObject(parser);

		if (matchingObject == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "No matching objects found");
			return;
		}

		for (final Object key : matchingObject.getPropertiesKeys())
			response.setHeader(key.toString(), matchingObject.getProperty(key.toString()));

		setHeaders(matchingObject, response);
	}
}
