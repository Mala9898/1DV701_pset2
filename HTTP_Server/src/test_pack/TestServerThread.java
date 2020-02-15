/**
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * 2020-02-15
 */

package test_pack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class TestServerThread implements Runnable {
	private Socket socket;
	ArrayList<String[]> fullRequest = new ArrayList<>();

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
		byte[] bytes = new byte[2048];
		StringBuilder lineFromClient = new StringBuilder();
		boolean finishedReading = false;
		String completeMessage = "";
		try {
			MainLoop:
			while (true) {
				String[] requestPart = getRequestLine(input);
				if (requestPart.length == 1 && requestPart[0].equals("")) {
					System.out.println("Finished getting header");
					finishedReading = true;
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