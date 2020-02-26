package HTTPServer.Abstractions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/* what a HTTP request looks like:

GET /uri HTTP/1.1            | STATUS LINE
Host: 127.0.0.1:4950         | Headers
Connection: keep-alive

<body starts here, if any>   | Body
 */

/**
 * @author Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 * <p>
 * Receives, parses and handles errors when dealing with a HTTP Request.
 * Purpose of this class is to return a 'nicer' Request object that holds all relevant information about the HTTP request.
 */
public class RequestParser {

	/**
	 * Gets and parses a HTTP request, neatly packs up data into an object
	 *
	 * @param input The input stream to pass to getRequest().
	 * @return The HTTP request in a simpler to user object form with relevant setters and getters.
	 * @throws IllegalArgumentException if request line was found to be malformed
	 * @throws IOException Passes the IOException from getRequest() up to caller. (Stream failure)
	 */
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
				else if(processing[0].equalsIgnoreCase("Expect")) {
					if(processing[1].trim().equalsIgnoreCase("100-continue")) {
						toReturn.setExpect100continue(true);
					}
				}
				else {
					System.out.println("Not supported: " + processing[0]);
				}
			}
		}
		return toReturn;
	}

	/**
	 * Gets and splits an incoming request into a String[] array, split on CRLF. Blocks until CRLFx2 comes in.
	 * @param inputStream The stream where data is expected to be found
	 * @return A string array where the lines are the request header lines found in a typical HTTP request (splits on CRLF)
	 * @throws IOException If something goes wrong with the stream
	 */
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
			// When -1 is read, client has sent FIN prematurely, malformed HTTP request
			else {
				throw new IOException("Host closed their connection");
			}
		}
		// Returns a String array that is split on CRLF.
		return new String(bytes.toByteArray()).split("[\\r\\n]+");
	}

}
