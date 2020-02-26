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
	private boolean expect100continue = false;
	private int contentLength;

	public Request() {
		// Empty constructor, no variable initialization required.
	}

	/**
	 * Is the request valid? (i.e either multipart/form-data or image/png)
	 *
	 * @return true if passed basic checks, false otherwise.
	 */
	public boolean isValidPOST() {
		// we only support multipart/form-data and binary uploads (image/png)
		if (!getContentType().equals("multipart/form-data") && !getContentType().equals("image/png")) {
			return false;
		}
		// content length has to be non-zero
		else if (getContentLength() <= 0) {
			return false;
		}
		// ensure boundary string is provided
		else if (getContentType().equals("multipart/form-data") && getBoundary().length() <= 0) {
			return false;
		}
		// Otherwise, everything should be reasonably OK.
		else {
			return true;
		}
	}

	/**
	 * User-Agent
	 *
	 * @return Request User-Agent
	 */
	public String getUserAgent() {
		return userAgent;
	}

	/**
	 * Set user agent
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * Get host (client IP address)
	 *
	 * @return Request host (IP or hostname)
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Set host
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * Connection HTTP header field (Keep-Alive, Close etc)
	 *
	 * @return Request connection parameter
	 */
	public String getConnection() {
		return connection;
	}

	/**
	 * Connection HTTP header field (Keep-Alive, Close etc)
	 */
	public void setConnection(String connection) {
		this.connection = connection;
	}

	/**
	 * Get HTTP Content-Type header value
	 *
	 * @return Request content-type
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * Set HTTP Content-Type header value
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * multipart/form-data boundary string
	 *
	 * @return Request multipart boundary string
	 */
	public String getBoundary() {
		return boundary;
	}

	/**
	 * multipart/form-data boundary string
	 */
	public void setBoundary(String boundary) {
		this.boundary = boundary;
	}

	/**
	 * HTTP Request line method (GET/POST/PUT etc)
	 *
	 * @return Request HTTP Method
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * HTTP Request line method (GET/POST/PUT etc)
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * HTTP URI ( i.e /index.html  )
	 *
	 * @return Request pathRequest
	 */
	public String getPathRequest() {
		return pathRequest;
	}

	/**
	 * HTTP URI ( i.e /index.html  )
	 */
	public void setPathRequest(String pathRequest) {
		this.pathRequest = pathRequest;
	}

	/**
	 * HTTP request line HTTP version
	 *
	 * @return Request HTTP version
	 */
	public String getHttpVersion() {
		return httpVersion;
	}

	/**
	 * HTTP request line HTTP version
	 */
	public void setHttpVersion(String httpVersion) {
		this.httpVersion = httpVersion;
	}

	/**
	 * Content-Length
	 *
	 * @return Request content length
	 */
	public int getContentLength() {
		return contentLength;
	}

	/**
	 * Set Content-Length field
	 */
	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	/**
	 * does client expect a 100 Continue response?
	 * @return true if expected, false if not
	 */
	public boolean isExpect100continue() {
		return expect100continue;
	}

	/**
	 * Sets boolean relating to if client expects a 100 Continue before sending their payload
	 */
	public void setExpect100continue(boolean expect100continue) {
		this.expect100continue = expect100continue;
	}
}
