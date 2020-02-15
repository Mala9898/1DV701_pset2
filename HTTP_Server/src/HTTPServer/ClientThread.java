package HTTPServer;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 */

// TODO: implement static serving

public class ClientThread implements Runnable{

    private Socket clientSocket;
    private File directory;

    public ClientThread(Socket clientSocket, File directory) {
        this.clientSocket = clientSocket;
        this.directory = directory;
    }

    @Override
    public void run() {
        try {
            System.out.println("directory: "+directory+" canonical: "+directory.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
