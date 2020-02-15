package ExampleServer;

import HTTPServer.HTTPServer;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 */
public class ExampleServer {

    public static void main(String[] args) {

        HTTPServer server = new HTTPServer();
        server.serveStatic("public");
        server.run();

    }
}
