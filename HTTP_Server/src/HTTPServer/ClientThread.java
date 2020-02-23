package HTTPServer;

import HTTPServer.Multipart.MultipartObject;

import java.io.*;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 */


public class ClientThread implements Runnable {

	private static final String INDEX_HTML = "/index.html";
	private static final String INDEX_HTM = "/index.htm";
	private static final String ERR_404 = "/404.html";
	private String error404Path;
	private Socket clientSocket;
	private File servingDirectory;
	private final int REQUEST_BUFFER_LEN = 90000;

	OutputStream outputStream = null;
	InputStream inputStream = null;

	public ClientThread(Socket clientSocket, File directory) {
		this.clientSocket = clientSocket;
		this.servingDirectory = directory;
		error404Path = servingDirectory.getAbsolutePath() + ERR_404;
	}

	@Override
	public void run() {
		// Failure boolean prevents request handling, kills thread prematurely.
		boolean failure = false;

		try {
			outputStream = new BufferedOutputStream(clientSocket.getOutputStream());
			inputStream = clientSocket.getInputStream();
		}
		catch (IOException e) {
			System.err.println("Error when creating input or output stream: " + e.getMessage());
			failure = true;
		}

		RequestParser requestParser = new RequestParser();
		Request request = null;

		try {
			request = requestParser.parseRequest(inputStream);
		}
		catch (IOException e) {
			try {
				System.err.println("Bad request received");
				sendError(StatusCode.CLIENT_ERROR_400_BAD_REQUEST);
			}
			catch (IOException ex) {
				System.err.println("Error sending failed: " + e.getMessage());
			}
			failure = true;
		} catch (Exception e) {
			System.err.println("IOException: "+e.getMessage());
		}

		// Processes the request method, methods will send appropriate response.
		if (!failure) {
			try {
				String method = request.getMethod();
				if (method.equals("GET")) {
					processGet(request);
				}
				else if (method.equals("PUT")) {
					processPut(request);
				}
				else if (method.equals("POST")) {
					processPost(request);
				}
				else if (method.equals("HEAD")) {
					// TODO
//					 processHead();
				}
				else if (method.equals("CONNECT") || method.equals("DELETE") || method.equals("OPTIONS") || method.equals("TRACE") || method.equals("PATCH")) {
					sendError(StatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED);
				}
				else if (method.equals("HEAD")) {
					// processHead();
				}
				else {
					sendError(StatusCode.CLIENT_ERROR_400_BAD_REQUEST);
					System.out.println("Not supported");
				}
			}
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

		// Attempts to close the socket
		try {
			clientSocket.close();
		}
		catch (IOException e) {
			System.err.println("Closing socket failed: " + e.getMessage());
		}
		System.out.println("Thread terminating");
	}

	// Processes a received GET Request, handles case where page is not found. Any IO exceptions are passed up to the run() method.
	private void processGet(Request request) throws IOException {
		String requestedPath = servingDirectory.getAbsolutePath() + request.getPathRequest();
		String finalPath = "";
		boolean error404 = false;
		StatusCode finalStatus = StatusCode.SUCCESS_200_OK;

		// TODO - maybe rewrite this to avoid creating a file object, maybe a path object is enough?
		File requestedFile = new File(servingDirectory, request.getPathRequest());

		// -------- TEST REDIRECT FUNCTIONALITY --------
		if(request.getPathRequest().equals("/redirect")) {
			sendRedirect("/redirectlanding");
			return;
		}

		// -------- 403 FORBIDDEN FUNCTIONALITY --------
		if(request.getPathRequest().startsWith("/forbidden")) {
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

	private void processPost(Request request) throws IOException {
		System.out.println("GOT POST REQUEST!");
		System.out.printf("content-type={%s} boundary={%s} %n", request.getContentType(), request.getBoundary());

		BodyParser bodyParser = new BodyParser();

		if (request.getContentType().equals("multipart/form-data")) {
			// HERE WE HAVE ACCESS TO ALL THE INDIVIDUAL MULTIPART OBJECTS! (including, name, filename, payload etc.)

			ArrayList<MultipartObject> payloadData = bodyParser.getMultipartContent(inputStream, request.getContentLength(), request.getBoundary());

			if (payloadData.size() >= 1) {
				for (MultipartObject multipartObject : payloadData) {
					// only save png images
					if(multipartObject.getDispositionContentType().equals("image/png")){
						System.out.printf("saving {%s} %n", multipartObject.getDispositionFilename());
						try (OutputStream out = new FileOutputStream(servingDirectory.getAbsolutePath() + "/uploaded/" + multipartObject.getDispositionFilename())) {
							out.write(multipartObject.getData());
						}
						catch (Exception e) {
							System.out.println("Couldn't save file: " + e.getMessage());
							e.printStackTrace();
						}
					}
				}
			}
			else {
				System.err.println("NO MULTIPART DATA FOUND");
				sendError(StatusCode.CLIENT_ERROR_400_BAD_REQUEST);
				return;
			}

			// TODO -- Send response
			// 201 created if new
			// 200 OK or 204 if replacing
		}
		else if (request.getContentType().equals("image/png")) {
			byte[] payloadData = bodyParser.getBinaryContent(inputStream, request.getContentLength());
			Path writeDestination = Paths.get(servingDirectory + "/uploaded/FINALE.png");

			System.out.println("writing a file...");
			try (OutputStream out = new FileOutputStream(servingDirectory.getAbsolutePath() + "/uploaded//FINALE.png")) {
				out.write(payloadData);
			}
			catch (Exception e) {
				System.out.println("Something went wrong: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private void processPut(Request request) throws IOException {

		BodyParser bodyParser = new BodyParser();

		boolean internalError = false;
		Path destination = Paths.get(servingDirectory + request.getPathRequest());
		File requestedFile = new File(String.valueOf(destination));

		// prevent "../../" hacks
		// https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/File.html#getCanonicalPath()
		//  "removes redundant names such as "." and ".." from the pathname,
		//   resolving symbolic links (on UNIX platforms), and converting drive letters to a standard case (on Microsoft Windows platforms)."

		// TODO - Check if this actually still works, changed servingDirectory.getPath() to servingDirectory.getCanonicalPath().
		// TODO -- Move this into a separate method, reused code!!
		if (!requestedFile.getCanonicalPath().startsWith(servingDirectory.getCanonicalPath())) {
			// TODO - Send 403 Forbidden!
			System.err.println("400 bad request, terminating");
			System.exit(1);
		}
		// TODO - Make this actually function as intended, make sure no huge subfolder structures are created.
		if (requestedFile.getCanonicalPath().startsWith(servingDirectory.getCanonicalPath() + "\\upload\\")) {
			System.out.println("OK");
		}
		// check if resource already exists
		boolean exists = requestedFile.exists();
		byte[] payloadData = bodyParser.getBinaryContent(inputStream, request.getContentLength());

		// write or overwrite depending exist state
		System.out.println("Attempting write...");
		try (OutputStream out = new FileOutputStream(requestedFile)) {
			out.write(payloadData);
		}
		catch (Exception e) {
			System.out.println("Something went wrong: " + e.getMessage());
			internalError = true;
			e.printStackTrace();
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

	private void sendError(StatusCode statusCode) throws IOException {
		ResponseBuilder responseBuilder = new ResponseBuilder();
		String body = "";
		String header = "";
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