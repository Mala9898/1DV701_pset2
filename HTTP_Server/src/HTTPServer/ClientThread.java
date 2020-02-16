package HTTPServer;

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

// TODO: implement static serving

public class ClientThread implements Runnable {

	private Socket clientSocket;
	private File servingDirectory;
	//    private final int REQUEST_BUFFER_LEN = 4096;
	private final int REQUEST_BUFFER_LEN = 90000;

	public ClientThread(Socket clientSocket, File directory) {
		this.clientSocket = clientSocket;
		this.servingDirectory = directory;
	}

	@Override
	public void run() {
		try {
			// create two output streams, one "raw" for sending binary data, and a Writer for sending ASCII (header) text
			OutputStream rawOutputStream = new BufferedOutputStream(clientSocket.getOutputStream());
			Writer outputStream = new OutputStreamWriter(rawOutputStream);
			InputStream inputStream = clientSocket.getInputStream();
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

			RequestParser request = null;
			try {
				request = new RequestParser(getRequest(inputStream));
			}
			catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}

			if (request.getMethod().equals("POST")) {
				// TODO: implement x-www-form-urlencoded, multi-part form, binary data.
				// TODO: Detect content type that client is trying to send, this just shoves data into an image file.

				System.out.println("GOT POST REQUEST!");
				byte[] payloadData = getContent(inputStream, request.getContentLength());
				Path writeDestination = Paths.get(servingDirectory + "/FINALE.png");

				// Try with resources is automatically closing.
				try (OutputStream out = new FileOutputStream(servingDirectory.getAbsolutePath() + "/FINALE.png")) {
					out.write(payloadData);
				}
				catch (Exception e) {
					System.out.println("Something went wrong: " + e.getMessage());
					e.printStackTrace();
				}

			}
			if (request.getMethod().equals("GET")) {
				File file = new File(servingDirectory, request.getPathRequest());

				// prevent "../../" hacks
				// https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/File.html#getCanonicalPath()
				//  "removes redundant names such as "." and ".." from the pathname,
				//   resolving symbolic links (on UNIX platforms), and converting drive letters to a standard case (on Microsoft Windows platforms)."
				if (!file.getCanonicalPath().startsWith(servingDirectory.getPath())) {
					// todo send 400 bad request
				}

				if (Files.isDirectory(Paths.get(servingDirectory + request.getPathRequest()))) {
					if (Files.isReadable(Paths.get(servingDirectory + request.getPathRequest() + "/index.html"))) {
//                    System.out.println("found index.html");
						file = new File(Paths.get(servingDirectory + request.getPathRequest() + "/index.html").toString());
					}
					else if (Files.isReadable(Paths.get(servingDirectory + request.getPathRequest() + "/index.htm"))) {
//                    System.out.println("found index.htm");
						file = new File(Paths.get(servingDirectory + request.getPathRequest() + "/index.htm").toString());
					}
					else {
						System.out.println("FOUND NOTHING! index.html");
						// todo: send "404 not found".... neither index.html nor index.htm was found. simply an empty directory.
					}
                }
                System.out.println("final file:" +file.toPath());

//                ResponseBuilder responseBuilder = new ResponseBuilder();

                if(file.canRead()) {
                    System.out.println("can read file");
                    byte[] contentBytes = Files.readAllBytes(Paths.get(file.getPath()));

                    // get MIME type
                    // https://stackoverflow.com/questions/51438/getting-a-files-mime-type-in-java
                    String contentType = URLConnection.guessContentTypeFromName(file.getName());
                    System.out.println("MIME: "+contentType);

                    String header = ResponseBuilder.generateHeader(contentType,StatusCode.SUCCESS_200_OK ,contentBytes.length);
                    System.out.println("\n\nheader: \n"+header);

                    outputStream.write(header);
                    outputStream.flush();

                    rawOutputStream.write(contentBytes);
	                outputStream.flush();

                }
                else {
	                System.err.println("CANNOT READ FILE");
                }
				inputStream.close();
				outputStream.close();
				rawOutputStream.close();
			}

		}
		catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Thread terminating");
	}

	// TODO - Support if the content sent came in multiple chunks of TCP data, we do NOT have to support Transfer-Encoding: Chunked!!
	private byte[] getContent(InputStream in, int contentLength) throws IOException {
		byte[] test = new byte[contentLength];
		in.read(test, 0, contentLength);
		return test;
	}

	private byte[] getRequest(InputStream in) throws IOException {
		ArrayList<Byte> bytes = new ArrayList<>();
		byte read;
		boolean first = true;
		boolean duo = false;

		while (true) {
			if ((read = (byte) in.read()) != -1) {
				System.out.print((char) read);
				bytes.add(read);
				if (read == '\r' || read == '\n') {
					if (first) {
						first = false;
					}
					else if (duo) {
						break;
					}
					else {
						duo = true;
						first = true;
					}
				}
				else {
					duo = false;
					first = true;
				}
			}
			else {
				throw new IOException("Host closed connection");
			}
		}
		return byteConversion(bytes);
	}

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

}