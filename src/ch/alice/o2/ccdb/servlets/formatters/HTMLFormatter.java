package ch.alice.o2.ccdb.servlets.formatters;

import java.io.PrintWriter;
import java.util.Date;
import java.util.Map;

import ch.alice.o2.ccdb.servlets.LocalObjectWithVersion;
import ch.alice.o2.ccdb.servlets.SQLObject;
import lazyj.Format;

/**
 * @author costing
 * @since 2018-04-26
 */
public class HTMLFormatter implements SQLFormatter {

	private boolean extendedReport = false;

	@Override
	public void header(final PrintWriter writer) {
		writer.print(
				"<table style='font-size:10px' border=1 cellspacing=0 cellpadding=2><thead><tr><th>ID</th><th>Valid from</th><th>Valid until</th><th>Initial validity limit</th><th>Created at</th><th>Last modified</th><th>MD5</th><th>File name</th><th>Content type</th><th>Size</th><th>Path</th><th>Metadata</th><th>Replicas</th></thead>\n");
	}

	@Override
	public void format(final PrintWriter writer, final SQLObject obj) {
		writer.print("<tr><td nowrap align=left>");
		writer.print(obj.id.toString());

		writer.print("</td><td nowrap align=right>");
		writer.print(obj.validFrom);
		writer.println("<br>");
		final Date dFrom = new Date(obj.validFrom);
		writer.print(Format.showDate(dFrom));

		writer.print("</td><td nowrap align=right>");
		writer.print(obj.validUntil);
		writer.println("<br>");
		final Date dUntil = new Date(obj.validUntil);
		writer.print(Format.showDate(dUntil));

		writer.print("</td><td align=right>");
		writer.print(obj.initialValidity);
		writer.println("<br>");
		final Date dInitial = new Date(obj.initialValidity);
		writer.print(Format.showDate(dInitial));

		writer.print("</td><td align=right>");
		writer.print(obj.createTime);
		writer.println("<br>");
		final Date dCreated = new Date(obj.createTime);
		writer.print(Format.showDate(dCreated));

		writer.print("</td><td align=right>");
		writer.print(obj.getLastModified());
		writer.println("<br>");
		final Date dLastModified = new Date(obj.getLastModified());
		writer.print(Format.showDate(dLastModified));

		writer.print("</td><td align=center nowrap>");
		writer.print(Format.escHtml(obj.md5));

		writer.print("</td><td align=right nowrap>");
		writer.print(Format.escHtml(obj.fileName));

		writer.print("</td><td align=right nowrap>");
		writer.print(Format.escHtml(obj.contentType));

		writer.print("</td><td align=right nowrap>");
		writer.print(obj.size);

		writer.print("</td><td align=left nowrap>");
		writer.print(Format.escHtml(obj.getPath()));

		writer.print("</td><td align=left><dl>");
		for (final Map.Entry<Integer, String> entry : obj.metadata.entrySet()) {
			writer.print("<dt>");
			writer.print(Format.escHtml(SQLObject.getMetadataString(entry.getKey())));
			writer.print("</dt><dd>");
			writer.print(Format.escHtml(entry.getValue()));
			writer.print("</dd>\n");
		}

		writer.print("</dl></td><td align=left><ul>");

		for (final Integer replica : obj.replicas) {
			writer.print("<li><a href='");
			writer.print(Format.escHtml(obj.getAddress(replica)));
			writer.print("'>");
			writer.print(replica);
			writer.print("</a></li>\n");
		}

		writer.print("</ul></td></tr>\n");
	}

	@Override
	public void format(final PrintWriter writer, final LocalObjectWithVersion obj) {
		writer.print("<tr><td nowrap align=left>");
		writer.print(obj.getID());

		writer.print("</td><td nowrap align=right>");
		writer.print(obj.getStartTime());
		writer.println("<br>");
		final Date dFrom = new Date(obj.getStartTime());
		writer.print(Format.showDate(dFrom));

		writer.print("</td><td nowrap align=right>");
		writer.print(obj.getEndTime());
		writer.println("<br>");
		final Date dUntil = new Date(obj.getEndTime());
		writer.print(Format.showDate(dUntil));

		writer.print("</td><td align=right>");
		writer.print(obj.getInitialValidity());
		writer.println("<br>");
		final Date dInitial = new Date(obj.getInitialValidity());
		writer.print(Format.showDate(dInitial));

		writer.print("</td><td align=right>");
		writer.print(obj.getCreateTime());
		writer.println("<br>");
		final Date dCreated = new Date(obj.getCreateTime());
		writer.print(Format.showDate(dCreated));

		writer.print("</td><td align=right>");
		writer.print(obj.getLastModified());
		writer.println("<br>");
		final Date dLastModified = new Date(obj.getLastModified());
		writer.print(Format.showDate(dLastModified));

		writer.print("</td><td align=center nowrap>");
		writer.print(Format.escHtml(obj.getProperty("Content-MD5")));

		writer.print("</td><td align=right nowrap>");
		writer.print(Format.escHtml(obj.getOriginalName()));

		writer.print("</td><td align=right nowrap>");
		writer.print(Format.escHtml(obj.getProperty("Content-Type", "application/octet-stream")));

		writer.print("</td><td align=right nowrap>");
		writer.print(obj.getSize());

		writer.print("</td><td align=left nowrap>");
		writer.print(Format.escHtml(obj.getPath()));

		writer.print("</td><td align=left><dl>");
		for (final Object key : obj.getPropertiesKeys()) {
			writer.print("<dt>");
			writer.print(Format.escHtml(key.toString()));
			writer.print("</dt><dd>");
			writer.print(Format.escHtml(obj.getProperty(key.toString())));
			writer.print("</dd>\n");
		}

		writer.print("</dl></td><td align=left><ul>");

		writer.print("<li><a href='");
		writer.print(Format.escHtml(obj.getPath()));
		writer.print("'>0");
		writer.print("</a></li>\n");

		writer.print("</ul></td></tr>\n");
	}

	@Override
	public void footer(final PrintWriter writer) {
		writer.print("</table>\n");
	}

	@Override
	public void middle(final PrintWriter writer) {
		// nothing
	}

	@Override
	public void start(final PrintWriter writer) {
		writer.write("<!DOCTYPE html><html>\n");
	}

	@Override
	public void subfoldersListingHeader(final PrintWriter writer) {
		writer.write("<br><br><table style='font-size:10px' border=1 cellspacing=0 cellpadding=2><thead><tr><th>Subfolder</th>");

		if (extendedReport)
			writer.write("<th>Own objects</th><th>Own size</th><th>Subfolder objects</th><th>Subfolder total size</th>");

		writer.write("</tr></thead>\n");
	}

	@Override
	public void subfoldersListing(final PrintWriter writer, final String path, final String url) {
		writer.write("<tr><td><a href='/browse/");
		writer.write(Format.escHtml(url));
		writer.write("'>");
		writer.write(Format.escHtml(path));
		writer.write("</a></td></tr>\n");
	}

	private long objectCount = 0;
	private long objectSize = 0;

	@Override
	public void subfoldersListing(final PrintWriter writer, final String path, final String url, final long ownCount, final long ownSize, final long subfolderCount, final long subfolderSize) {
		writer.write("<tr><td><a href='/browse/");
		writer.write(Format.escHtml(url));
		writer.write("?report=true'>");
		writer.write(Format.escHtml(path));
		writer.write("</a></td>");

		if (extendedReport) {
			writer.write("<td align=right>");
			writer.write(ownCount > 0 ? String.valueOf(ownCount) : "-");
			writer.write("</td><td align=right>");
			writer.write(ownSize > 0 ? Format.size(ownSize) : "-");
			writer.write("</td><td align=right>");
			writer.write(subfolderCount > 0 ? String.valueOf(subfolderCount) : "-");
			writer.write("</td><td align=right>");
			writer.write(subfolderSize > 0 ? Format.size(subfolderSize) : "-");
			writer.write("</td>");
		}

		objectCount += ownCount + subfolderCount;
		objectSize += ownSize + subfolderSize;

		writer.write("</tr>\n");
	}

	@Override
	public void subfoldersListingFooter(final PrintWriter writer, final long ownCount, final long ownSize) {
		if (extendedReport) {
			writer.write("<tfoot><tr><th>TOTAL</th><th align=right>");
			writer.write(ownCount > 0 ? String.valueOf(ownCount) : "-");
			writer.write("</th><th align=right>");
			writer.write(ownSize > 0 ? Format.size(ownSize) : "-");
			writer.write("</th><th align=right>");
			writer.write(objectCount > 0 ? String.valueOf(objectCount) : "-");
			writer.write("</th><th align=right>");
			writer.write(objectSize > 0 ? Format.size(objectSize) : "-");
			writer.write("</th></tfoot>");
		}

		writer.write("</table>\n");
	}

	@Override
	public void end(final PrintWriter writer) {
		writer.write("</html>");
	}

	@Override
	public void setExtendedReport(final boolean extendedReport) {
		this.extendedReport = extendedReport;
	}
}
