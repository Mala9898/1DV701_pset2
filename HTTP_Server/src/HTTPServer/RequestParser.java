package HTTPServer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 * <p>
 * TODO: implement GET, HEAD, POST, PUT
 */

/* what a HTTP request looks like:

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
	private String boundary;


	private int contentLength;

	// TODO -- Check if more types are required.

	// TODO -- Throw IllegalArgument if a poorly formatted request is received, ex - if you wrote some garbage into telnet.

	public RequestParser(byte[] req) throws NumberFormatException {
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
	public int getContentLength() {
		return contentLength;
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

	public String getBoundary() {
		return boundary;
	}

	private void processData() throws NumberFormatException {
		String[] processing;
		boolean first = true;
		for (String line : requestLines) {
			processing = line.split(":");
			// On first line, ex GET / HTTP/1.1
			// HTTP method is case sensitive!
			if (first) {
				processing = line.split("\\s+");
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
					// Special multipart processing
					processContentType(line);
				}
				else if (processing[0].equalsIgnoreCase("Content-Length")) {
					contentLength = Integer.parseInt(processing[1].trim());
				}
				else {
					System.out.println("Not supported: " + processing[0]);
				}

				// TODO - Payload start, Accept (MIME types)
			}
		}
	}

	// TODO - Add extensive comments, what input is given, what happens to the input, what is the output
	// TODO - Refactor this to return a String instead of being a void method
	private void processContentType(String line) {
		//					contentType = processing[1].trim();
		// Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
		Pattern pattern = Pattern.compile("^Content-Type:[\\s]{0,1}(?<contentType>[\\w\\/-]+)(?:\\s*;\\s*boundary\\s*=\\s*\"?(?<boundary>[\\w-]*))?[\" ]?$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(line);

		while (matcher.find()) {
			System.out.printf("group count: %d %n", matcher.groupCount());
			if (matcher.groupCount() == 1) {
				contentType = matcher.group("contentType");
			}
			else if (matcher.groupCount() == 2) {
				contentType = matcher.group("contentType");
				boundary = "--" + matcher.group("boundary"); // boundaries are always prefixed by additional double dashes
			}
		}
	}
}
