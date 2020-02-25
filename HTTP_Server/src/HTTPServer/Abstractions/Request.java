package HTTPServer.Abstractions;

/**
 * @author Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-23
 * <p>
 * Getter and setter class that provides encapsulation for a typical HTTP header request.
 */

public class Request {

    private String userAgent;
    private String host;
    private String connection;
    private String contentType;
    private String boundary;
    private String method;
    private String pathRequest;
    private String httpVersion;
    private int contentLength;

    public Request() {
        // Empty constructor, no variable initialization required.
    }

    /**
     * Is the request valid? (i.e either multipart/form-data or image/png)
     * @return
     */
    public boolean isValidPOST() {
        // we only support multipart/form-data and binary uploads (image/png)
        if(!getContentType().equals("multipart/form-data") && !getContentType().equals("image/png")) {
            return false;
        }
        // content length has to be non-zero
        if(getContentLength() <= 0) {
            return false;
        }
        // ensure boundary string is provided
        if(getContentType().equals("multipart/form-data") && getBoundary().length() <= 0){
            return false;
        }

        return true;
    }

    /**
     * User-Agent
     * @return
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Set user agent
     * @param userAgent
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Get host (client IP address)
     * @return
     */
    public String getHost() {
        return host;
    }

    /**
     * Set host
     * @param host
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Connection HTTP header field (Keep-Alive, Close etc)
     * @return
     */
    public String getConnection() {
        return connection;
    }

    /**
     * Connection HTTP header field (Keep-Alive, Close etc)
     * @return
     */
    public void setConnection(String connection) {
        this.connection = connection;
    }

    /**
     * Get HTTP Content-Type header value
     * @return
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Set HTTP Content-Type header value
     * @return
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * multipart/form-data boundary string
     * @return
     */
    public String getBoundary() {
        return boundary;
    }

    /**
     * multipart/form-data boundary string
     * @return
     */
    public void setBoundary(String boundary) {
        this.boundary = boundary;
    }

    /**
     * HTTP Request line method (GET/POST/PUT etc)
     * @return
     */
    public String getMethod() {
        return method;
    }

    /**
     * HTTP Request line method (GET/POST/PUT etc)
     * @return
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * HTTP URI ( i.e /index.html  )
     * @return
     */
    public String getPathRequest() {
        return pathRequest;
    }

    /**
     * HTTP URI ( i.e /index.html  )
     * @return
     */
    public void setPathRequest(String pathRequest) {
        this.pathRequest = pathRequest;
    }

    /**
     * HTTP request line HTTP version
     * @return
     */
    public String getHttpVersion() {
        return httpVersion;
    }

    /**
     * HTTP request line HTTP version
     * @return
     */
    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    /**
     * Content-Length
     * @return
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * Content-Length
     * @return
     */
    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }
}
