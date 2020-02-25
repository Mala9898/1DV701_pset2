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

	private static final String INDEX_HTML = "/index.html";
	private static final String INDEX_HTM = "/index.htm";
	private static final String ERR_404 = "/404.html";
	private String error404Path;
	private Socket clientSocket;
	private File servingDirectory;

	OutputStream outputStream = null;
	InputStream inputStream = null;

	// Constructor only needs serving directory and the socket where the HTTP connection is coming from.
	public ClientThread(Socket clientSocket, File directory) {
		this.clientSocket = clientSocket;
		this.servingDirectory = directory;
		error404Path = servingDirectory.getAbsolutePath() + ERR_404;
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
					// TODO
//					 processHead();
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

		// TODO - maybe rewrite this to avoid creating a file object, maybe a path object is enough?
		File requestedFile = new File(servingDirectory, request.getPathRequest());

		// -------- TEST REDIRECT FUNCTIONALITY --------
		if (request.getPathRequest().equals("/redirect")) {
			sendRedirect("/redirectlanding");
			return;
		}

		// -------- 403 FORBIDDEN FUNCTIONALITY --------
		if (request.getPathRequest().startsWith("/forbidden")) {
			sendError(StatusCode.CLIENT_ERROR_403_FORBIDDEN);
			return;
		}


		if (!requestedFile.getCanonicalPath().startsWith(servingDirectory.getCanonicalPath())) {
			sendError(StatusCode.CLIENT_ERROR_403_FORBIDDEN);
			return;
		}

		// If file is readable and is a directory, start looking for HTML or HTM in that folder.
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
		// If requested file is a single file and not directory, and is also readable.
		else if (Files.isReadable(Paths.get(requestedPath))) {
			finalPath = requestedPath;
		}
		// If file or folder does not exist.
		else {
			error404 = true;
		}

		if (error404) {
			// If previous if-block indicates that resource does not exist, set response to path 404.html and 404 header.
			sendContentResponse(error404Path, StatusCode.CLIENT_ERROR_404_NOT_FOUND);
		}
		else {
			// Otherwise, send requested content.
			sendContentResponse(finalPath, finalStatus);
		}
	}

	// Processes a received POST Request. Exceptions are thrown to caller
	private void processPost(Request request) throws IOException {
		System.out.println("GOT POST REQUEST!");
		System.out.printf("content-type={%s} boundary={%s} %n", request.getContentType(), request.getBoundary());

		if(!request.isValidPOST())
			sendError(StatusCode.CLIENT_ERROR_400_BAD_REQUEST);

		// We only serve one endpoint
		if(!request.getPathRequest().equals("/content")) {
			sendError(StatusCode.CLIENT_ERROR_404_NOT_FOUND);
		}

		// everything is OK. Tell client they can go ahead and send the rest of the payload if they haven't already.
		// TODO somehow Insomnia doesn't recognize this?!
		//  outputStream.write("HTTP/1.1 100 Continue\r\n".getBytes());

		BodyParser bodyParser = new BodyParser();
		boolean internalError = false;
		if (request.getContentType().equals("multipart/form-data")) {
			// HERE WE HAVE ACCESS TO ALL THE INDIVIDUAL MULTIPART OBJECTS! (including, name, filename, payload etc.)
			// But we restrict ourselves to only one image upload :)
			List<MultipartObject> payloadData = bodyParser.getMultipartContent(inputStream, request.getContentLength(), request.getBoundary());

			if (payloadData.size() == 1) {
				// get the first multipart/form-data object
				MultipartObject multipartObject = payloadData.get(0);

				// only save png image
				if (multipartObject.getDispositionContentType().equals("image/png")) {

					Path destination = Paths.get(servingDirectory +"/content/" + multipartObject.getDispositionFilename());
					File requestedFile = new File(String.valueOf(destination));

					// check if resource already exists
					if(requestedFile.exists()) {
						Random random = new Random();
						multipartObject.setDispositionFilename(random.nextInt()+multipartObject.getDispositionFilename());
					}

					System.out.printf("saving {%s} %n", multipartObject.getDispositionFilename());
					try (OutputStream out = new FileOutputStream(servingDirectory.getAbsolutePath() + "/content/" + multipartObject.getDispositionFilename())) {
						out.write(multipartObject.getData());
					}
					catch (Exception e) {
						internalError = true;
						System.out.println("Couldn't save file: " + e.getMessage());
						sendError(StatusCode.SERVER_ERROR_500_INTERNAL_SERVER_ERROR);
						return;
					}
					sendHeaderResponse( "/content/"+multipartObject.getDispositionFilename(), StatusCode.SUCCESS_201_CREATED);
					System.out.printf("sent RESPONSE! {%s} %n", "/content/"+multipartObject.getDispositionFilename());
				}

			}
			else {
				System.err.println("Did not receive a single image");
				sendError(StatusCode.CLIENT_ERROR_400_BAD_REQUEST);
				return;
			}

			// 201 created if new + Location
			// !!!! [[[[200 OK if replacing]]]]: lets assume POST always creates a new resource.
		}
		else if (request.getContentType().equals("image/png")) {
			byte[] payloadData = bodyParser.getBinaryContent(inputStream, request.getContentLength());
			Path writeDestination = Paths.get(servingDirectory + "/content/FINALE.png");

			System.out.println("writing a file...");
			try (OutputStream out = new FileOutputStream(servingDirectory.getAbsolutePath() + "/content//FINALE.png")) {
				out.write(payloadData);
			}
			catch (Exception e) {
				System.out.println("Something went wrong: " + e.getMessage());
				e.printStackTrace();
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

		// check if resource already exists
		boolean exists = requestedFile.exists();
		byte[] payloadData = bodyParser.getBinaryContent(inputStream, request.getContentLength());

		// TODO -- Move this into a separate method, reused code!!
		if (!requestedFile.getCanonicalPath().startsWith(servingDirectory.getCanonicalPath())) {
			// TODO - Send 403 Forbidden!
			System.err.println("400 bad request, terminating");
			System.exit(1);
		}

		if (request.getContentType().equals("multipart/form-data")) {
			// TODO implement this
		}
		else if (request.getContentType().equals("image/png")) {

			// TODO - Make this actually function as intended, make sure no huge subfolder structures are created.
			if (requestedFile.getCanonicalPath().startsWith(servingDirectory.getCanonicalPath() + "\\upload\\")) {
				System.out.println("OK");
			}

			// write or overwrite depending exist state
			System.out.println("Attempting write...");
			try (OutputStream out = new FileOutputStream(requestedFile)) {
				out.write(payloadData);
			}
			catch (Exception e) {
				System.out.println("Something went wrong: " + e.getMessage());
				internalError = true;
			}


			if (exists) {
				// send 204 no content if file existed
				sendHeaderResponse(request.getPathRequest(), StatusCode.SUCCESS_204_NO_CONTENT);
			}
			else if (internalError) {
				System.err.println("Internal Server Error");
				// TODO - Send 500 internal server error
			}
			else {
				// send 201 created if new
				sendHeaderResponse(request.getPathRequest(), StatusCode.SUCCESS_201_CREATED);
			}
			// 200 OK if replacing
		}
		else {
			sendError(StatusCode.CLIENT_ERROR_415_UNSUPPORTED_MEDIA_TYPE);
		}
	}

	private void processHead(Request request) {
		// TODO - Implement head
	}

	private void sendError(StatusCode statusCode) throws IOException {
		ResponseBuilder responseBuilder = new ResponseBuilder();
		String body;
		String header;
		switch (statusCode) {
			case CLIENT_ERROR_404_NOT_FOUND:
//				error404Path
				body = responseBuilder.HTMLMessage("404 not found");
				header = responseBuilder.generateGenericHeader("text/html", StatusCode.CLIENT_ERROR_404_NOT_FOUND, body.length());
				outputStream.write(header.getBytes());
				outputStream.write(body.getBytes());
				outputStream.flush();
				break;

			case CLIENT_ERROR_403_FORBIDDEN:
				body = responseBuilder.HTMLMessage("403 Forbidden");
				header = responseBuilder.generateGenericHeader("text/html", StatusCode.CLIENT_ERROR_403_FORBIDDEN, body.length());
				outputStream.write(header.getBytes());
				outputStream.write(body.getBytes());
				outputStream.flush();
				break;

			case CLIENT_ERROR_400_BAD_REQUEST:
				body = responseBuilder.HTMLMessage("400 Bad Request");
				header = responseBuilder.generateGenericHeader("text/html", StatusCode.CLIENT_ERROR_400_BAD_REQUEST, body.length());
				outputStream.write(header.getBytes());
				outputStream.write(body.getBytes());
				outputStream.flush();
				break;
		}
	}

	private void sendRedirect(String location) throws IOException {
		ResponseBuilder responseBuilder = new ResponseBuilder();
		String toWrite = responseBuilder.relocateResponse(location);
		outputStream.write(toWrite.getBytes());
		outputStream.flush();
	}

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

	private void sendHeaderResponse(String contextFile, StatusCode finalStatus) throws IOException {
		ResponseBuilder responseBuilder = new ResponseBuilder();
		byte[] headerBytes = responseBuilder.generatePOSTPUTHeader("text/html", finalStatus, 0, contextFile).getBytes();
		outputStream.write(headerBytes);
		outputStream.flush();
	}

	// TODO - Check if if someone tries to get out of the intended pathway, return true if OK, false if trying to access something they shouldn't.
	private boolean checkPathAccess() {
		return false;
	}
}