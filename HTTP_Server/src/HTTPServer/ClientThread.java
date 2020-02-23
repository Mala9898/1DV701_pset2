package HTTPServer;

import HTTPServer.Multipart.MultipartObject;

import java.io.*;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 */

// TODO: implement static serving

public class ClientThread implements Runnable {

	private static final String INDEX_HTML = "/index.html";
	private static final String INDEX_HTM = "/index.htm";
	private static final String ERR_404 = "/404.html";
	private String error404Path;
	private Socket clientSocket;
	private File servingDirectory;
	//    private final int REQUEST_BUFFER_LEN = 4096;
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

		try {
			outputStream = new BufferedOutputStream(clientSocket.getOutputStream());
			inputStream = clientSocket.getInputStream();
		}
		catch (IOException e) {
			// TODO - Add better error handling
			System.err.println("Error when creating input or output stream: " + e.getMessage());
			System.exit(1);
		}

		System.out.println("Using directory: " + servingDirectory.getAbsolutePath());

		RequestParser requestHeader = null;
		try {
			requestHeader = new RequestParser(getRequest());
		}
		catch (IOException e) {
			System.err.println("Request parse failed, bad request received: " + e.getMessage());
			System.exit(1);
		}
		try {
			if (requestHeader.getMethod().equals("GET")) {
				processGet(requestHeader);
			}
			else if (requestHeader.getMethod().equals("PUT")) {
				processPut(requestHeader);
			}
			else if (requestHeader.getMethod().equals("POST")) {
				processPost(requestHeader);
			}
			else {
				// TODO - Send 400 Bad Request
				System.out.println("Not supported");
			}
		}
		catch (IOException e) {
			System.err.println("Error when processing request: " + e.getMessage());
			e.printStackTrace();
			// TODO - Send 500 internal server error
		}
		try {
			clientSocket.close();
		}
		catch (IOException e) {
			System.err.println("Closing socket failed: " + e.getMessage());
		}
		System.out.println("Thread terminating");
	}

	// TODO - Support if the content sent came in multiple chunks of TCP data, we do NOT have to support Transfer-Encoding: Chunked!! We can refuse this kind of request.
	private ArrayList<MultipartObject> getMultipartContent(int contentLength, String _boundary) throws IOException {

		BufferedInputStream reader = new BufferedInputStream(inputStream);

		String boundarySTART = _boundary; // "--XYZ"
		String boundary = _boundary + "\r\n";
		String payloadStart = "\r\n\r\n";
		String boundaryAnotherPart = "\r\n";
		String boundaryEndPart = "--";
		int contentBufferLength = boundary.length() + 4;

		ArrayList<MultipartObject> toReturn = new ArrayList<MultipartObject>();
		ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();
		ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
		ByteBuffer tempHeaderBuffer = ByteBuffer.allocate(1024);
		ByteBuffer tempBuffer = ByteBuffer.allocate(contentBufferLength);

		int readByte = 0;

		boolean isPart = false;
		boolean isPartPayload = false;
		boolean boundaryCheckingMode = false;
		boolean canOnlyBeEnd = false;
		int matchCounter = 0;
		int partPayloadStartMatchCounter = 0;
		int partPayloadEndMatchCounter = 0;

		int boundaryAnotherPartCounter = 0;
		int boundaryEndPartCounter = 0;
		while ((readByte = reader.read()) != -1) {
			// detect start boundary
			if (!isPart) {
				if (readByte == (int) boundary.charAt(matchCounter)) {
					matchCounter += 1;
					if (matchCounter == boundary.length()) {
						isPart = true;
//						System.out.println("Starting boundary detected!");
						continue;
					}
				}
				else {
					matchCounter = 0;
					continue;
				}
			}

			// we're at least past the header start boundary
			if (isPart) {
				if (!isPartPayload) {
					tempHeaderBuffer.put((byte) readByte);

					// check if we're at start of payload "\r\n\r\n"
					if (readByte == (int) payloadStart.charAt(partPayloadStartMatchCounter)) {
						partPayloadStartMatchCounter += 1;
						if (partPayloadStartMatchCounter == payloadStart.length()) {
							isPartPayload = true;
//							System.out.println("Start of part payload detected!");

							int endCondition = tempHeaderBuffer.position() - 4;
							tempHeaderBuffer.flip();
							for (int i = 0; i < endCondition; i++) {
								byte toCopy = tempHeaderBuffer.get();
								headerBuffer.write(toCopy);
							}

							byte[] headerBytes = headerBuffer.toByteArray();
//							System.out.printf("header: {%s} %n", new String(headerBytes, 0, headerBytes.length));

							continue;
						}
						else {
							continue;
						}
					}
					else {
						partPayloadStartMatchCounter = 0;
						continue;
					}
				}

				if (isPartPayload) {

					// here -> --XYZ|
					if (partPayloadEndMatchCounter < boundarySTART.length()) {
						if (readByte == (int) boundarySTART.charAt(partPayloadEndMatchCounter)) {
//                        boundaryCheckingMode = true;
							partPayloadEndMatchCounter += 1;

							// --XYZ detected: enter boundary mode
							if (partPayloadEndMatchCounter == boundarySTART.length()) {
								boundaryCheckingMode = true;

//								System.out.println("--XYZ detected ... entering boundary checking mode");
								continue;
							}
							else {
								// save byte to a temporary buffer in case it turns out to be a false alarm
								tempBuffer.put((byte) readByte);
								continue;
							}
						}
					}
					// --XYZ| <-- here
					else {
						// three options
						// 1. --XYZ-- (end of multipart/form-data)
						// 2. --XYZ (another part)
						// 3. --XYZ{anything} false alarm
						if (boundaryCheckingMode) {
							if (readByte == (int) boundaryEndPart.charAt(boundaryEndPartCounter)) {
								canOnlyBeEnd = true;
								boundaryEndPartCounter += 1;
								if (boundaryEndPartCounter == boundaryEndPart.length()) {
//									System.out.println("END of multipart found");
									boundaryCheckingMode = false;
									tempBuffer = ByteBuffer.allocate(contentBufferLength);
									partPayloadEndMatchCounter = 0;
									boundaryEndPartCounter = 0;
									partPayloadStartMatchCounter = 0;

									MultipartObject multipartObject = new MultipartObject(headerBuffer.toByteArray(), contentBuffer.toByteArray());
									toReturn.add(multipartObject);

									headerBuffer.reset();
									tempHeaderBuffer = ByteBuffer.allocate(1024);

									isPartPayload = false;
									matchCounter = 0;
									break;
								}
								continue;
							}
							else if (!canOnlyBeEnd) {
								if (readByte == (int) boundaryAnotherPart.charAt(boundaryAnotherPartCounter)) {
									boundaryAnotherPartCounter += 1;
									if (boundaryAnotherPartCounter == boundaryAnotherPart.length()) {
//										System.out.println("another multipart section found");
										boundaryCheckingMode = false;
										tempBuffer = ByteBuffer.allocate(contentBufferLength);
										partPayloadEndMatchCounter = 0;
										boundaryAnotherPartCounter = 0;
										partPayloadStartMatchCounter = 0;

										isPartPayload = false;
										matchCounter = 0;

										MultipartObject multipartObject = new MultipartObject(headerBuffer.toByteArray(), contentBuffer.toByteArray());
										toReturn.add(multipartObject);

										headerBuffer.reset();
										tempHeaderBuffer = ByteBuffer.allocate(1024);

										contentBuffer.reset();
										continue;
									}
									continue;
								}
							}

						}
					}

					if (partPayloadEndMatchCounter > 0) {
						// check if we have to write what a false alarm when detecting the end
						tempBuffer.flip(); // make the end of buffer the position of the last element
						while (tempBuffer.hasRemaining()) {
							contentBuffer.write(tempBuffer.get());
						}
						tempBuffer = ByteBuffer.allocate(contentBufferLength);
						partPayloadEndMatchCounter = 0;
					}

					// current byte is part of a payload
					contentBuffer.write((byte) readByte);

				}
			}
		}

		return toReturn;

	}

	private byte[] getBinaryContent(InputStream in, int contentLength) throws IOException {
		byte[] content = new byte[contentLength];

		// credit: the use of ByteArrayOutputStream: https://www.baeldung.com/convert-input-stream-to-array-of-bytes
		ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();

		int totalRead = 0;
		int bytesRead = 0;
		while ((bytesRead = in.read(content, 0, contentLength)) != -1) {
			contentBuffer.write(content, 0, bytesRead);
			totalRead += bytesRead;
			System.out.printf("read  %10d/%d %n", totalRead, contentLength);
			if (totalRead >= contentLength) {
				System.out.println("got all the data");
				break;
			}
		}
		contentBuffer.close();
		// TODO -- Throw exception if bytes read was less than expected content length within a suitable timeout.
		return contentBuffer.toByteArray();
	}

	// Returns a request header
	private byte[] getRequest() throws IOException {
		ArrayList<Byte> bytes = new ArrayList<>();
		byte read;
		boolean run = true;

		boolean first = true;
		boolean second = false;

		while (run) {
			// Read bytes, valid header is ALWAYS in ASCII.
			if ((read = (byte) inputStream.read()) != -1) {
				System.out.print((char) read);
				// Add byte to list.
				bytes.add(read);
				// On CR or LF
				if (read == '\r' || read == '\n') {
					if (first) {
						// On CR
						first = false;
					}
					// SonarLint is wrong, second does turn true on CRLFx2!!
					// On CRLFx2, terminate while loop.
					else if (second) {
						run = false;
					}
					else {
						// On LF
						second = true;
						first = true;
					}
				}
				// On any character other than CR or LF
				else {
					second = false;
					first = true;
				}
			}
			// If -1 is read, EOF has been reached which means the socket received a FIN/ACK
			// ---> NOT REALLY TRUE: -1 can be received when sender closes their TCP output (tcp is bidirectional). They can still listen on their input and the socket is alive.
			// ---> server closed the connection prematurely when I tried a GET /index.html with Postman (Insomnia alternative)
			else {
				// throw new IOException("Host closed connection");
				// Terminates while loop if -1 received.
				run = false;
			}
		}
		return byteConversion(bytes);
	}

	// Takes a Byte list and returns a primitive byte[] array with elements unpacked.
	private byte[] byteConversion(ArrayList<Byte> bytesIn) {
		// Why 0? Compiler wants it that way, doesn't actually try to stuff everything into an empty array.
		Byte[] objectBytes = bytesIn.toArray(new Byte[0]);
		byte[] primitiveReturnBytes = new byte[objectBytes.length];
		int i = 0;
		for (Byte b : objectBytes) {
			primitiveReturnBytes[i++] = b;
		}
		return primitiveReturnBytes;
	}

	// Processes a received GET Request, handles case where page is not found. Any IO exceptions are passed up to the run() method.
	private void processGet(RequestParser requestHeader) throws IOException {
		String requestedPath = servingDirectory.getAbsolutePath() + requestHeader.getPathRequest();
		String finalPath = "";
		boolean error404 = false;
		StatusCode finalStatus = StatusCode.SUCCESS_200_OK;

		// TODO - maybe rewrite this to avoid creating a file object, maybe a path object is enough?
		File requestedFile = new File(servingDirectory, requestHeader.getPathRequest());

		if(requestHeader.getPathRequest().equals("/redirect")) {
			sendRedirect("/redirectlanding");
		}

		// prevent "../../" hacks
		// https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/File.html#getCanonicalPath()
		//  "removes redundant names such as "." and ".." from the pathname,
		//   resolving symbolic links (on UNIX platforms), and converting drive letters to a standard case (on Microsoft Windows platforms)."

		// TODO - Check if this actually still works, changed servingDirectory.getPath() to servingDirectory.getCanonicalPath().
		if (!requestedFile.getCanonicalPath().startsWith(servingDirectory.getCanonicalPath())) {
			// TODO - Send 403 Forbidden!
			System.err.println("400 bad request, terminating");
			System.exit(1);
		}

		// If file is readable and is a directory, start looking for HTML or HTM in that folder.
		if (Files.isDirectory(Paths.get(requestedPath))) {
			finalPath = requestedPath + INDEX_HTML;

			// If index.html doesn't exist, try index.htm
			if (!Files.isReadable(Paths.get(finalPath))) {
				finalPath = requestedPath + INDEX_HTM;

				// If index html and index htm doesn't exist
				if (!Files.isReadable(Paths.get(finalPath))) {
					System.err.println("Resource not found");
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
			System.err.println("Resource not found");
			error404 = true;
		}

		// If previous if-block indicates that resource does not exist, set response to path 404.html and 404 header.
		if (error404) {
			System.out.println("error 404");
//			sendResponse(StatusCode.CLIENT_ERROR_404_NOT_FOUND);
			sendContentResponse(error404Path, StatusCode.CLIENT_ERROR_404_NOT_FOUND);
		}
		else {
			sendContentResponse(finalPath, finalStatus);
		}
	}

	private void processPost(RequestParser requestHeader) throws IOException {
		// TODO: implement x-www-form-urlencoded, multi-part form, binary data.
		// TODO: Detect content type that client is trying to send, this just shoves data into an image file.
		// TODO limit this buffer
		System.out.println("GOT POST REQUEST!");
		System.out.printf("content-type={%s} boundary={%s} %n", requestHeader.getContentType(), requestHeader.getBoundary());

		if (requestHeader.getContentType().equals("multipart/form-data")) {
			// HERE WE HAVE ACCESS TO ALL THE INDIVIDUAL MULTIPART OBJECTS! (including, name, filename, payload etc.)
			ArrayList<MultipartObject> payloadData = getMultipartContent(requestHeader.getContentLength(), requestHeader.getBoundary());

			// print the received filenames
			if (payloadData.size() >= 1) {
				for (MultipartObject multipartObject : payloadData) {
					if(multipartObject.getDispositionContentType().equals("image/png")){
						System.out.printf("saving {%s} %n", multipartObject.getDispositionFilename());
						try (OutputStream out = new FileOutputStream(servingDirectory.getAbsolutePath() + "/uploaded/" + multipartObject.getDispositionFilename())) {
							out.write(multipartObject.getData());
						}
						catch (Exception e) {
							System.out.println("Something went wrong: " + e.getMessage());
							e.printStackTrace();
						}
					}
				}
			}
			else {
				System.err.println("\tNO MULTIPART DATA FOUND");
			}
//					Path writeDestination = Paths.get(servingDirectory + "/FINALE.png");
//
//					for(byte[] toWrite : payloadData) {
//						System.out.println("writing a file...");
//						Random random = new Random();
//						try (OutputStream out = new FileOutputStream(servingDirectory.getAbsolutePath() + "/uploaded/FINALE"+ random.nextInt() +".png")) {
//							out.write(toWrite);
//						}
//						catch (Exception e) {
//							System.out.println("Something went wrong: " + e.getMessage());
//							e.printStackTrace();
//						}
//					}

			// TODO -- Send response
			// 201 created if new
			// 200 OK or 204 if replacing
		}
		else if (requestHeader.getContentType().equals("image/png")) {
			byte[] payloadData = getBinaryContent(inputStream, requestHeader.getContentLength());
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

	// TODO -- Make a put implementation here!
	private void processPut(RequestParser requestHeader) throws IOException {
		boolean internalError = false;
		Path destination = Paths.get(servingDirectory + requestHeader.getPathRequest());
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
		byte[] payloadData = getBinaryContent(inputStream, requestHeader.getContentLength());

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
			sendHeaderResponse(requestHeader.getPathRequest(), StatusCode.SUCCESS_204_NO_CONTENT);
		}
		else if (internalError) {
			System.err.println("Internal Server Error");
			// TODO - Send 500 internal server error
		}
		else {
			// send 201 created if new
			sendHeaderResponse(requestHeader.getPathRequest(), StatusCode.SUCCESS_201_CREATED);
		}
		// 200 OK if replacing
	}

	private void sendResponse(StatusCode statusCode) throws IOException {
		ResponseBuilder responseBuilder = new ResponseBuilder();
		switch (statusCode) {
			case CLIENT_ERROR_404_NOT_FOUND:
//				error404Path
				String body = responseBuilder.HTMLMessage("404 not found");
				String header = responseBuilder.generateGenericHeader("text/html", StatusCode.CLIENT_ERROR_404_NOT_FOUND, body.length());
				outputStream.write(header.getBytes());
				outputStream.write(body.getBytes());
				outputStream.flush();
				break;
		}
	}

	private void sendRedirect(String location) throws IOException{
		ResponseBuilder responseBuilder = new ResponseBuilder();
		String toWrite = responseBuilder.relocateResponse(location);
		outputStream.write(toWrite.getBytes());
		outputStream.flush();
	}

	/*
	If you debug and look at the requested paths, you will see that the 'path' variable mixes (/) and (\), this still works fine with java.io.File.
	Even with a double // or double \\, it io.File filter this out and still works.
    */
	private void sendContentResponse(String path, StatusCode finalStatus) throws IOException {
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