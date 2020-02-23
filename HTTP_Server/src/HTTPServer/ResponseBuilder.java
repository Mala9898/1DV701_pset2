package HTTPServer;

import java.util.Date;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 */


public class ResponseBuilder {

    private ResponseBuilder() {
        // Private constructor to hide the implicit public constructor
    }

    public static final String CRLF = "\r\n";

    public static String generatePOSTPUTHeader(String contentType, StatusCode statusCode, long length, String contentLocation) {
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

    public static String generateGenericHeader(String contentType, StatusCode statusCode, long length) {
        StringBuilder header = new StringBuilder();
        header.append("HTTP/1.1 ").append(statusCode.getCode()).append(CRLF);
        header.append("Server: assignment 2 server" + CRLF);
        header.append("Date: ").append(new Date()).append(CRLF);
        header.append("Content-Type: ").append(contentType).append(CRLF);
        header.append("Content-Length: ").append(length).append(CRLF);
        header.append(CRLF); // end of header is indicated by two CRLFs. We add the last one here.
        return header.toString();
    }
}
