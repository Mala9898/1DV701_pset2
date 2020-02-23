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

	private static ThreadPoolExecutor executorService;
	private static ServerSocket serverSocket;
	private static File servingDirectory;
	private static int port = 80;
	private static final int UINT16_MAX = 65535;

	public static void main(String[] args) {
		if(args.length < 1)
			printAndQuit("not enough arguments");

		// this is very platform specific
		if(!validPort(args[0]))
			printAndQuit("port is out of range");
		port = Integer.parseInt(args[0]);

		serveStatic("public");
		run();
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
	public static void serveStatic(String directory) {
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

	public static void run() {

		try {
			// create a socket that awaits incoming clients
			serverSocket = new ServerSocket(port);

			executorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
			executorService.setMaximumPoolSize(2 * Runtime.getRuntime().availableProcessors() + 10);

		}
		catch (IOException e) {
			System.err.println("could not initialize socket: " + e.getMessage());
			try {
				serverSocket.close();
			}
			catch (Exception e2) {
				System.err.println("failed to close socket: " + e2.getMessage());
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

	/**
	 * is port value valid?
	 * @param s
	 * @return
	 */
	private static boolean validPort(String s) {
		try {
			if(inInclusiveRange(Integer.parseInt(s), 1, UINT16_MAX))
				return true;
		} catch (NumberFormatException e){
			return false;
		}
		return false;
	}

	/**
	 * helper math function to test if value is in [n1, n2]
	 * @param value
	 * @param n1 lower inclusive limit
	 * @param n2 upper inclusive limit
	 * @return
	 */
	private static boolean inInclusiveRange(int value, int n1, int n2) {
		return (value >= n1) && (value <= n2);
	}

	/**
	 * Prints a message and quits
	 * @param message
	 */
	private static void printAndQuit(String message) {
		System.out.println(message);
		System.exit(-1);
	}

}
