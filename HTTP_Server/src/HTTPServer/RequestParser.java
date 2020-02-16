package HTTPServer;

import java.sql.Connection;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 * <p>
 * TODO: implement GET, HEAD, POST, PUT
 */
public class RequestParser {
	private byte[] requestBytes;
	private String requestFull;
	private String[] requestLines;
	private int payloadStartIndex;

	private String[] httpMain = null;

	private String userAgent;
	private String host;
	// TODO - Figure out which of these are actually required.
	private String connection;
	private String contentType;
	private String contentLength;

	public RequestParser(byte[] req) {
		// Trim unnecessary variables as time goes on
		requestBytes = req;
		requestFull = new String(requestBytes);

		// Split on CRLF, remove whitespace.
		requestLines = requestFull.split("[\\r\\n]+");

		// TODO - Figure out if payload really starts there
		// TODO - Figure out if this is actually needed for anything
		payloadStartIndex = requestFull.indexOf("\r\n\r\n") + 4;

		processData();

	}

	// Returns who is connected
	public String getUserAgent() {
		return userAgent;
	}

	// When you want to know how much data the client wants to PUT or POST.
	public int getContentLength() {
		return Integer.parseInt(contentLength);
	}

	// Get host that client wants to connect to
	public String getHost() {
		return host;
	}

	public String getConnection() {
		return connection;
	}

	// Get requested method; GET, PUT, POST, etc.
	public String getMethod() {
		return httpMain[0];
	}

	// Get requested path
	public String getPathRequest() {
		return httpMain[1];
	}

	// Not needed, will only be working with HTTP/1.1
	public String getHttpVersion() {
		return httpMain[2];
	}

	private void processData() {
		String[] processing;
		boolean first = true;
		for (String s : requestLines) {
			processing = s.split(":");
			if (first) {
				httpMain = processing;
				first = false;
			}
			else {
				// User agent is split in a more sophisticated way, fix!
				if (processing[0].equalsIgnoreCase("User-Agent")) {
					userAgent = processing[1];
				}
				else if (processing[0].equalsIgnoreCase("Host")) {
					host = processing[1];
				}
				else if (processing[0].equalsIgnoreCase("Connection")) {
					connection = processing[1];
				}
				else if (processing[0].equalsIgnoreCase("Content-Type")) {
					contentType = processing[1];
				}
				else if (processing[0].equalsIgnoreCase("Content-Length")) {
					contentLength = processing[1];
				}
				else {
					System.out.println("Not supported: " + processing[0]);
				}

				// TODO - Payload start, Accept (MIME types)
			}
		}
	}


}
