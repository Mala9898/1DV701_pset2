package HTTPServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 */
public class HTTPServer {

	private ThreadPoolExecutor executorService;
	private ServerSocket serverSocket;
	private File servingDirectory;
	private int port = 4950;


	public HTTPServer() {

	}

	public void setPort(int _port) {
		if (_port >= 1 && _port <= 65535) {
			port = _port;
		}
	}

	/**
	 * Serve files from a directory
	 *
	 * @param directory
	 */
	public void serveStatic(String directory) {
		try {
			servingDirectory = new File(directory);
			if (!servingDirectory.isDirectory()) {
				System.err.println("serving directory has to be a directory.");
				System.exit(-1);
			}
		}
		catch (Exception e) {
			System.err.println("IO exception occurred while reading directory: " + e.getMessage());
			System.exit(-1);
		}
	}

	public void run() {

		try {
			// create a socket that awaits incoming clients
			serverSocket = new ServerSocket(port);

			executorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
			executorService.setMaximumPoolSize(2 * Runtime.getRuntime().availableProcessors() + 10);

		}
		catch (IOException e) {
			System.out.println("could not initialize socket: " + e.getMessage());
			try {
				serverSocket.close();
			}
			catch (IOException e2) {
				System.out.println("failed to close socket: " + e2.getMessage());
			}
			System.exit(1);
		}

		while (true) {
			// wait for a client to connect. accept() blocks until a client connects
			try {
				Socket clientSocket = serverSocket.accept();

				// tell TCP to not be lazy by passing data to application layer immediately
				clientSocket.setTcpNoDelay(true);

				// handle client in a thread from the thread pool
				executorService.submit(new ClientThread(clientSocket, servingDirectory));
			}
			catch (IOException e) {
				System.out.println("General IOException: " + e.getMessage());
			}
			catch (RejectedExecutionException e) {
				System.out.println("Thread creation failure, could not accept client: " + e.getMessage());
			}
		}
	}

}
