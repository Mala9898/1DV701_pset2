package HTTPServer.Abstractions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 * <p>
 * Receives, parses and handles errors when dealing with a HTTP Request.
 * Purpose of this class is to return a 'nicer' Request object that holds all relevant information about the HTTP request.
 */

/* what a HTTP request looks like:

GET /uri HTTP/1.1            | STATUS LINE
Host: 127.0.0.1:4950         | Headers
Connection: keep-alive

<body starts here, if any>   | Body
 */
public class RequestParser {

	public RequestParser() {
		// Empty constructor
		// TODO - Evaluate if this *really* needs to be an object.
	}

	// Parses request, throws IllegalArgumentException if bad header format is found. IOException if something happens during receive.
	// Returns a Request object that represents the received request.
	// TODO - Reduce size and complexity of this method!
	public Request parseRequest(InputStream input) throws IOException {
		// Gets a full request with lines split at CRLF. Blocks until CRLFx2 is received.
		String[] requestLines = getRequest(input);

		Request toReturn = new Request();
		String[] processing;
		boolean first = true;

		// On first line, ex GET / HTTP/1.1
		// HTTP method is case sensitive!
		for (String line : requestLines) {
			processing = line.split(":");
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
				// TODO User agent is split in a more sophisticated way, fix!
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

	// Gets and splits an incoming request into a String[] array, split on CRLF. Blocks until CRLFx2 comes in.
	private String[] getRequest(InputStream inputStream) throws IOException {
		// Holds all bytes
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();

		// Holds a single character in byte form
		byte read;

		// State booleans
		boolean loop = true;
		boolean first = true;
		boolean second = false;

		while (loop) {
			// Read bytes, valid header is ALWAYS in ASCII.
			if ((read = (byte) inputStream.read()) != -1) {
				System.out.print((char) read);
				// Add byte to list.
				bytes.write(read);
				// On CR or LF
				if (read == '\r' || read == '\n') {
					if (first) {
						// On CR
						first = false;
					}
					// SonarLint is wrong, second does turn true on CRLFx2!!
					// On CRLFx2, terminate while loop.
					else if (second) {
						loop = false;
					}
					else {
						// On LF
						second = true;
						first = true;
					}
				}
				// On any character other than CR or LF
				else {
					second = false;
					first = true;
				}
			}
			// When -1 is read, client has sent FIN.
			else {
				loop = false;
			}
		}
		// Returns a String array that is split on CRLF.
		return new String(bytes.toByteArray()).split("[\\r\\n]+");
	}

}
