package HTTPServer;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 * <p>
 * TODO: implement GET, HEAD, POST, PUT
 */

/* what a HTTP looks like:

GET /uri HTTP/1.1            | STATUS LINE
Host: 127.0.0.1:4950         | Headers
Connection: keep-alive

<body starts here, if any>   | Body
 */
public class RequestParser {
	private byte[] requestBytes;
	private String requestFull;
	private String[] requestLines;

	private String[] httpMain = null;

	private String userAgent;
	private String host;
	private String connection;
	private String contentType;
	private String contentLength;

	// TODO -- Check if more types are required.

	public RequestParser(byte[] req) {
		// Trim unnecessary variables as time goes on
		requestBytes = req;
		requestFull = new String(requestBytes);

		// Split on CRLF
		requestLines = requestFull.split("[\\r\\n]+");

		processData();

	}

	// Returns who is connected
	public String getUserAgent() {
		return userAgent;
	}

	// When you want to know how much data the client wants to PUT or POST.
	public int getContentLength() throws NumberFormatException {
		return Integer.parseInt(contentLength.trim());
	}

	// Get host that client wants to connect to
	public String getHost() {
		return host;
	}

	// Returns keep alive state request.
	public String getConnection() {
		return connection;
	}

	public String getContentType() {
		return contentType;
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
			// On first line, ex GET / HTTP/1.1
			// HTTP method is case sensitive!
			if (first) {
				processing = s.split("\\s+");
				httpMain = processing;
				first = false;
			}
			else {
				// User agent is split in a more sophisticated way, fix!
				if (processing[0].equalsIgnoreCase("User-Agent")) {
					userAgent = processing[1].trim();
				}
				else if (processing[0].equalsIgnoreCase("Host")) {
					host = processing[1].trim();
				}
				else if (processing[0].equalsIgnoreCase("Connection")) {
					connection = processing[1].trim();
				}
				else if (processing[0].equalsIgnoreCase("Content-Type")) {
					contentType = processing[1].trim();
				}
				else if (processing[0].equalsIgnoreCase("Content-Length")) {
					contentLength = processing[1].trim();
				}
				else {
					System.out.println("Not supported: " + processing[0]);
				}

				// TODO - Payload start, Accept (MIME types)
			}
		}
	}


}
