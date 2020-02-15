/**
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * 2020-02-15
 */

package test_pack;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

public class TestServer {
	private static File rootDir = null;

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("usage: specify root directory");
			System.err.println("Continuing using default directory...");
			rootDir = new File("");
		}
		else {
			rootDir = getValidatedDir(args[0]);
		}
		System.out.println("Using file path: " + rootDir.getAbsolutePath());


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

	private static File getValidatedDir(String arg) {
		File dir = new File(arg);
		if (!dir.exists()) {
			System.err.println("Invalid path");
			System.exit(1);
		}
		else if (!dir.isDirectory()) {
			System.err.println("Path is not a directory!");
			System.exit(1);
		}
		return dir;
	}
}