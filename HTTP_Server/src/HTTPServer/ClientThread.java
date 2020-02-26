package HTTPServer;

import HTTPServer.Abstractions.*;
import HTTPServer.Multipart.MultipartObject;

import java.io.*;
import java.net.Socket;
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
	private Random random = new Random();
	private static final String INDEX_HTML = "/index.html";
	private static final String INDEX_HTM = "/index.htm";
	private Socket clientSocket;
	private File servingDirectory;

	OutputStream outputStream = null;
	InputStream inputStream = null;

	// Constructor only needs serving directory and the socket where the HTTP connection is coming from.
	public ClientThread(Socket clientSocket, File directory) {
		this.clientSocket = clientSocket;
		this.servingDirectory = directory;
	}

	@Override
	public void run() {
		// Failure boolean prevents request handling, kills thread prematurely.
		boolean failure = false;

		// Sets up InputStream and OutputStream, set failure boolean if this throws.
		try {
			outputStream = new BufferedOutputStream(clientSocket.getOutputStream());
			inputStream = clientSocket.getInputStream();
		}
		catch (IOException e) {
			System.err.println("Error when creating input or output stream: " + e.getMessage());
			failure = true;
		}

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
				System.err.println("Error sending failed: " + e.getMessage());
			}
			failure = true;
		}
		// RequestParser throws generic IO Error when it failed to receive.
		catch (Exception e) {
			System.err.println("Generic IOException: " + e.getMessage());
		}

		if (!failure && request != null) {
			// Processes the request method, methods will send appropriate response.
			// Exceptions relating to errors during response creation are handled in handleRequest()
			handleRequest(request);
		}

		// Attempts to close the socket
		try {
			clientSocket.close();
		}
		catch (IOException e) {
			System.err.println("Closing socket failed: " + e.getMessage());
		}
		System.out.println("Thread terminating");
	}

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
					// TODO Implement this!
					processHead(request);
					break;

				case "CONNECT":
				case "DELETE":
				case "OPTIONS":
				case "TRACE":
				case "PATCH":
					// Unimplemented requests for these methods sends a 501 to client
					sendError(StatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED);
					break;

				default:
					// Method requests not part of HTTP/1.1 will send a 400 bad request to client
					sendError(StatusCode.CLIENT_ERROR_400_BAD_REQUEST);
					System.out.println("Not supported");
					break;
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
				ex.printStackTrace();
			}
		}
	}

	// Processes a received GET Request, handles case where page is not found. Any IO exceptions are passed up to caller.
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
		if (request.getPathRequest().equals("/content")) {
			File file = new File(servingDirectory.getAbsolutePath() + "/content");
			String[] files = file.list();

			StringBuilder message = new StringBuilder();
			if (files.length <= 0) {
				message.append("<p>No content currently exists on the server.</p>");
			}
			else {
				message.append("<ul>\n");
				for (String s : files) {
					message.append(String.format("<li><a href=\"%s\">%s</a></li>", "/content/" + s, s).toString());
				}
				message.append("</ul>\n");
			}
			ResponseBuilder responseBuilder = new ResponseBuilder();
			String body = responseBuilder.generateHTMLwithBody(message.toString());
			String header = responseBuilder.generateGenericHeader("text/html", StatusCode.SUCCESS_200_OK, body.length());
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
			return;
		}

		// Otherwise, send requested content.
		sendContentResponse(finalPath, finalStatus);

	}

	// Processes a received POST Request. Exceptions are thrown to caller
	// We implement a /content REST API endpoint here
	private void processPost(Request request) throws IOException {
		System.out.println("GOT POST REQUEST!");
		System.out.printf("content-type={%s} boundary={%s} %n", request.getContentType(), request.getBoundary());

		if (!request.isValidPOST()) {
			sendError(StatusCode.CLIENT_ERROR_400_BAD_REQUEST);
		}

		// We only serve one endpoint, send 403 if anything else.
		if (!request.getPathRequest().equals("/content")) {
			sendError(StatusCode.CLIENT_ERROR_403_FORBIDDEN);
		}

		BodyParser bodyParser = new BodyParser();
		if (request.getContentType().equals("multipart/form-data")) {
			// HERE WE HAVE ACCESS TO ALL THE INDIVIDUAL MULTIPART OBJECTS! (including, name, filename, payload etc.)
			// But we restrict ourselves to only one image upload :)
			List<MultipartObject> payloadData = bodyParser.getMultipartContent(inputStream, request.getContentLength(), request.getBoundary());

			if (payloadData.size() == 1) {
				// get the first multipart/form-data object
				MultipartObject multipartObject = payloadData.get(0);

				// only save png image
				if (multipartObject.getDispositionContentType().equals("image/png")) {

					Path destination = Paths.get(servingDirectory + "/content/" + multipartObject.getDispositionFilename());
					File requestedFile = new File(String.valueOf(destination));

					// check if resource already exists
					if (requestedFile.exists()) {
						multipartObject.setDispositionFilename(getIteratedFilename(multipartObject.getDispositionFilename()));
					}

					System.out.printf("saving {%s} %n", multipartObject.getDispositionFilename());

					// Attempts to write file, sends 500 internal error if failed
					try (OutputStream out = new FileOutputStream(servingDirectory.getAbsolutePath() + "/content/" + multipartObject.getDispositionFilename())) {
						out.write(multipartObject.getData());
					}
					catch (Exception e) {
						System.out.println("Couldn't save file: " + e.getMessage());
						sendError(StatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
						return;
					}
					// Sends 201 created on success
					sendHeaderResponse("/content/" + multipartObject.getDispositionFilename(), StatusCode.SUCCESS_201_CREATED);

					System.out.printf("sent RESPONSE! {%s} %n", "/content/" + multipartObject.getDispositionFilename());
				}
				else {
					// If content-type was unexpected, send 415.
					sendError(StatusCode.CLIENT_ERROR_415_UNSUPPORTED_MEDIA_TYPE);
				}
			}
			else {
				System.err.println("Did not receive a single image");
				sendError(StatusCode.CLIENT_ERROR_400_BAD_REQUEST);
			}
		}
		// ++++++ ONLY IF NEEDED ++++++++ we can assume POSTs are only over multipart/form-data <form>s?
		// TODO - Evaluate what we should do with this, this is currently incomplete.
		else if (request.getContentType().equals("image/png")) {
			byte[] payloadData = bodyParser.getBinaryContent(inputStream, request.getContentLength());

			System.out.println("writing a file...");
			try (OutputStream out = new FileOutputStream(servingDirectory.getAbsolutePath() + "/content//FINALE.png")) {
				out.write(payloadData);
			}
			catch (Exception e) {
				System.out.println("Something went wrong: " + e.getMessage());
				sendError(StatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
			}
		}
	}

	// Processes a received PUT request. Exceptions are thrown to caller.
	private void processPut(Request request) throws IOException {

		// https://stackoverflow.com/questions/797834/should-a-restful-put-operation-return-something

		BodyParser bodyParser = new BodyParser();

		boolean internalError = false;
		Path destination = Paths.get(servingDirectory + request.getPathRequest());
		File requestedFile = new File(String.valueOf(destination));

		// Checks if requested path is allowed, or if we should send 403 and stop method.
		if (isPathForbidden(requestedFile, request)) {
			sendError(StatusCode.CLIENT_ERROR_403_FORBIDDEN);
			return;
		}

		// check if resource already exists
		boolean exists = requestedFile.exists();
		byte[] payloadData = bodyParser.getBinaryContent(inputStream, request.getContentLength());

		if (request.getContentType().equals("image/png")) {

			// write or overwrite depending exist state
			System.out.println("Attempting write...");
			try (OutputStream out = new FileOutputStream(requestedFile)) {
				out.write(payloadData);
			}
			catch (Exception e) {
				System.out.println("Failed to save file:  " + e.getMessage());
				internalError = true;
			}

			if (internalError) {
				System.err.println("Internal Server Error");
				sendError(StatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
			}
			else if (exists) {
				// send 204 no content if file existed
				sendHeaderResponse(request.getPathRequest(), StatusCode.SUCCESS_204_NO_CONTENT);
			}
			else {
				// send 201 created if new
				sendHeaderResponse(request.getPathRequest(), StatusCode.SUCCESS_201_CREATED);
			}
		}
		else {
			sendError(StatusCode.CLIENT_ERROR_415_UNSUPPORTED_MEDIA_TYPE);
		}
	}

	private void processHead(Request request) {
		// TODO - Implement head
	}

	/**
	 * send a 4XX/5XX error response
	 * @param statusCode
	 * @throws IOException
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
	 * @param location
	 * @throws IOException
	 */
	private void sendRedirect(String location) throws IOException {
		ResponseBuilder responseBuilder = new ResponseBuilder();
		String toWrite = responseBuilder.relocateResponse(location);
		outputStream.write(toWrite.getBytes());
		outputStream.flush();
	}

	/**
	 * sends a file (HTML page) to client
	 * @param path
	 * @param finalStatus
	 * @throws IOException
	 */
	private void sendContentResponse(String path, StatusCode finalStatus) throws IOException {
		/*
		If you debug and look at the requested paths, you will see that the 'path' variable mixes (/) and (\), this still works fine with java.io.File.
		Even with a double // or double \\, it io.File filter this out and still works.
        */
		ResponseBuilder responseBuilder = new ResponseBuilder();
		File f = new File(path);
		System.out.println("Outputting to stream: " + f.getAbsolutePath());
		byte[] headerBytes = (responseBuilder.generateGenericHeader(URLConnection.guessContentTypeFromName(f.getName()), finalStatus, f.length())).getBytes();
		if (f.canRead()) {
			outputStream.write(headerBytes);
			outputStream.write(Files.readAllBytes(Paths.get(f.getAbsolutePath())));
			// flush() tells stream to send the bytes into the TCP stream
			outputStream.flush();
		}
	}

	/**
	 * sends a HTTP formatted header
	 * @param URI resource
	 * @param finalStatus
	 * @throws IOException
	 */
	private void sendHeaderResponse(String contextFile, StatusCode finalStatus) throws IOException {
		ResponseBuilder responseBuilder = new ResponseBuilder();
		byte[] headerBytes = responseBuilder.generatePOSTPUTHeader("text/html", finalStatus, 0, contextFile).getBytes();
		outputStream.write(headerBytes);
		outputStream.flush();
	}

	/**
	 * Check if if someone tries to get out of the intended pathway, return true if OK, false if trying to access something they shouldn't.
	 * @param requestedFile
	 * @param request
	 * @return
	 * @throws IOException
	 */
	private boolean isPathForbidden(File requestedFile, Request request) throws IOException {
		// -------- 403 FORBIDDEN FUNCTIONALITY --------
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
	private String getIteratedFilename(String filename) {
		return random.nextInt() + filename;
	}
}