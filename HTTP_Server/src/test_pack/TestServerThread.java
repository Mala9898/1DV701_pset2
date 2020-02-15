/**
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * 2020-02-15
 */

package test_pack;

import HTTPServer.ResponseBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class TestServerThread implements Runnable {
	private Socket socket;
	private String index = "index.html";
	ArrayList<String[]> fullRequest = new ArrayList<>();

	// Constructor
	public TestServerThread(Socket s) {
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
		try {
			while (true) {
				String[] requestPart = getRequestLine(input);
				if (requestPart.length == 1 && requestPart[0].equals("")) {
					System.out.println("Finished getting header");
					// When header is fully 'gotten', get out of this loop.
					break;
				}
				else {
					fullRequest.add(requestPart);
				}
			}
		}
		catch (IOException e) {
			System.err.println("Connection failed, reason: " + e.getMessage());
			System.err.println("Closing connection: " + socket.getInetAddress());
		}

		String[] mainRequest = fullRequest.get(0);
		if (mainRequest[0].equals("GET")) {
			File f = new File("HTTP_Server\\public\\index.html");
			System.out.println(f.getAbsolutePath());
			try {
				byte[] aLottaBytes = (ResponseBuilder.generateHeader("html", (int) f.length())).getBytes();
				output.write(aLottaBytes);
				output.write(Files.readAllBytes(Paths.get(f.getAbsolutePath())));
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

	private String[] getRequestLine(InputStream in) throws IOException {
		int read;
		StringBuilder lineFromClient = new StringBuilder();
		String completeMessage;
		boolean first = true;
		while (true) {
			if ((read = in.read()) != -1) {
				System.out.print((char) read);
				if (read == '\r' || read == '\n') {
					if (first) {
						first = false;
						continue;
					}
					completeMessage = lineFromClient.toString();
					// Split on whitespace
					return completeMessage.split("\\s+");
				}
				else {
					lineFromClient.append((char) read);
				}
			}
			else {
				throw new IOException("Host closed connection");
			}
		}
	}
}