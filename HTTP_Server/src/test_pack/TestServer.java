package test_pack;

import java.io.IOException;
import java.net.ServerSocket;

public class TestServer {
	public static void main(String[] args) {
		ServerSocket server = null;
		try {
			server = new ServerSocket(80);
		}
		catch (IOException e) {
			System.err.println("Socket creation failed: " + e.getMessage());
			System.exit(1);
		}
		System.out.println("Started server, listening on port: " + server.getLocalPort());
		try {
			while (true) {
				Thread t = new Thread(new TestServerThread(server.accept()));
				t.start();
			}
		}
		catch (IOException e) {
			System.err.println("Thread creation failed: " + e.getMessage());
		}


	}
}
