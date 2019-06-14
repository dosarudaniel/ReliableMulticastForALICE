package ch.alice.o2.ccdb.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.fileupload.IOUtils;

import alien.monitoring.Monitor;
import alien.monitoring.MonitorFactory;
import ch.alice.o2.ccdb.UUIDTools;

/**
 * SQL-backed implementation of CCDB. This servlet only implements GET (and HEAD) for a particular UUID that is known to reside on this server. It should normally not be accessed directly but clients
 * might end up here if the file was not migrated to other physical locations.
 *
 * @author costing
 * @since 2017-10-13
 */
@WebServlet("/download/*")
public class SQLDownload extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Monitor monitor = MonitorFactory.getMonitor(SQLDownload.class.getCanonicalName());

	@Override
	protected void doHead(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final long lStart = System.nanoTime();

		try {
			doGet(request, response, true);
		} finally {
			monitor.addMeasurement("HEAD_ms", (System.nanoTime() - lStart) / 1000000.);
		}
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		final long lStart = System.nanoTime();

		try {
			doGet(request, response, false);
		} finally {
			monitor.addMeasurement("GET_ms", (System.nanoTime() - lStart) / 1000000.);
		}
	}

	private static void doGet(final HttpServletRequest request, final HttpServletResponse response, final boolean head) throws IOException {
		final String pathInfo = request.getPathInfo();

		UUID id = null;

		try {
			id = UUID.fromString(pathInfo.substring(1));
		} catch (@SuppressWarnings("unused") final Throwable t) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The only path information supported is the requested object ID, i.e. '/download/UUID'");
			return;
		}

		final SQLObject matchingObject = SQLObject.getObject(id);

		// System.err.println(matchingObject);

		if (matchingObject == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Object id " + id + " could not be found in the database");
			return;
		}

		download(head, matchingObject, request, response);
	}

	static void download(final boolean head, final SQLObject obj, final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		SQLBacked.setHeaders(obj, response);

		if (head) {
			response.setContentLengthLong(obj.size);
			response.setHeader("Content-Disposition", "inline;filename=\"" + obj.fileName + "\"");
			response.setHeader("Content-Type", obj.contentType);
			response.setHeader("Accept-Ranges", "bytes");
			SQLBacked.setMD5Header(obj, response);

			return;
		}

		final String range = request.getHeader("Range");

		final File localFile = obj.getLocalFile(false);

		if (localFile == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "The file cannot be served by this server directly, the physical copy is not on this machine");
			return;
		}

		if (range == null || range.trim().isEmpty()) {
			response.setHeader("Accept-Ranges", "bytes");
			response.setContentLengthLong(obj.size);
			response.setHeader("Content-Disposition", "inline;filename=\"" + obj.fileName + "\"");
			response.setHeader("Content-Type", obj.contentType);
			SQLBacked.setMD5Header(obj, response);

			try (InputStream is = new FileInputStream(localFile); OutputStream os = response.getOutputStream()) {
				IOUtils.copy(is, os);
			}

			return;
		}

		// a Range request was made, serve only the requested bytes

		if (!range.startsWith("bytes=")) {
			response.setHeader("Content-Range", "bytes */" + obj.size);
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

				if (start >= obj.size) {
					response.setHeader("Content-Range", "bytes */" + obj.size);
					response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, "You requested an invalid range, starting beyond the end of the file (" + start + ")");
					return;
				}

				if (idx < (s.length() - 1)) {
					end = Long.parseLong(s.substring(idx + 1));

					if (end >= obj.size) {
						response.setHeader("Content-Range", "bytes */" + obj.size);
						response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, "You requested bytes beyond what the file contains (" + end + ")");
						return;
					}
				}
				else
					end = obj.size - 1;

				if (start > end) {
					response.setHeader("Content-Range", "bytes */" + obj.size);
					response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
							"The requested range is wrong, the second value (" + end + ") should be larger than the first (" + start + ")");
					return;
				}
			}
			else
				if (idx == 0) {
					// a single negative value means 'last N bytes'
					start = Long.parseLong(s.substring(idx + 1));

					end = obj.size - 1;

					start = end - start + 1;

					if (start < 0) {
						response.setHeader("Content-Range", "bytes */" + obj.size);
						response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE,
								"You requested more last bytes (" + s.substring(idx + 1) + ") from the end of the file than the file actually has (" + obj.size + ")");
						start = 0;
					}
				}
				else {
					start = Long.parseLong(s);

					if (start >= obj.size) {
						response.setHeader("Content-Range", "bytes */" + obj.size);
						response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, "You requested an invalid range, starting beyond the end of the file (" + start + ")");
						return;
					}

					end = obj.size - 1;
				}

			requestedRanges.add(new AbstractMap.SimpleEntry<>(Long.valueOf(start), Long.valueOf(end)));
		}

		if (requestedRanges.size() == 0) {
			response.setHeader("Content-Range", "bytes */" + obj.size);
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
			response.setHeader("Content-Range", "bytes " + first + "-" + last + "/" + obj.size);
			response.setHeader("Content-Disposition", "inline;filename=\"" + obj.fileName + "\"");
			response.setHeader("Content-Type", obj.contentType);

			try (RandomAccessFile input = new RandomAccessFile(localFile, "r"); OutputStream output = response.getOutputStream()) {
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
			subHeader.append("\nContent-Type: ").append(obj.contentType).append('\n');
			subHeader.append("Content-Range: bytes ").append(first).append("-").append(last).append("/").append(obj.size).append("\n\n");

			final String sh = subHeader.toString();

			subHeaders.add(sh);

			contentLength += toCopy + sh.length();
		}

		final String documentFooter = "\n--" + boundaryString + "--\n";

		contentLength += documentFooter.length();

		response.setContentLengthLong(contentLength);

		try (RandomAccessFile input = new RandomAccessFile(localFile, "r"); OutputStream output = response.getOutputStream()) {
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
	protected void doDelete(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "The DELETE method should use the main entry point instead of /download/, which is reserved for direct read access to the objects");
	}

	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "You shouldn't try to create objects via the /download/ servlet, go to / instead");
	}

	@Override
	protected void doPut(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Object manipulation is not available via the /download/ servlet, go to / instead");
	}
}
