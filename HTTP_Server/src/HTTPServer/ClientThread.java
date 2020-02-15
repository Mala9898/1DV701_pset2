package HTTPServer;

import java.io.*;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 */

// TODO: implement static serving

public class ClientThread implements Runnable{

    private Socket clientSocket;
    private File servingDirectory;
    private final int REQUEST_BUFFER_LEN = 4096;

    public ClientThread(Socket clientSocket, File directory) {
        this.clientSocket = clientSocket;
        this.servingDirectory = directory;
    }

    @Override
    public void run() {
        try {
            // create two output streams, one "raw" for sending binary data, and a Writer for sending ASCII (header) text
            OutputStream rawOutputStream = new BufferedOutputStream(clientSocket.getOutputStream());
            Writer outputStream = new OutputStreamWriter(rawOutputStream);

            Reader inputStream = new InputStreamReader(clientSocket.getInputStream(), "US-ASCII");

            char[] requestBuffer = new char[REQUEST_BUFFER_LEN];
            int totalBytesRead = inputStream.read(requestBuffer, 0, REQUEST_BUFFER_LEN);

            // todo: send "414 URI Too Long" error if totalBytesRead >= 4096:
            String requestString = new String(requestBuffer, 0, totalBytesRead);

            // split by \r\n. additionally, "+" removes empty lines.
            // https://stackoverflow.com/questions/454908/split-java-string-by-new-line
            String[] requestLines = requestString.split("[\\r\\n]+");

//            Arrays.stream(requestLines).forEach(line -> System.out.println("line:{"+line+"}"));

            // Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
            // SOURCE: https://tools.ietf.org/html/rfc2616#page-35
            String[] firstLineParameters = requestLines[0].split(" ");

            if(firstLineParameters.length < 3){
                // TODO send 400 Bad Request
            }

            String requestMethod = firstLineParameters[0];
            String requestURI = firstLineParameters[1];

            File file = new File(servingDirectory, requestURI);

            // prevent "../../" hacks
            // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/File.html#getCanonicalPath()
            //  "removes redundant names such as "." and ".." from the pathname,
            //   resolving symbolic links (on UNIX platforms), and converting drive letters to a standard case (on Microsoft Windows platforms)."
            if(!file.getCanonicalPath().startsWith(servingDirectory.getPath())){
                // todo send 400 bad request
            }


            if(Files.isDirectory(Paths.get(servingDirectory+requestURI))) {
                if(Files.isReadable(Paths.get(servingDirectory+requestURI + "/index.html"))){
//                    System.out.println("found index.html");
                    file = new File(Paths.get(servingDirectory+requestURI+"/index.html").toString());
                }
                else if(Files.isReadable(Paths.get(servingDirectory+requestURI + "/index.htm"))){
//                    System.out.println("found index.htm");
                    file = new File(Paths.get(servingDirectory+requestURI+"/index.htm").toString());
                } else {
                    System.out.println("FOUND NOTHING! index.html");
                    // todo: send "404 not found".... neither index.html nor index.htm was found. simply an empty directory.
                }
            }

            System.out.println("final file:" +file.toPath());

            Arrays.stream(firstLineParameters).forEach(line -> System.out.println("\tfirstLine:{"+line+"}"));



            if(file.canRead()) {
                System.out.println("can read file");
                byte[] contentBytes = Files.readAllBytes(Paths.get(file.getPath()));

                // get MIME type
                // https://stackoverflow.com/questions/51438/getting-a-files-mime-type-in-java
                String contentType = URLConnection.guessContentTypeFromName(file.getName());
                System.out.println("MIME: "+contentType);

                String header = ResponseBuilder.generateHeader(contentType, contentBytes.length);
                System.out.println("\n\nheader: \n"+header);

                outputStream.write(header);
                outputStream.flush();

                rawOutputStream.write(contentBytes);
                outputStream.flush();

            } else {
                System.err.println("CANNOT READ FILE");
            }
//            responseBuilder.

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
