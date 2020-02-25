package HTTPServer.Abstractions;

import java.util.Date;

/**
 * @author Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 */


public class ResponseBuilder {

    // this is now a an Object because TAs LOVE OBJECT ORIENTATION
//    private ResponseBuilder() {
//        // Private constructor to hide the implicit public constructor
//    }
    // TODO - Evaluate if this *really* needs to be an object.

    public static final String CRLF = "\r\n";

    public String generatePOSTPUTHeader(String contentType, StatusCode statusCode, long length, String contentLocation) {
        StringBuilder header = new StringBuilder();
        header.append("HTTP/1.1 ").append(statusCode.getCode()).append(CRLF);
        header.append("Server: assignment 2 server" + CRLF);
        header.append("Date: ").append(new Date()).append(CRLF);
        if (statusCode == StatusCode.SUCCESS_201_CREATED) {
            // Indicates URL of newly created item
            header.append("Content-Location: ").append(contentLocation).append(CRLF);
        }
        else if (statusCode == StatusCode.SUCCESS_204_NO_CONTENT) {
            // Indicated target of redirection
            header.append("Content-Location ").append(contentLocation).append(CRLF);
        }
        else {
            throw new IllegalArgumentException("StatusCode did not match header requiring a contentLocation field");
        }
        header.append("Content-Type: ").append(contentType).append(CRLF);
        header.append("Content-Length: ").append(length).append(CRLF);
        header.append(CRLF); // end of header is indicated by two CRLFs. We add the last one here.
        return header.toString();
    }

    public String generateGenericHeader(String contentType, StatusCode statusCode, long length) {
        StringBuilder header = new StringBuilder();
        header.append("HTTP/1.1 ").append(statusCode.getCode()).append(CRLF);
        header.append("Server: assignment 2 server" + CRLF);
        header.append("Date: ").append(new Date()).append(CRLF);
        header.append("Content-Type: ").append(contentType).append(CRLF);
        header.append("Content-Length: ").append(length).append(CRLF);
        header.append(CRLF); // end of header is indicated by two CRLFs. We add the last one here.
        return header.toString();
    }

    public String relocateResponse(String location) {
        StringBuilder message = new StringBuilder();
        message.append("HTTP/1.1 ").append(StatusCode.REDIRECTION_302_FOUND.getCode()).append(CRLF);
        message.append("Location: " + location + CRLF);
        message.append("Server: assignment 2 server" + CRLF);
        message.append("Date: ").append(new Date()).append(CRLF);
        message.append("Content-Type: text/html").append(CRLF);
        message.append(CRLF);

        message.append(generateHTMLMessage("moved to <a href=\"" + location + "\">"));

        return message.toString();
    }

    /**
     * Returns a simple HTML document with a message
     *
     * @param message The massage to put into the basic HTML document
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
}
