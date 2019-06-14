package ch.alice.o2.ccdb.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utilities common for servlets 
 * 
 * @author costing
 * @since 2019-05-10
 */
public class ServletHelper {
	static void printUsage(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		response.setContentType("text/plain");

		try (PrintWriter pw = response.getWriter()) {
			pw.append("Usage of /*:\n");
			pw.append("  GET:\n    task name / detector name / start time / <UUID> - return the content of that UUID, or\n    task name / detector name / [ / time [ / key = value]* ]\n\n");
			pw.append("  POST:\n    task name / detector name / start time [ / end time ] [ / UUID ] [ / key = value ]*\n    binary blob as multipart parameter called 'blob'\n\n");
			pw.append("  PUT:\n    task name / detector name / start time [ / new end time ] [ / UUID ] [? (key=newvalue&)* ]\n\n");
			pw.append("  DELETE:\n    task name / detector name / start time / UUID\n    or any other selection string, the matching object will be deleted\n\n");
			pw.append("Usage of /browse/* or /latest/*:\n");
			pw.append("  GET:\n    task name / detector name / [start time, default = now] [/key=value]*\n");
			pw.append("    Use the Accept header to control the output format (one of text/plain, text/html, text/xml, application/json)\n\n");
			pw.append("This call was made with:\n  servlet path: " + request.getServletPath());
			pw.append("\n  context path: " + request.getContextPath());
			pw.append("\n  HTTP method: " + request.getMethod());
			pw.append("\n  path info: " + request.getPathInfo());
			pw.append("\n  query string: " + request.getQueryString());
			pw.append("\n  request URI: " + request.getRequestURI());
			pw.append("\n");
		}
	}
}
