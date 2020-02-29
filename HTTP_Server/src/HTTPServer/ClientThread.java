package HTTPServer;

import HTTPServer.Abstractions.*;
import HTTPServer.Multipart.MultipartObject;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

/**
 * @author Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 * <p>
 * Client thread is a class that intends to handle a single HTTP transaction, from start to finish.
 */


public class ClientThread implements Runnable {
	private static final String INDEX_HTML = "/index.html";
	private static final String INDEX_HTM = "/index.htm";
	OutputStream outputStream = null;
	InputStream inputStream = null;
	private Random random = new Random();
	private Socket clientSocket;
	private File servingDirectory;

	// Constructor only needs serving directory and the socket where the HTTP connection is coming from.
	public ClientThread(Socket clientSocket, File directory, int timeout) throws IOException {
		this.clientSocket = clientSocket;
		this.servingDirectory = directory;
		clientSocket.setSoTimeout(timeout);

		outputStream = new BufferedOutputStream(clientSocket.getOutputStream());
		inputStream = clientSocket.getInputStream();
	}

	@Override
	public void run() {
		// Failure boolean prevents request handling, kills thread prematurely.
		boolean failure = false;

		// Sets up request objects
		RequestParser requestParser = new RequestParser();
		Request request = null;

		try {
			request = requestParser.parseRequest(inputStream);
		}
		// RequestParser throws IllegalArgument when the received request is malformed.
		catch (IllegalArgumentException e) {
			try {
				System.err.println("Bad request received");
				sendError(StatusCode.CLIENT_ERROR_400_BAD_REQUEST);
			}
			catch (IOException ex) {
				System.err.println("Error sending the error response: " + e.getMessage());
			}
			failure = true;
		}
		// RequestParser throws generic IO Error when it failed to receive.
		catch (SocketTimeoutException e) {
			System.err.println("Client did not manage to send data in time, terminating connection");
			try {
				sendError(StatusCode.CLIENT_ERROR_408_REQUEST_TIMEOUT);
			}
			catch (IOException ex) {
				System.err.println("Error sending the error response: " + e.getMessage());
			}
			failure = true;
		}
		catch (Exception e) {
			System.err.println("Generic IOException: " + e.getMessage());
		}

		if (!failure && request != null) {
			// Processes the request method, methods will send appropriate response.
			// Exceptions relating to errors during response creation are handled in handleRequest()
			handleRequest(request);
		}

		/*
		 *	src: https://stackoverflow.com/questions/17437950/when-does-an-http-1-0-server-close-the-connection
		 *
		 * "In HTTP 1.1, the server does not close the connection after sending the response UNLESS:
		 * the client sent a Connection: close request header, or the server sent a Connection: close response header.
		 * If such a response header exists, the client must close its end of the connection after receiving the response."
		 *
		 * In this server, we send the Connection: close header as part of the generic message generation.
		 * That is why we close the connection here.
		 */

		// Attempts to close the socket
		try {
			clientSocket.close();
		}
		catch (IOException e) {
			System.err.println("Closing socket failed: " + e.getMessage());
		}
		System.out.println("Thread terminating");
	}

	/**
	 * Handle HTTP request, switches between different cases of processing methods depending on HTTP method
	 *
	 * @param request The HTTP request in object form
	 */
	private void handleRequest(Request request) {
		String method = request.getMethod();
		try {
			switch (method) {
				case "GET":
					processGet(request);
					break;

				case "PUT":
					processPut(request);
					break;

				case "POST":
					processPost(request);
					break;

				case "HEAD":
				case "CONNECT":
				case "DELETE":
				case "OPTIONS":
				case "TRACE":
				case "PATCH":
					// Unimplemented requests for these methods sends a 405 to client
					sendError(StatusCode.CLIENT_ERROR_405_NOT_ALLOWED);
					break;

				default:
					// src: https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/501
					// Method requests not part of HTTP/1.1 will send a 501 not implemented, which appears to be the proper response for an unknown method
					sendError(StatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED);
					System.out.println("Not supported");
					break;
			}
		}
		catch (SocketTimeoutException e) {
			System.err.println("Client did not manage to send data in time, terminating connection");
			try {
				sendError(StatusCode.CLIENT_ERROR_408_REQUEST_TIMEOUT);
			}
			catch (IOException ex) {
				System.err.println("Failed to send error to client: " + e.getMessage());
			}
		}
		// Any general unhandled exception that arises when attempting to process a request will send a 500 internal error to the client.
		catch (IOException e) {
			System.err.println("Error when processing request: " + e.getMessage());
			try {
				sendError(StatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
			}
			catch (IOException ex) {
				System.err.println("Failed to send error to client: " + e.getMessage());
			}
		}
	}

	/**
	 * Processes a received GET Request, handles case where page is not found. Any IO exceptions are passed up to caller.
	 *
	 * @param request HTTP Request in object form
	 * @throws IOException if something goes wrong during processing
	 */
	private void processGet(Request request) throws IOException {
		String requestedPath = servingDirectory.getAbsolutePath() + request.getPathRequest();
		String finalPath = "";
		boolean error404 = false;
		StatusCode finalStatus = StatusCode.SUCCESS_200_OK;

		File requestedFile = new File(servingDirectory, request.getPathRequest());

		// -------- TEST REDIRECT FUNCTIONALITY --------
		if (request.getPathRequest().equals("/redirect")) {
			sendRedirect("/redirectlanding");
			return;
		}
		// Checks if requested path is allowed, or if we should send 403 and stop method.
		if (isPathForbidden(requestedFile, request)) {
			sendError(StatusCode.CLIENT_ERROR_403_FORBIDDEN);
			return;
		}

		// ----- hijack "/content" endpoint to serve a dynamically generated HTML page with listed content uploads
		// code below generates and sends this dynamic page
		if (request.getPathRequest().equals("/content") || request.getPathRequest().equals("/content/")) {
			// Generates the content body, puts it into a string
			String messageBody = generateContentPage("/content");

			// Constructs the response with our custom body
			ResponseBuilder responseBuilder = new ResponseBuilder();
			String body = responseBuilder.generateHTMLwithBody(messageBody);
			String header = responseBuilder.generateGenericHeader("text/html", StatusCode.SUCCESS_200_OK, body.length());

			// Sends the response
			outputStream.write(header.getBytes());
			outputStream.write(body.getBytes());
			outputStream.flush();
			return;
		}

		// If file is readable and is a directory, serve .HTML, .HTM (in order) if found. Otherwise, 404 not found.
		if (Files.isDirectory(Paths.get(requestedPath))) {
			finalPath = requestedPath + INDEX_HTML;

			// If index.html doesn't exist, try index.htm
			if (!Files.isReadable(Paths.get(finalPath))) {
				finalPath = requestedPath + INDEX_HTM;

				// If index html and index htm doesn't exist
				if (!Files.isReadable(Paths.get(finalPath))) {
					error404 = true;
				}
			}
		}
		else if (Files.isReadable(Paths.get(requestedPath))) {
			// If requested file is a single file and not directory, and is also readable.
			finalPath = requestedPath;
		}
		else {
			// If file or folder does not exist.
			error404 = true;
		}

		// If previous if-block indicates that resource does not exist, set response to path 404.html and 404 header.
		if (error404) {
			sendError(StatusCode.CLIENT_ERROR_404_NOT_FOUND);
		}
		else {
			// Otherwise, send requested content.
			sendContentResponse(finalPath, finalStatus);
		}
	}

	/**
	 * Processes a received POST Request. Exceptions are thrown to caller
	 * We implement a /content REST API endpoint here
	 *
	 * @param request HTTP Request in object form
	 * @throws IOException if something goes wrong during processing
	 */
	private void processPost(Request request) throws IOException {
		System.out.println("GOT POST REQUEST!");
		System.out.printf("content-type={%s} boundary={%s} %n", request.getContentType(), request.getBoundary());

		// Implementation of client error 411, length field is required when using POST/PUT
		if (request.getContentLength() == null) {
			sendError(StatusCode.CLIENT_ERROR_411_LENGTH_REQUIRED);
			System.out.println("Length was expected but not parsed, sending 411 length required");
			return;
		}

		// Simple validity check
		if (!request.isValidPOST()) {
			sendError(StatusCode.CLIENT_ERROR_400_BAD_REQUEST);
			return;
		}

		// We only serve one endpoint, send 403 if anything else.
		if (!request.getPathRequest().equals("/content") && !request.getPathRequest().equals("/content/")) {
			sendError(StatusCode.CLIENT_ERROR_403_FORBIDDEN);
			return;
		}

		// send 100 Continue if client requested it
		if (request.isExpect100continue()) {
			send100Continue();
		}

		BodyParser bodyParser = new BodyParser();
		if (request.getContentType().equals("multipart/form-data")) {
			// HERE WE HAVE ACCESS TO ALL THE INDIVIDUAL MULTIPART OBJECTS! (including, name, filename, payload etc.)
			// But we restrict ourselves to only one image upload :)

			// Gets the multipart content
			List<MultipartObject> payloadData = bodyParser.getMultipartContent(inputStream, request.getContentLength(), request.getBoundary());

			if (payloadData.size() == 1) {
				// get the first multipart/form-data object
				MultipartObject multipartObject = payloadData.get(0);

				// only save png image, otherwise send 415 media type unsupported
				if (multipartObject.getDispositionContentType().equals("image/png")) {

					Path destination = Paths.get(servingDirectory + "/content/" + multipartObject.getDispositionFilename());
					File requestedFile = new File(String.valueOf(destination));

					// check if resource already exists, sets random filename preamble if this is the case
					if (requestedFile.exists()) {
						multipartObject.setDispositionFilename(getRandomFilename(multipartObject.getDispositionFilename()));
					}

					System.out.printf("saving {%s} %n", multipartObject.getDispositionFilename());

					// Attempts to write file, rethrows exception to be handled upwards if this breaks
					try (OutputStream out = new FileOutputStream(servingDirectory.getAbsolutePath() + "/content/" + multipartObject.getDispositionFilename())) {
						out.write(multipartObject.getData());
					}
					// rethrows on failure to be handled by handleRequest()
					catch (IOException e) {
						System.out.println("Couldn't save file: " + e.getMessage());
						throw e;
					}
					// Sends 201 created on success
					sendHeaderResponse("/content/" + multipartObject.getDispositionFilename(), StatusCode.SUCCESS_201_CREATED);

					System.out.printf("sent RESPONSE! {%s} %n", "/content/" + multipartObject.getDispositionFilename());
				}
				else {
					// If content-type inside the multipart data was unexpected, send 415.
					sendError(StatusCode.CLIENT_ERROR_415_UNSUPPORTED_MEDIA_TYPE);
				}
			}
			else {
				System.err.println("Did not receive a single image");
				sendError(StatusCode.CLIENT_ERROR_400_BAD_REQUEST);
			}
		}
		else {
			// Sends 415 if media type was not multipart/form-data.
			sendError(StatusCode.CLIENT_ERROR_415_UNSUPPORTED_MEDIA_TYPE);
		}
	}

	/**
	 * Processes a received PUT request. Exceptions are thrown to caller.
	 *
	 * @param request HTTP Request in object form
	 * @throws IOException if something goes wrong during processing
	 */
	private void processPut(Request request) throws IOException {
		// Implementation of client error 411, length field is required when using POST/PUT
		if (request.getContentLength() == null) {
			sendError(StatusCode.CLIENT_ERROR_411_LENGTH_REQUIRED);
			System.out.println("Length was expected but not parsed, sending 411 length required");
			return;
		}

		BodyParser bodyParser = new BodyParser();

		Path destination = Paths.get(servingDirectory + request.getPathRequest());
		File requestedFile = new File(String.valueOf(destination));

		// Checks if requested path is allowed, or if we should send 403 and stop method.
		if (isPathForbidden(requestedFile, request)) {
			sendError(StatusCode.CLIENT_ERROR_403_FORBIDDEN);
			return;
		}

		// send 100 Continue if client requested it
		if (request.isExpect100continue()) {
			send100Continue();
		}

		// check if resource already exists
		boolean exists = requestedFile.exists();
		byte[] payloadData = bodyParser.getBinaryContent(inputStream, request.getContentLength());

		// Affirms that the payload data was equal to the content length, otherwise sends a bad request.
		if (payloadData.length != request.getContentLength()) {
			sendError(StatusCode.CLIENT_ERROR_400_BAD_REQUEST);
			return;
		}

		if (destination.toString().endsWith(".png")) {

			// we don't allow creating resources under nested directories, checks the amount of backslashes in the converted path
			// FOR SOME REASON WINDOWS AND MAC HAVE DIFFERENT REPRESENTATIONS OF FORWARD SLASHES. THIS TAKES CARE OF BOTH.
			long dirCountWindows = destination.toString().chars().filter(ch -> ch == '\\').count();
			long dirCountUnix = destination.toString().chars().filter(ch -> ch == '/').count();
			if (dirCountWindows != 2 && dirCountUnix != 2) {
				sendError(StatusCode.CLIENT_ERROR_403_FORBIDDEN);
				return;
			}

			// write or overwrite depending exist state
			System.out.println("Attempting write...");
			try (OutputStream out = new FileOutputStream(requestedFile)) {
				out.write(payloadData);
			}
			catch (IOException e) {
				System.out.println("Failed to save file:  " + e.getMessage());
				throw e;
			}

			if (exists) {
				// send 204 no content if file existed
				sendHeaderResponse(request.getPathRequest(), StatusCode.SUCCESS_204_NO_CONTENT);
			}
			else {
				// send 201 created if new
				sendHeaderResponse(request.getPathRequest(), StatusCode.SUCCESS_201_CREATED);
			}
		}
		else if (destination.endsWith("content")) {
			sendError(StatusCode.CLIENT_ERROR_403_FORBIDDEN);
		}
		else {
			sendError(StatusCode.CLIENT_ERROR_415_UNSUPPORTED_MEDIA_TYPE);
		}
	}

	/**
	 * send a 4XX/5XX error response
	 *
	 * @param statusCode Code to send
	 * @throws IOException if we failed to send error because of stream issues
	 */
	private void sendError(StatusCode statusCode) throws IOException {
		ResponseBuilder responseBuilder = new ResponseBuilder();
		String body;
		String header;

		body = responseBuilder.generateHTMLMessage(statusCode.getCode());
		header = responseBuilder.generateGenericHeader("text/html", statusCode, body.length());
		outputStream.write(header.getBytes());
		outputStream.write(body.getBytes());
		outputStream.flush();
	}

	/**
	 * sends a 302 redirect response which the browsers will go to
	 *
	 * @param redirectLocation the location where the browser will end up
	 * @throws IOException if stream failed
	 */
	private void sendRedirect(String redirectLocation) throws IOException {
		ResponseBuilder responseBuilder = new ResponseBuilder();
		String toWrite = responseBuilder.relocateResponse(redirectLocation);
		outputStream.write(toWrite.getBytes());
		outputStream.flush();
	}

	/**
	 * sends a file (HTML page) to client
	 *
	 * @param path        Path to the file
	 * @param finalStatus StatusCode to send with the response
	 * @throws IOException If stream failed
	 */
	private void sendContentResponse(String path, StatusCode finalStatus) throws IOException {
		/*
		If you debug and look at the requested paths, you will see that the 'path' variable mixes (/) and (\), this still works fine with java.io.File.
		Even with a double // or double \\, it (io.File) filters this out and still works.
        */
		ResponseBuilder responseBuilder = new ResponseBuilder();
		File f = new File(path);
		System.out.println("Outputting to stream: " + f.getAbsolutePath());
		byte[] headerBytes = (responseBuilder.generateGenericHeader(URLConnection.guessContentTypeFromName(f.getName()), finalStatus, f.length())).getBytes();
		byte[] contentBytes = Files.readAllBytes(Paths.get(f.getAbsolutePath()));
		if (f.canRead()) {
			outputStream.write(headerBytes);
			outputStream.write(contentBytes);
			// flush() tells stream to send the bytes into the TCP stream
			outputStream.flush();
		}
	}

	/**
	 * sends a HTTP formatted header
	 *
	 * @param contextFile URI Resource included in header response
	 * @param finalStatus Status to send with this message
	 * @throws IOException If stream failed
	 */
	private void sendHeaderResponse(String contextFile, StatusCode finalStatus) throws IOException {
		ResponseBuilder responseBuilder = new ResponseBuilder();
		byte[] headerBytes = responseBuilder.generatePOSTPUTHeader(finalStatus, contextFile).getBytes();
		outputStream.write(headerBytes);
		outputStream.flush();
	}

	/**
	 * Check if if someone tries to get out of the intended pathway, return true if OK, false if trying to access something they shouldn't.
	 *
	 * @param requestedFile The file that was requested in the HTTP request.
	 * @param request       The HTTP Request itself
	 * @return true if forbidden, false otherwise
	 * @throws IOException if a file error occurs
	 */
	private boolean isPathForbidden(File requestedFile, Request request) throws IOException {
		// -------- 403 FORBIDDEN FUNCTIONALITY --------
		// Checks if client tried to access the /forbidden folder itself or any files inside it.
		if (request.getPathRequest().startsWith("/forbidden")) {
			return true;
		}
		// Checks for ../ hacks, returns false if request tried to move upwards in directory structure.
		else if (!requestedFile.getCanonicalPath().startsWith(servingDirectory.getCanonicalPath())) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Given a file "file1.png" exists, this returns "file2.png"
	 *
	 * @param filename filename to be combined with number
	 * @return a default random number along with the filename
	 */
	private String getRandomFilename(String filename) {
		return random.nextInt() + filename;
	}

	/**
	 * sends 100 Continue HTTP response
	 */
	private void send100Continue() throws IOException {
		System.out.println("SENDING 100 continue");
		outputStream.write("HTTP/1.1 100 Continue\r\n\r\n".getBytes());
		outputStream.flush();
	}

	/**
	 * Generates a dynamic content page based on the contents of the folder passed as an input argument.
	 *
	 * @param subDir A subdirectory of the serving directory
	 * @return A string containing a basic HTML body with links to every resource in the folder.
	 * @throws FileNotFoundException if folder didn't exist
	 */
	private String generateContentPage(String subDir) throws FileNotFoundException {
		File file = new File(servingDirectory.getAbsolutePath() + subDir);
		String[] files = file.list();
		// if file.list() returns null, content wasn't found or something happened
		if (files == null) {
			throw new FileNotFoundException("Unable to get content directory");
		}

		// dynamically generated html page
		// Builds a string that represents a basic HTML body containing links to resources inside the /content folder
		StringBuilder message = new StringBuilder();
		if (files.length <= 0) {
			message.append("<p>No content currently exists on the server.</p>");
		}
		else {
			message.append("<ul>\n");
			for (String s : files) {
				message.append(String.format("<li><a href=\"%s\">%s</a></li>", "/content/" + s, s));
			}
			message.append("</ul>\n");
		}
		return message.toString();
	}
}