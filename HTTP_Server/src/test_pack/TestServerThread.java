package test_pack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TestServerThread implements Runnable {
	private Socket socket;

	public TestServerThread(Socket s) {
		socket = s;
	}

	@Override
	public void run() {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = socket.getInputStream();
			out = socket.getOutputStream();
		}
		catch (IOException e) {
			System.err.println("Write or read pipes broke on creation: " + e.getMessage());
			System.exit(1);
		}
		try {
			byte read;
			while (true) {
				if ((read = (byte) in.read()) != -1) {
					System.out.print((char) read);
				}
				else {
					System.out.println();
					System.err.println("Connection from " + socket.getInetAddress().getHostAddress() + " was closed by host");
					break;
				}

			}
		}
		catch (IOException e) {
			System.err.println("Connection failed, reason: " + e.getMessage());
			System.err.println("Closing connection: " + socket.getInetAddress());
			System.exit(1);
		}

	}
}
