package HTTPServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
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
	private static int localPort;
	private static final int UINT16_MAX = 65535;

	public static void main(String[] args) {
		// Terminates program if not 2
		checkArgLength(args);
		// Parses port number
		localPort = checkPort(args);
		// TODO -- Check if this is actually ok when running via command line and compiling in linux
		// Sets working directory
		setDir("public");
		startServer();
	}

	private static void startServer() {
		try {
			// create a socket that awaits incoming clients
			serverSocket = new ServerSocket(localPort);
			// Creates a pool for executors for improved performance in comparison to just using Thread
			executorService = (ThreadPoolExecutor) Executors.newCachedThreadPool();
			// Arbitrary pool size.
			executorService.setMaximumPoolSize(2 * Runtime.getRuntime().availableProcessors() + 10);
		}
		catch (IOException e) {
			System.err.println("could not initialize socket: " + e.getMessage());
			System.exit(1);
		}

		while (true) {
			try {
				// handle client in a thread from the thread pool, serverSocket.accept() is blocking until a client connects.
				executorService.submit(new ClientThread(serverSocket.accept(), servingDirectory));
			}
			catch (IOException e) {
				System.out.println("General IO error: " + e.getMessage());
			}
			catch (RejectedExecutionException e) {
				System.out.println("Thread creation failure, could not accept client: " + e.getMessage());
			}
		}
	}

	/**
	 * Serve files from a directory
	 *
	 * @param directory directory to serve files from
	 */
	private static void setDir(String directory) {
		try {
			servingDirectory = new File(directory);
			if (!servingDirectory.isDirectory()) {
				printAndQuit("serving directory has to be a directory");
			}
		}
		catch (Exception e) {
			System.err.println("IO exception occurred while reading directory: " + e.getMessage());
			System.exit(1);
		}
	}

	private static void checkArgLength(String[] args) {
		if (args.length != 2) {
			printAndQuit("Usage: [port-number] [root directory]");
		}
	}

	/**
	 * @param arguments
	 * @return
	 */
	private static int checkPort(String[] arguments) {
		if (!validPort(arguments[0])) {
			printAndQuit("Port is out of range or invalid");
		}
		return Integer.parseInt(arguments[0]);
	}

	/**
	 * Sanity checks port value
	 *
	 * @param s Port value as a string
	 * @return true if valid, false if invalid port found
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
	 * @param value Integer to test
	 * @param n1 Lower inclusive limit
	 * @param n2 Upper inclusive limit
	 * @return true if within range, false if outside of range
	 */
	private static boolean inInclusiveRange(int value, int n1, int n2) {
		return (value >= n1) && (value <= n2);
	}

	/**
	 * Prints a message and quits
	 * @param message Prints this message, quits afterward.
	 */
	private static void printAndQuit(String message) {
		System.err.println(message);
		System.exit(-1);
	}

}
