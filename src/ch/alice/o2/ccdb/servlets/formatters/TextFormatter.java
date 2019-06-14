package ch.alice.o2.ccdb.servlets.formatters;

import java.io.PrintWriter;

import ch.alice.o2.ccdb.servlets.LocalObjectWithVersion;
import ch.alice.o2.ccdb.servlets.SQLObject;
import lazyj.Format;

/**
 * @author costing
 * @since 2018-04-26
 */
public class TextFormatter implements SQLFormatter {
	@Override
	public void header(final PrintWriter writer) {
		// nothing
	}

	@Override
	public void format(final PrintWriter writer, final SQLObject obj) {
		writer.println(obj.toString());
	}

	@Override
	public void format(final PrintWriter writer, final LocalObjectWithVersion obj) {
		writer.println(obj.toString());
	}

	@Override
	public void footer(final PrintWriter writer) {
		// nothing
	}

	@Override
	public void middle(final PrintWriter writer) {
		writer.println();
	}

	@Override
	public void start(final PrintWriter writer) {
		// nothing
	}

	@Override
	public void subfoldersListingHeader(final PrintWriter writer) {
		writer.println("\n\nSubfolders:");
	}

	@Override
	public void subfoldersListing(final PrintWriter writer, final String path, final String url) {
		writer.println("  " + path);
	}

	@Override
	public void subfoldersListing(final PrintWriter writer, final String path, final String url, final long ownCount, final long ownSize, final long subfolderCount, final long subfolderSize) {
		writer.println("  " + path + ", own objects: " + ownCount + " of " + Format.size(ownSize) + ", subfolders contain " + subfolderCount + " objects of " + Format.size(subfolderSize));
	}

	@Override
	public void subfoldersListingFooter(final PrintWriter writer, final long ownCount, final long ownSize) {
		// nothing
	}

	@Override
	public void end(final PrintWriter writer) {
		// nothing
	}

	@Override
	public void setExtendedReport(final boolean extendedReport) {
		// nothing needed
	}
}
