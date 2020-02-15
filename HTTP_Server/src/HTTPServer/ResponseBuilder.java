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

    public static String generateHeader(String contentType,StatusCode statusCode, int length) {
        StringBuilder header = new StringBuilder();
        header.append("HTTP/1.1 "+statusCode.getCode()+CRLF); // TODO fix this hardcoded
        header.append("Server: assignment 2 server"+CRLF);
        header.append("Date: "+(new Date()) +CRLF);
        header.append("Content-Type: "+contentType +CRLF);
//        header.append("Content-Type: "+contentType+"; charset=UTF-8" +CRLF);
        header.append("Content-Length: " + length + CRLF);
        header.append(CRLF); // end of header is indicated by two CRLFs. We add the last one here.
        return header.toString();
    }
}
