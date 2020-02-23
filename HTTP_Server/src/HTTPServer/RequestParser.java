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

	public RequestParser() {

	}

	public Request parse(byte[] req) throws IllegalArgumentException {

		// Trim unnecessary variables as time goes on
		requestBytes = req;
		requestFull = new String(requestBytes);

		// Split on CRLF
		requestLines = requestFull.split("[\\r\\n]+");

		Request toReturn = new Request();

		String[] processing;
		boolean first = true;
		for (String line : requestLines) {
			processing = line.split(":");
			// On first line, ex GET / HTTP/1.1
			// HTTP method is case sensitive!
			if (first) {
				processing = line.split("\\s+");
				if (processing.length != 3) {
					throw new IllegalArgumentException();
				}
				else {
					toReturn.setMethod(processing[0]);
					toReturn.setPathRequest(processing[1]);
					toReturn.setHttpVersion(processing[2]);
					first = false;
				}
			}
			else {
				// User agent is split in a more sophisticated way, fix!
				if (processing[0].equalsIgnoreCase("User-Agent")) {
					toReturn.setUserAgent(processing[1].trim());
				}
				else if (processing[0].equalsIgnoreCase("Host")) {
					toReturn.setHost(processing[1].trim());
				}
				else if (processing[0].equalsIgnoreCase("Connection")) {
					toReturn.setConnection(processing[1].trim());
				}
				else if (processing[0].equalsIgnoreCase("Content-Type")) {
					// Special multipart processing
					// Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
					Pattern pattern = Pattern.compile("^Content-Type:[\\s]{0,1}(?<contentType>[\\w\\/-]+)(?:\\s*;\\s*boundary\\s*=\\s*\"?(?<boundary>[\\w-]*))?[\" ]?$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
					Matcher matcher = pattern.matcher(line);

					while (matcher.find()) {
						System.out.printf("group count: %d %n", matcher.groupCount());
						if (matcher.groupCount() == 1) {
							toReturn.setContentType(matcher.group("contentType"));
						}
						else if (matcher.groupCount() == 2) {
							toReturn.setContentType(matcher.group("contentType"));
							toReturn.setBoundary("--" + matcher.group("boundary")); // boundaries are always prefixed by additional double dashes
						}
					}
				}
				else if (processing[0].equalsIgnoreCase("Content-Length")) {
					toReturn.setContentLength(Integer.parseInt(processing[1].trim()));
				}
				else {
					System.out.println("Not supported: " + processing[0]);
				}
			}
		}
		return toReturn;
	}

}
