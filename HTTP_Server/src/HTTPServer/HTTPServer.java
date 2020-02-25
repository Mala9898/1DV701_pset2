package HTTPServer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 */
public class HTTPServer {

	private static ThreadPoolExecutor executorService;
	private static ServerSocket serverSocket;
	private static File servingDirectory;
	private static int localPort;
	private static final int UNIT16_MAX = 65535;
	private static int expectedArguments = 2;

	public static void main(String[] args) {
		// Terminates program if not 2
		checkArgLength(args);
		// Parses and sanity checks port number, terminates program if invalid
		localPort = checkPort(args);
		// TODO -- Check if this works ok on a linux server
		// Sets working directory, terminates program if non-existent directory or not a child of the present working directory
		setDir(args[1]);
		// Starts main server loop
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
	 * Serve files from a directory, does basic validation on the directory structure
	 *
	 * @param directory directory to serve files from
	 */
	private static void setDir(String directory) {
		try {
			servingDirectory = new File(directory);
			if (!servingDirectory.isDirectory()) {
				printAndQuit("serving directory has to be a directory");
			}
			if (!servingDirectory.getCanonicalPath().startsWith(System.getProperty("user.dir"))) {
				printAndQuit("serving directory has to be a child of the present working directory, pwd is: " + System.getProperty("user.dir") + "\n"
						+ "Requested directory was: " + servingDirectory.getCanonicalPath());
			}
		}
		catch (Exception e) {
			System.err.println("IO exception occurred while reading directory: " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Checks if argument length is expected, terminates program if not.
	 *
	 * @param args Program arguments
	 */
	private static void checkArgLength(String[] args) {
		if (args.length != expectedArguments) {
			printAndQuit("Requires two arguments");
		}
	}

	/**
	 * @param arguments The input arguments for the jar file
	 * @return the parsed port number if valid, throws if invalid
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
			if (inInclusiveRange(Integer.parseInt(s), 1, UNIT16_MAX)) {
				return true;
			}
		}
		catch (NumberFormatException e) {
			return false;
		}
		return false;
	}

	/**
	 * helper math function to test if value is in [n1, n2]
	 *
	 * @param value Integer to test
	 * @param n1    Lower inclusive limit
	 * @param n2    Upper inclusive limit
	 * @return true if within range, false if outside of range
	 */
	private static boolean inInclusiveRange(int value, int n1, int n2) {
		return (value >= n1) && (value <= n2);
	}

	/**
	 * Prints a message and quits
	 *
	 * @param message Prints this message, quits afterward.
	 */
	private static void printAndQuit(String message) {
		System.err.println(message);
		System.err.println();
		System.err.println("Usage: [port-number] [serving directory (in relation to present directory)]");
		System.exit(-1);
	}

}
