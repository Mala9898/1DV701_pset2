/**
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * 2020-02-15
 */

package test_pack;

import HTTPServer.RequestParser;
import HTTPServer.ResponseBuilder;
import HTTPServer.StatusCode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.ReferenceQueue;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class TestServerThread implements Runnable {
	private Socket socket;
	private final String INDEX_HTML = "/index.html";
	private final String INDEX_HTM = "/index.htm";
	private File rootDirectory;
	ArrayList<String[]> fullRequest = new ArrayList<>();

	// Constructor
	public TestServerThread(Socket s, File f) {
		rootDirectory = f;
		socket = s;
	}

	@Override
	public void run() {
		InputStream input = null;
		OutputStream output = null;
		try {
			input = socket.getInputStream();
			output = socket.getOutputStream();
		}
		catch (IOException e) {
			System.err.println("Write or read pipes broke on creation: " + e.getMessage());
			System.exit(1);
		}
		RequestParser requestParser = null;
		try {
			requestParser = new RequestParser(getRequest(input));
			System.out.println("Finished getting header");
		}
		catch (IOException e) {
			System.err.println("Connection failed, reason: " + e.getMessage());
			System.err.println("Closing connection: " + socket.getInetAddress());
		}

		if (requestParser.getMethod().equals("GET")) {
			String requestedPath = rootDirectory.getAbsolutePath() + requestParser.getPathRequest();
			String finalPath = "";

			if (Files.isDirectory(Paths.get(requestedPath))) {
				finalPath = requestedPath + INDEX_HTML;
				if (!Files.isReadable(Paths.get(finalPath))) {
					finalPath = requestedPath + INDEX_HTM;
					if (!Files.isReadable(Paths.get(finalPath))) {
						System.err.println("ERR 404 TEMP");
					}
				}
			}
			else if (Files.isReadable(Paths.get(requestedPath))) {
				finalPath = requestedPath;
			}
			else {
				// 404 not found
				System.err.println("Resource not found");
			}
			File f = new File(finalPath);

			System.out.println(f.getAbsolutePath());
			try {
				byte[] aLottaBytes = (ResponseBuilder.generateHeader("html", StatusCode.SUCCESS_200_OK, (int) f.length())).getBytes();
				output.write(aLottaBytes);
				if (f.canRead()) {
					output.write(Files.readAllBytes(Paths.get(f.getAbsolutePath())));
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}


		// On termination of thread
		System.out.println("Finished serving content to: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
		System.out.println("Closing connection");
		try {
			socket.close();
		}
		catch (IOException e) {
			System.err.println("Error during socket termination: " + e.getMessage());
		}


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
					else {
						if (duo) {
							break;
						}
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
		// Why 0? Compiler wants it that way, doesn't actually try to stuff everything into an empty array.
		Byte[] objectBytes = bytes.toArray(new Byte[0]);
		byte[] bytesToReturn = new byte[objectBytes.length];
		int i = 0;
		for (Byte b : objectBytes) {
			bytesToReturn[i++] = b;
		}
		return bytesToReturn;
	}
}