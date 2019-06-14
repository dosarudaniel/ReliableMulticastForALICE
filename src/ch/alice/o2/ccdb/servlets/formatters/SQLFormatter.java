package ch.alice.o2.ccdb.servlets.formatters;

import java.io.PrintWriter;

import ch.alice.o2.ccdb.servlets.LocalObjectWithVersion;
import ch.alice.o2.ccdb.servlets.SQLObject;

/**
 * @author costing
 * @since 2018-04-26
 */
public interface SQLFormatter {
	/**
	 * Start of document
	 *
	 * @param writer
	 */
	public void start(PrintWriter writer);

	/**
	 * Start of object listing
	 *
	 * @param writer
	 *            object to append to
	 */
	public void header(PrintWriter writer);

	/**
	 * Formatting one object at a time
	 *
	 * @param writer
	 *            object to append to
	 * @param obj
	 *            current element to serialize
	 */
	public void format(PrintWriter writer, SQLObject obj);

	/**
	 * Formatting one object at a time
	 *
	 * @param writer
	 *            object to append to
	 * @param obj
	 *            current element to serialize
	 */
	public void format(PrintWriter writer, LocalObjectWithVersion obj);

	/**
	 * If there is more than one element in the list and there has to be a separator between consecutive elements, this is one separator
	 *
	 * @param writer
	 *            object to append to
	 */
	public void middle(PrintWriter writer);

	/**
	 * End of the object listing
	 *
	 * @param writer
	 *            object to append to
	 */
	public void footer(PrintWriter writer);

	/**
	 * Start of the subfolders listing
	 *
	 * @param writer
	 */
	public void subfoldersListingHeader(PrintWriter writer);

	/**
	 * One subpath
	 *
	 * @param writer
	 * @param path
	 *            just the folder name
	 * @param url
	 *            folder name and other constraints (time, metadata ...) to apply the same filtering to the subfolders too
	 */
	public void subfoldersListing(PrintWriter writer, String path, String url);

	/**
	 * One subpath, for extended reports
	 *
	 * @param writer
	 * @param path
	 * @param url
	 * @param ownCount number of objects stored in this particular folder
	 * @param ownSize total size of the objects in this folder directly
	 * @param subfolderCount number of objects in all of its subfolders
	 * @param subfolderSize size of all objects in all its subfolders
	 */
	public void subfoldersListing(PrintWriter writer, String path, String url, long ownCount, long ownSize, long subfolderCount, long subfolderSize);

	/**
	 * End of subfolders listing
	 *
	 * @param writer
	 * @param ownCount objects in this folder
	 * @param ownSize total size of objects in this folder
	 */
	public void subfoldersListingFooter(PrintWriter writer, long ownCount, long ownSize);

	/**
	 * End of document
	 *
	 * @param writer
	 */
	public void end(PrintWriter writer);

	/**
	 * Set the extended report flag. When set the browsing will include size and count of objects in the given path and all its subfolders.
	 *
	 * @param extendedReport
	 */
	public void setExtendedReport(boolean extendedReport);
}
