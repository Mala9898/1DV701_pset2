package HTTPServer;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 */

// credit: inspired by https://howtodoinjava.com/java/enum/java-enum-string-example/

public enum StatusCode {
    SUCCESS_200_OK("200 OK"),
    SUCCESS_201_CREATED("201 Created"),
    SUCCESS_204_NO_CONTENT("204 No Content"),
    REDIRECTION_301_MOVED_PERMANENTLY("301 Moved Permanently"),
    REDIRECTION_304_NOT_MODIFIED("304 Not Modified"),
    REDIRECTION_307_TEMPORARY_REDIRECT("307 Temporary Redirect"),
    REDIRECTION_308_PERMANENT_REDIRECT("308 Permanent Redirect"),
    CLIENT_ERROR_400_BAD_REQUEST("400 Bad Request"),
    CLIENT_ERROR_401_UNAUTHORIZED("401 Unauthorized"),
    CLIENT_ERROR_403_FORBIDDEN("403 Forbidden"),
    CLIENT_ERROR_404_NOT_FOUND("404 Not Found"),
    CLIENT_ERROR_408_REQUEST_TIMEOUT("408 Request Timeout"),
    CLIENT_ERROR_411_LENGTH_REQUIRED("411 Length Required"),
    CLIENT_ERROR_413_PAYLOAD_TOO_LARGE("413 Payload Too Large"),
    CLIENT_ERROR_414_URI_TOO_LONG("414 URI Too Long"),
    CLIENT_ERROR_415_UNSUPPORTED_MEDIA_TYPE("415 Unsupported Media Type"),
    CLIENT_ERROR_418_IM_A_TEAPOT("418 I'm a teapot"),
    CLIENT_ERROR_451_UNAVAILABLE_FOR_LEGAL_REASONS("451 Unavailable For Legal Reasons"),
    SERVER_ERROR_500_INTERNAL_SERVER_ERROR("500 Internal Server Error"),
    SERVER_ERROR_501_NOT_IMPLEMENTED("501 Not Implemented"),
    SERVER_ERROR_503_SERVICE_UNAVAILABLE("503 Service Unavailable");

    private String statusCode;

    StatusCode(String statusCode) {
        this.statusCode = statusCode;
    }
}
