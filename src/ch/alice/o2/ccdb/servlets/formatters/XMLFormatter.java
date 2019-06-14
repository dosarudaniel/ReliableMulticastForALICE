package ch.alice.o2.ccdb.servlets.formatters;

import java.io.PrintWriter;
import java.util.Map;

import ch.alice.o2.ccdb.servlets.LocalObjectWithVersion;
import ch.alice.o2.ccdb.servlets.SQLObject;
import lazyj.Format;

/**
 * @author costing
 * @since 2018-04-26
 */
public class XMLFormatter implements SQLFormatter {
	@Override
	public void header(final PrintWriter writer) {
		writer.print("<objects>\n");
	}

	@Override
	public void format(final PrintWriter writer, final SQLObject obj) {
		writer.print("<object id='");
		writer.print(obj.id.toString());

		writer.print("' validFrom='");
		writer.print(obj.validFrom);

		writer.print("' validUntil='");
		writer.print(obj.validUntil);

		writer.print("' initialValidity='");
		writer.print(obj.initialValidity);

		writer.print("' created='");
		writer.print(obj.createTime);

		writer.print("' lastModified='");
		writer.print(obj.getLastModified());

		writer.print("' md5='");
		writer.print(Format.escHtml(obj.md5));

		writer.print("' fileName='");
		writer.print(Format.escHtml(obj.fileName));

		writer.print("' contentType='");
		writer.print(Format.escHtml(obj.contentType));

		writer.print("' size='");
		writer.print(obj.size);

		writer.print("'  path='");
		writer.print(Format.escHtml(obj.getPath()));

		writer.print("'>\n");

		for (final Map.Entry<Integer, String> entry : obj.metadata.entrySet()) {
			writer.print("  <metadata key='");
			writer.print(Format.escHtml(SQLObject.getMetadataString(entry.getKey())));
			writer.print("' value='");
			writer.print(Format.escHtml(entry.getValue()));
			writer.print("'/>\n");
		}

		for (final Integer replica : obj.replicas) {
			writer.print("  <replica id='");
			writer.print(replica);
			writer.print("' addr='");
			writer.print(Format.escHtml(obj.getAddress(replica)));
			writer.print("'/>\n");
		}

		writer.print("</object>\n");
	}

	@Override
	public void footer(final PrintWriter writer) {
		writer.print("</objects>\n");
	}

	@Override
	public void middle(final PrintWriter writer) {
		// nothing
	}

	@Override
	public void start(final PrintWriter writer) {
		writer.println("<document>");
	}

	@Override
	public void subfoldersListingHeader(final PrintWriter writer) {
		writer.println("<folders>");
	}

	@Override
	public void subfoldersListing(final PrintWriter writer, final String path, final String url) {
		writer.print("<path name='");
		writer.print(Format.escHtml(path));
		writer.println("'/>");
	}

	@Override
	public void subfoldersListing(final PrintWriter writer, final String path, final String url, final long ownCount, final long ownSize, final long subfolderCount, final long subfolderSize) {
		subfoldersListing(writer, path, url);
		// TODO actual implementation of extended listing
	}

	@Override
	public void subfoldersListingFooter(final PrintWriter writer, final long ownCount, final long ownSize) {
		writer.println("</folders>");
	}

	@Override
	public void end(final PrintWriter writer) {
		writer.println("</document>");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see ch.alice.o2.ccdb.servlets.formatters.SQLFormatter#format(java.io.PrintWriter, ch.alice.o2.ccdb.servlets.LocalObjectWithVersion)
	 */
	@Override
	public void format(final PrintWriter writer, final LocalObjectWithVersion obj) {
		writer.print("<object id='");
		writer.print(Format.escHtml(obj.getID()));

		writer.print("' validFrom='");
		writer.print(obj.getStartTime());

		writer.print("' validUntil='");
		writer.print(obj.getEndTime());

		writer.print("' initialValidity='");
		writer.print(obj.getInitialValidity());

		writer.print("' created='");
		writer.print(obj.getCreateTime());

		writer.print("' lastModified='");
		writer.print(obj.getLastModified());

		writer.print("' md5='");
		writer.print(Format.escHtml(obj.getProperty("Content-MD5")));

		writer.print("' fileName='");
		writer.print(Format.escHtml(obj.getOriginalName()));

		writer.print("' contentType='");
		writer.print(Format.escHtml(obj.getProperty("Content-Type", "application/octet-stream")));

		writer.print("' size='");
		writer.print(obj.getSize());

		writer.print("'  path='");
		writer.print(Format.escHtml(obj.getPath()));

		writer.print("'>\n");

		for (final Object key : obj.getPropertiesKeys()) {
			writer.print("  <metadata key='");
			writer.print(Format.escHtml(key.toString()));
			writer.print("' value='");
			writer.print(Format.escHtml(obj.getProperty(key.toString())));
			writer.print("'/>\n");
		}

		writer.print("  <replica id='");
		writer.print(0);
		writer.print("' addr='");
		writer.print(Format.escHtml(obj.getPath()));
		writer.print("'/>\n");

		writer.print("</object>\n");
	}

	@Override
	public void setExtendedReport(final boolean extendedReport) {
		// Extended report not implemented for XML dump
	}
}
