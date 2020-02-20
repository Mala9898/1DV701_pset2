package HTTPServer;

import HTTPServer.Multipart.MultipartObject;

import java.io.*;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 */

// TODO: implement static serving

public class ClientThread implements Runnable {

	private static final String INDEX_HTML = "/index.html";
	private static final String INDEX_HTM = "/index.htm";
	private String error404HtmlPath;
	private Socket clientSocket;
	private File servingDirectory;
	//    private final int REQUEST_BUFFER_LEN = 4096;
	private final int REQUEST_BUFFER_LEN = 90000;

	public ClientThread(Socket clientSocket, File directory) {
		this.clientSocket = clientSocket;
		this.servingDirectory = directory;
		error404HtmlPath = servingDirectory.getAbsolutePath() + "/404.html";
	}

	@Override
	public void run() {
		try {
			// create two output streams, one "raw" for sending binary data, and a Writer for sending ASCII (header) text
			OutputStream outputStream = new BufferedOutputStream(clientSocket.getOutputStream());
			InputStream inputStream = clientSocket.getInputStream();

			System.out.println("Using directory: " + servingDirectory.getAbsolutePath());
/*

        // TODO -- Shove all this into RequestParser

            byte[] requestBuffer = new byte[REQUEST_BUFFER_LEN];
            int totalBytesRead = inputStream.read(requestBuffer, 0, REQUEST_BUFFER_LEN);
            System.out.println("BYTES READ: "+totalBytesRead);
//            RequestParser test = new RequestParser(requestBuffer, totalBytesRead);

            // todo: send "414 URI Too Long" error if totalBytesRead >= 4096:
            String requestString = new String(requestBuffer, 0, totalBytesRead);

//            int indexOfPayloadStart = requestString.indexOf("\r\n\r\n");
//            System.out.println("\t PAYLOAD START: " + indexOfPayloadStart);

            // split by \r\n. additionally, "+" removes empty lines.
            // https://stackoverflow.com/questions/454908/split-java-string-by-new-line
//            String[] requestLines = requestString.split("[\\r\\n]+");
//            String[] requestLines = requestString.replace(" ", "").split("[\\s]");
//            String[] requestLines = requestString.split("[\\s]");
            String[] requestLines = requestString.split("[\\r\\n\\r\\n]");

//            Pattern ptrn = Pattern.compile("([a-zA-Z]+) (\\d+)");
//            Pattern pattern = Pattern.compile("(.*)\\s\\s(.*)");
//            Pattern pattern = Pattern.compile(".*^(\\r\\n\\r\\n)$.*");
//            Pattern pattern = Pattern.compile( "\\r\\n\\r\\n", Pattern.MULTILINE);
            Pattern pattern = Pattern.compile( "^(\\r\\n|\\r|\\n)*$", Pattern.MULTILINE);

            Matcher matcher = pattern.matcher(requestString);
            MatchResult matchResult = matcher.toMatchResult();

            int headerStart = 0;
            int headerEnd = 0;
            int payloadStart = 0;
            int payloadEnd = 0;

            boolean first = true;
            while (matcher.find()) {
                if(first) {
                    headerEnd = matcher.start();
                    payloadStart= matcher.start();
                    payloadEnd = requestString.length();
                    first = false;
                }
                System.out.println(String.format("Match: %s at index [%d, %d]",
                        matcher.group(), matcher.start(), matcher.end()));
            }
            String extractedHeader = requestString.substring(0, headerEnd);
            String extractedPayload = requestString.substring(payloadStart, payloadEnd);

            // GET REQUEST LINE
            // Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
            Pattern patternRequestline = Pattern.compile( "^(GET|POST|HEAD|PUT)\\s+([\\/\\w?=%.]*)\\s+(HTTP\\/.*)");
            Matcher matcher2 = patternRequestline.matcher(extractedHeader);
            String[] firstLineParameters = {"","",""};
            while (matcher2.find()) {
                System.err.println(String.format("\tMatch: %s at index [%d, %d]",
                        matcher2.group(), matcher2.start(), matcher2.end()));
                System.out.printf("group count: %d %n", matcher2.groupCount());
                if(matcher2.groupCount() == 3) {
                    firstLineParameters[0]=matcher2.group(1);
                    firstLineParameters[1]=matcher2.group(2);
                    firstLineParameters[2]=matcher2.group(3);
                }
            }

            // GET CONTENT TYPE
            Pattern patternContentType = Pattern.compile( "^(Content-Type):\\s*([\\w\\/-]+)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            Matcher matcher3 = patternContentType.matcher(extractedHeader);
            String requestContentType = "";
            while (matcher3.find()) {
                if(matcher3.groupCount() == 2) {
                    requestContentType = matcher3.group(2);
                    break;
                }
            }
            System.out.printf("\t contenttype: {%s} %n", requestContentType);

            // GET CONTENT LENGTH
            Pattern patternContentLength = Pattern.compile( "^(Content-Length):\\s*([\\w\\/-]+)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            Matcher matcher4 = patternContentLength.matcher(extractedHeader);
            int requestContentLength = 0;
            while (matcher4.find()) {
                if(matcher4.groupCount() == 2) {
                    try {
                        requestContentLength = Integer.parseInt(matcher4.group(2));
                    } catch (NumberFormatException e) {
                        // TODO send 4XX client error
                    }
                    break;
                }
            }
            System.out.printf("\t content-length: {%s} %n", requestContentLength);

//            String reqLine = matcherRequestline.group();
//            Arrays.stream(requestLines).forEach(line -> System.out.println("line:{"+line+"}"));


            if(firstLineParameters.length < 3){
                // TODO send 400 Bad Request
            }

            String requestMethod = firstLineParameters[0];
            String requestURI = firstLineParameters[1];

//            Arrays.stream(firstLineParameters).forEach(line -> System.out.println("\tfirstLine:{"+line+"}"));
*/

			RequestParser requestHeader = null;
			try {
				requestHeader = new RequestParser(getRequest(inputStream));
			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			if (requestHeader.getMethod().equals("GET")) {
				processGet(requestHeader, outputStream);
			}
			else if (requestHeader.getMethod().equals("PUT")) {
				processPut(requestHeader, outputStream);
			}
			else if (requestHeader.getMethod().equals("POST")) {
				// TODO: implement x-www-form-urlencoded, multi-part form, binary data.
				// TODO: Detect content type that client is trying to send, this just shoves data into an image file.
				// TODO limit this buffer
				System.out.println("GOT POST REQUEST!");
				System.out.printf("content-type={%s} boundary={%s} %n", requestHeader.getContentType(), requestHeader.getBoundary());

				if(requestHeader.getContentType().equals("multipart/form-data")) {
					ArrayList<MultipartObject> payloadData = getMultipartContent(inputStream, requestHeader.getContentLength(),requestHeader.getBoundary());

					if(payloadData.size() >= 1) {
						for(MultipartObject multipartObject : payloadData) {
							System.out.printf("filename: {%s}  %n", multipartObject.getDispositionFilename());
						}
					} else {
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

				}
				else if(requestHeader.getContentType().equals("image/png")) {
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

			else {
				// TODO - Send 400 Bad Request
				System.out.println("Not supported");
			}

		}
		catch (IOException e) {
			e.printStackTrace();
		}
		try {
			clientSocket.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Thread terminating");
	}

	// TODO - Support if the content sent came in multiple chunks of TCP data, we do NOT have to support Transfer-Encoding: Chunked!! We can refuse this kind of request.
	private ArrayList<MultipartObject> getMultipartContent(InputStream inputStream, int contentLength, String _boundary) throws IOException {

		BufferedInputStream reader = new BufferedInputStream(inputStream);

		String boundarySTART = _boundary; // "--XYZ"
		String boundary = _boundary+"\r\n";
		String payloadStart = "\r\n\r\n";
		String boundaryAnotherPart = "\r\n";
		String boundaryEndPart = "--";
		int contentBufferLength = boundary.length()+4;

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
			if(!isPart) {
				if(readByte == (int)boundary.charAt(matchCounter)) {
					matchCounter+= 1;
					if(matchCounter == boundary.length()){
						isPart = true;
//						System.out.println("Starting boundary detected!");
						continue;
					}
				} else {
					matchCounter = 0;
					continue;
				}
			}

			// we're at least past the header start boundary
			if(isPart) {
				if(!isPartPayload) {
					tempHeaderBuffer.put((byte)readByte);

					// check if we're at start of payload "\r\n\r\n"
					if(readByte == (int)payloadStart.charAt(partPayloadStartMatchCounter)) {
						partPayloadStartMatchCounter += 1;
						if(partPayloadStartMatchCounter == payloadStart.length()){
							isPartPayload = true;
//							System.out.println("Start of part payload detected!");

							int endCondition = tempHeaderBuffer.position() - 4;
							tempHeaderBuffer.flip();
							for(int i = 0; i < endCondition; i++) {
								byte toCopy = tempHeaderBuffer.get();
								headerBuffer.write(toCopy);
							}

							byte[] headerBytes = headerBuffer.toByteArray();
//							System.out.printf("header: {%s} %n", new String(headerBytes, 0, headerBytes.length));

							continue;
						} else {
							continue;
						}
					} else {
						partPayloadStartMatchCounter = 0;
						continue;
					}
				}

				if(isPartPayload) {

					// here -> --XYZ|
					if(partPayloadEndMatchCounter < boundarySTART.length()) {
						if(readByte == (int)boundarySTART.charAt(partPayloadEndMatchCounter)) {
//                        boundaryCheckingMode = true;
							partPayloadEndMatchCounter += 1;

							// --XYZ detected: enter boundary mode
							if(partPayloadEndMatchCounter == boundarySTART.length()){
								boundaryCheckingMode = true;

//								System.out.println("--XYZ detected ... entering boundary checking mode");
								continue;
							} else {
								// save byte to a temporary buffer in case it turns out to be a false alarm
								tempBuffer.put((byte)readByte);
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
						if(boundaryCheckingMode) {
							if(readByte == (int)boundaryEndPart.charAt(boundaryEndPartCounter)) {
								canOnlyBeEnd = true;
								boundaryEndPartCounter += 1;
								if(boundaryEndPartCounter == boundaryEndPart.length()){
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
							else if(!canOnlyBeEnd) {
								if(readByte == (int)boundaryAnotherPart.charAt(boundaryAnotherPartCounter)) {
									boundaryAnotherPartCounter += 1;
									if(boundaryAnotherPartCounter == boundaryAnotherPart.length()){
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

					if(partPayloadEndMatchCounter > 0){
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
			contentBuffer.write(content, 0 , bytesRead);
			totalRead += bytesRead;
			System.out.printf("read  %10d/%d %n", totalRead, contentLength);
			if(totalRead >= contentLength){
				System.out.println("got all the data");
				break;
			}
		}
		contentBuffer.close();
		// TODO -- Throw exception if bytes read was less than expected content length within a suitable timeout.
		return contentBuffer.toByteArray();
	}

	// Returns a request header
	private byte[] getRequest(InputStream in) throws IOException {
		ArrayList<Byte> bytes = new ArrayList<>();
		byte read;
		boolean first = true;
		boolean duo = false;

		while (true) {
			// Read bytes, valid header is ALWAYS in ASCII.
			if ((read = (byte) in.read()) != -1) {
				System.out.print((char) read);
				// Add byte to list.
				bytes.add(read);
				// On CR or LF
				if (read == '\r' || read == '\n') {
					if (first) {
						// On CR
						first = false;
					}
					// SonarLint is wrong, duo does turn true on CRLFx2!!
					// On CRLFx2
					else if (duo) {
						break;
					}
					else {
						// On LF
						duo = true;
						first = true;
					}
				}
				// On any character other than CR or LF
				else {
					duo = false;
					first = true;
				}
			}
			// If -1 is read, EOF has been reached which means the socket received a FIN/ACK
			// ---> NOT REALLY TRUE: -1 can be received when sender closes their TCP output (tcp is bidirectional). They can still listen on their input and the socket is alive.
			// ---> server closed the connection prematurely when I tried a GET /index.html with Postman (Insomnia alternative)
			else {
//				throw new IOException("Host closed connection");
				break;
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

	private void processGet(RequestParser requestHeader, OutputStream output) throws IOException {
		String requestedPath = servingDirectory.getAbsolutePath() + requestHeader.getPathRequest();
		String finalPath = "";
		boolean set404error = false;
		StatusCode finalStatus = StatusCode.SUCCESS_200_OK;

		// TODO - maybe rewrite this to avoid creating a file object, maybe a path object is enough?
		File requestedFile = new File(servingDirectory, requestHeader.getPathRequest());

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
					set404error = true;
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
			set404error = true;
		}

		// If previous if-block indicates that resource does not exist, set response to path 404.html and 404 header.
		if (set404error) {
			finalStatus = StatusCode.CLIENT_ERROR_404_NOT_FOUND;
//			finalPath = error404HtmlPath;
			sendError(StatusCode.CLIENT_ERROR_404_NOT_FOUND, output);
		} else
			generateAndSendOutput(finalPath, finalStatus, output);
	}

	// TODO -- Make a put implementation here!
	private void processPut(RequestParser requestHeader, OutputStream output) {

	}

	/*
	If you debug and look at the requested paths, you will see that the finalPath variable mixes (/) and (\), this still works fine with java.io.File.
	Even with a double // or double \\, it io.File filter this out and still works.
    */
	private void generateAndSendOutput(String finalPath, StatusCode finalStatus, OutputStream output) throws IOException {
		File f = new File(finalPath);
		System.out.println("Outputting to stream: " + f.getAbsolutePath());
		byte[] headerBytes = (ResponseBuilder.generateHeader(URLConnection.guessContentTypeFromName(f.getName()), finalStatus, f.length())).getBytes();
		if (f.canRead()) {
			output.write(headerBytes);
			output.write(Files.readAllBytes(Paths.get(f.getAbsolutePath())));
			// flush() tells stream to send bytes immediately
			output.flush();
		}
	}
	private void sendError(StatusCode finalStatus, OutputStream output) throws IOException {
		byte[] headerBytes = (ResponseBuilder.generateHeader("text/html", finalStatus, ResponseBuilder.PAGE_404.length())).getBytes();

		output.write(headerBytes);
		output.write(ResponseBuilder.PAGE_404.getBytes());
		output.flush(); // flush() tells stream to send bytes immediately

	}
}