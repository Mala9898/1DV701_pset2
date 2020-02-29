package HTTPServer.Abstractions;

import java.util.Date;


/**
 * Class that creates general HTTP constructs
 *
 * @author Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 */
public class ResponseBuilder {

	public static final String CRLF = "\r\n";

	/**
	 * Returns
	 *
	 * @param statusCode      StatusCode to return
	 * @param contentLocation The location of the newly created or updated resource.
	 * @return a generated header containing relevant response and header fields
	 */
	public String generatePOSTPUTHeader(StatusCode statusCode, String contentLocation) {
		StringBuilder header = new StringBuilder();
		header.append(getGenerics(statusCode, "text/html"));
		switch (statusCode) {
			case SUCCESS_201_CREATED:
			case SUCCESS_204_NO_CONTENT:
				header.append("Content-Location: ").append(contentLocation).append(CRLF);
				break;

			default:
				throw new IllegalArgumentException("StatusCode did not match header requiring a contentLocation field");
		}
		header.append(CRLF); // end of header is indicated by two CRLFs. We add the last one here.
		return header.toString();
	}

	/**
	 * @param contentType Content type of body
	 * @param statusCode  Status code to put in message
	 * @param length      Byte length of body
	 * @return A string containing a generic HTTP header, ready to be sent.
	 */
	public String generateGenericHeader(String contentType, StatusCode statusCode, long length) {
		StringBuilder header = new StringBuilder();
		header.append(getGenerics(statusCode, contentType));
		header.append("Content-Length: ").append(length).append(CRLF);
		header.append(CRLF); // end of header is indicated by two CRLFs. We add the last one here.
		return header.toString();
	}

	/**
	 * @param location The URI to relocate the client to
	 * @return An HTTP redirect header, ready to be sent.
	 */
	public String relocateResponse(String location) {
		StringBuilder message = new StringBuilder();
		message.append(getGenerics(StatusCode.REDIRECTION_302_FOUND, "text/html"));
		message.append("Location: ").append(location).append(CRLF);
		message.append(CRLF);

		message.append(generateHTMLMessage("moved to <a href=\"" + location + "\">"));

		return message.toString();
	}

	/**
	 * @param status      StatusCode to include in response line
	 * @param contentType ContentType to include in the Content-Type: header
	 * @return A String containing the generic headers
	 */
	private String getGenerics(StatusCode status, String contentType) {
		return "HTTP/1.1 " + status.getCode() + CRLF +
				"Server: Assignment 2 Server" + CRLF +
				"Date: " + new Date() + CRLF +
				"Content-Type: " + contentType + CRLF +
				"Connection: close" + CRLF;
	}

	/**
	 * Returns a simple HTML document with a message
	 *
	 * @param message The massage to put into the basic HTML document inside an h1 tag
	 * @return a basic but syntax complete HTML document
	 */
	public String generateHTMLMessage(String message) {
		return "<!DOCTYPE html>\n" +
				"<html lang=\"en\">\n" +
				"<head>\n" +
				"    <title>Webserver</title>\n" +
				"</head>\n" +
				"<body>\n" +
				"<h1>" + message + "</h1>\n" +
				"<a href=\"index.html\">Back to homepage</a>" +
				"</body>\n" +
				"</html>";
	}

	/**
	 * Returns a simple HTML document with provided body
	 *
	 * @param body The body to put into the basic HTML document
	 * @return a basic HTML document wit the body defined by the body variable.
	 */
	public String generateHTMLwithBody(String body) {
		return "<!DOCTYPE html>\n" +
				"<html lang=\"en\">\n" +
				"<head>\n" +
				"    <title>Webserver</title>\n" +
				"</head>\n" +
				"<body>\n" +
				body + "\n" +
				"</body>\n" +
				"</html>";
	}
}
