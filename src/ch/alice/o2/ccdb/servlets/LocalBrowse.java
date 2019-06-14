package ch.alice.o2.ccdb.servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ch.alice.o2.ccdb.RequestParser;
import ch.alice.o2.ccdb.servlets.formatters.HTMLFormatter;
import ch.alice.o2.ccdb.servlets.formatters.JSONFormatter;
import ch.alice.o2.ccdb.servlets.formatters.SQLFormatter;
import ch.alice.o2.ccdb.servlets.formatters.TextFormatter;
import ch.alice.o2.ccdb.servlets.formatters.XMLFormatter;
import lazyj.Format;

/**
 * SQL-backed implementation of CCDB. This servlet implements browsing of objects in a particular path
 *
 * @author costing
 * @since 2018-04-26
 */
@WebServlet("/browse/*")
public class LocalBrowse extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		// list of objects matching the request
		// URL parameters are:
		// task name / detector name [ / time [ / UUID ] | [ / query string]* ]
		// if time is missing - get the last available time
		// query string example: "quality=2"

		final RequestParser parser = new RequestParser(request, true);

		final Collection<LocalObjectWithVersion> matchingObjects = getAllMatchingObjects(parser);

		String sContentType;
		SQLFormatter formatter = null;

		String sAccept = request.getParameter("Accept");

		if ((sAccept == null) || (sAccept.length() == 0))
			sAccept = request.getHeader("Accept");

		if ((sAccept == null || sAccept.length() == 0))
			sAccept = "text/plain";

		sAccept = sAccept.toLowerCase();

		if ((sAccept.indexOf("application/json") >= 0) || (sAccept.indexOf("text/json") >= 0)) {
			sContentType = "application/json";
			formatter = new JSONFormatter();
		}
		else
			if (sAccept.indexOf("text/html") >= 0) {
				sContentType = "text/html";
				formatter = new HTMLFormatter();
			}
			else
				if (sAccept.indexOf("text/xml") >= 0) {
					sContentType = "text/xml";
					formatter = new XMLFormatter();
				}
				else {
					sContentType = "text/plain";
					formatter = new TextFormatter();
				}

		response.setContentType(sContentType);

		try (PrintWriter pw = response.getWriter()) {
			formatter.start(pw);

			formatter.header(pw);

			boolean first = true;

			if (matchingObjects != null)
				for (final LocalObjectWithVersion object : matchingObjects) {
					if (first)
						first = false;
					else
						formatter.middle(pw);

					formatter.format(pw, object);
				}

			formatter.footer(pw);

			if (!parser.wildcardMatching) {
				// It is not clear which subfolders to list in case of a wildcard matching of objects. As the full hierarchy was included in the search there is no point in showing them, so just skip
				// this section.
				formatter.subfoldersListingHeader(pw);

				final String prefix = Local.basePath + "/" + parser.path;

				String suffix = "";

				if (parser.startTimeSet)
					suffix += "/" + parser.startTime;

				if (parser.uuidConstraint != null)
					suffix += "/" + parser.uuidConstraint;

				for (final Map.Entry<String, String> entry : parser.flagConstraints.entrySet())
					suffix += "/" + entry.getKey() + "=" + entry.getValue();

				final File fBaseDir = new File(prefix);

				final File[] baseDirListing = fBaseDir.listFiles((f) -> f.isDirectory());

				first = true;

				if (baseDirListing != null)
					for (final File fSubdir : baseDirListing) {
						try {
							Long.parseLong(fSubdir.getName());
						}
						catch (@SuppressWarnings("unused") final NumberFormatException nfe) {
							if (first)
								first = false;
							else
								formatter.middle(pw);

							final String pathPrefix = parser.path.length() > 0 ? parser.path + "/" : "";

							formatter.subfoldersListing(pw, "/" + pathPrefix + fSubdir.getName(), pathPrefix + fSubdir.getName() + suffix);
						}
					}

				formatter.subfoldersListingFooter(pw, 0, 0);
			}

			formatter.end(pw);
		}
	}

	/**
	 * @param parser
	 * @return all matching objects given the parser constraints
	 */
	public static final Collection<LocalObjectWithVersion> getAllMatchingObjects(final RequestParser parser) {
		if (parser.path == null)
			parser.path = "";

		final int idxStar = parser.path.indexOf('*');
		final int idxPercent = parser.path.indexOf('%');

		final File fBaseDir;

		final Pattern matchingPattern;

		if (idxStar >= 0 || idxPercent >= 0) {
			parser.wildcardMatching = true;

			final int idxFirst = idxStar >= 0 && idxPercent >= 0 ? Math.min(idxStar, idxPercent) : Math.max(idxStar, idxPercent);

			final int idxLastSlash = parser.path.lastIndexOf('/', idxFirst);

			String fixedPath = Local.basePath;

			String pattern = parser.path;

			if (idxLastSlash >= 0) {
				fixedPath += "/" + parser.path.substring(0, idxLastSlash);
				pattern = parser.path.substring(idxLastSlash + 1);
			}

			pattern = Format.replace(pattern, "*", "[^/]*");
			pattern = Format.replace(pattern, "%", "[^/]*");

			pattern += "/.*";

			matchingPattern = Pattern.compile(fixedPath + "/" + pattern);

			fBaseDir = new File(fixedPath);
		}
		else {
			fBaseDir = new File(Local.basePath + "/" + parser.path);

			if (parser.startTime > 0 && parser.uuidConstraint != null) {
				// is this the full path to a file? if so then download it

				final File toDownload = new File(fBaseDir, parser.startTime + "/" + parser.uuidConstraint.toString());

				if (toDownload.exists() && toDownload.isFile())
					return Arrays.asList(new LocalObjectWithVersion(parser.startTime, toDownload));

				// a particular object was requested but it doesn't exist
				return null;
			}

			matchingPattern = null;
		}

		final Collection<LocalObjectWithVersion> ret = new ArrayList<>();

		recursiveMatching(parser, ret, fBaseDir, matchingPattern);

		return ret;
	}

	private static void recursiveMatching(final RequestParser parser, final Collection<LocalObjectWithVersion> ret, final File fBaseDir, final Pattern matchingPattern) {
		LocalObjectWithVersion mostRecent = null;

		final File[] baseDirListing = fBaseDir.listFiles((f) -> f.isDirectory());

		if (baseDirListing == null)
			return;

		for (final File fInterval : baseDirListing)
			try {
				final long lValidityStart = Long.parseLong(fInterval.getName());

				if (matchingPattern != null || (parser.startTimeSet && lValidityStart > parser.startTime))
					continue;

				final File[] intervalFileList = fInterval.listFiles((f) -> f.isFile() && !f.getName().contains("."));

				if (intervalFileList == null)
					continue;

				for (final File f : intervalFileList) {
					final LocalObjectWithVersion owv = new LocalObjectWithVersion(lValidityStart, f);

					if ((!parser.startTimeSet || owv.covers(parser.startTime)) && (parser.notAfter <= 0 || owv.getCreateTime() <= parser.notAfter) && owv.matches(parser.flagConstraints)) {
						if (parser.latestFlag) {
							if (mostRecent == null)
								mostRecent = owv;
							else
								if (owv.compareTo(mostRecent) < 0)
									mostRecent = owv;
						}
						else
							ret.add(owv);
					}
				}

				if (parser.latestFlag && mostRecent != null)
					ret.add(mostRecent);
			}
			catch (@SuppressWarnings("unused") final NumberFormatException nfe) {
				// When the subfolder is not a number then it can be another level of objects, to be inspected as well

				if (parser.wildcardMatching) {
					if (matchingPattern != null) {
						final Matcher m = matchingPattern.matcher(fInterval.getAbsolutePath() + "/");

						if (m.matches()) {
							// full pattern match, from here on we can list files in the subfolders
							recursiveMatching(parser, ret, fInterval, null);
							continue;
						}
					}

					recursiveMatching(parser, ret, fInterval, matchingPattern);
				}
			}
	}
}
