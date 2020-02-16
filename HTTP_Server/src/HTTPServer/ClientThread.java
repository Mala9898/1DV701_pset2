package HTTPServer;

import java.io.*;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-15
 */

// TODO: implement static serving

public class ClientThread implements Runnable{

    private Socket clientSocket;
    private File servingDirectory;
//    private final int REQUEST_BUFFER_LEN = 4096;
    private final int REQUEST_BUFFER_LEN = 90000;

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
            System.out.println("BYTES READ: "+totalBytesRead);
//            RequestParser test = new RequestParser(requestBuffer, totalBytesRead);

            // todo: send "414 URI Too Long" error if totalBytesRead >= 4096:
            String requestString = new String(requestBuffer, 0, totalBytesRead);

//            int indexOfPayloadStart = requestString.indexOf("\r\n\r\n");
//            System.out.println("\t PAYLOAD START: " + indexOfPayloadStart);

            // split by \r\n. additionally, "+" removes empty lines.
            // https://stackoverflow.com/questions/454908/split-java-string-by-new-line
//            String[] requestLines = requestString.split("[\\r\\n]+");
//            String[] requestLines = requestString.replace(" ", "").split("[\\s]");
//            String[] requestLines = requestString.split("[\\s]");
            String[] requestLines = requestString.split("[\\r\\n\\r\\n]");

//            Pattern ptrn = Pattern.compile("([a-zA-Z]+) (\\d+)");
//            Pattern pattern = Pattern.compile("(.*)\\s\\s(.*)");
//            Pattern pattern = Pattern.compile(".*^(\\r\\n\\r\\n)$.*");
//            Pattern pattern = Pattern.compile( "\\r\\n\\r\\n", Pattern.MULTILINE);
            Pattern pattern = Pattern.compile( "^(\\r\\n|\\r|\\n)*$", Pattern.MULTILINE);

            Matcher matcher = pattern.matcher(requestString);
            System.out.println("\t groupcount: "+matcher.groupCount());
            MatchResult matchResult = matcher.toMatchResult();

            int headerStart = 0;
            int headerEnd = 0;
            int payloadStart = 0;
            int payloadEnd = 0;

            boolean first = true;
            while (matcher.find()) {
                if(first) {
                    headerEnd = matcher.start();
                    payloadStart= matcher.start()+4;
                    payloadEnd = requestString.length();
                    first = false;
                }
                System.out.println(String.format("Match: %s at index [%d, %d]",
                        matcher.group(), matcher.start(), matcher.end()));
            }
            String extractedHeader = requestString.substring(0, headerEnd);
            String extractedPayload = requestString.substring(payloadStart, payloadEnd);

            System.out.printf(" header {%s} \n payload {%s} %n", extractedHeader, extractedPayload);
//            Matcher matcher = ptrn.matcher("June 24, August 9, Dec 12");

            // GET REQUEST LINE
            Pattern patternRequestline = Pattern.compile( "^(GET|POST|HEAD|PUT)\\s+([\\/\\w?=%]*)\\s+(HTTP\\/.*)");
            Matcher matcher2 = patternRequestline.matcher(extractedHeader);
            while (matcher2.find()) {
                System.err.println(String.format("\tMatch: %s at index [%d, %d]",
                        matcher2.group(), matcher2.start(), matcher2.end()));
                System.out.printf("group count: %d %n", matcher2.groupCount());
                if(matcher2.groupCount() == 3) {
                    System.out.printf("group 1: {%s} %n", matcher2.group(1));
                    System.out.printf("group 2: {%s} %n", matcher2.group(2));
                    System.out.printf("group 3: {%s} %n", matcher2.group(3));
                }
            }

            // GET CONTENT TYPE
            Pattern patternContentType = Pattern.compile( "^(Content-Type):\\s*([\\w\\/-]+)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            Matcher matcher3 = patternContentType.matcher(extractedHeader);
            String requestContentType = "";
            while (matcher3.find()) {
//                System.err.println(String.format("\tCONTENT Match: %s at index [%d, %d]",
//                        matcher3.group(), matcher3.start(), matcher3.end()));
//                System.out.printf("group count: %d %n", matcher3.groupCount());
                if(matcher3.groupCount() == 2) {
                    requestContentType = matcher3.group(2);
                    break;
                }
            }
            System.out.printf("\t contenttype: {%s} %n", requestContentType);
//            String reqLine = matcherRequestline.group();

//            Arrays.stream(requestLines).forEach(line -> System.out.println("line:{"+line+"}"));

            // Request-Line   = Method SP Request-URI SP HTTP-Version CRLF
            // SOURCE: https://tools.ietf.org/html/rfc2616#page-35
            String[] firstLineParameters = {"GET", "/testFolder", "HTTP/1.1"};//requestLines[0].split(" ");

            if(firstLineParameters.length < 3){
                // TODO send 400 Bad Request
            }

            String requestMethod = firstLineParameters[0];
            String requestURI = firstLineParameters[1];

//            Arrays.stream(firstLineParameters).forEach(line -> System.out.println("\tfirstLine:{"+line+"}"));

            if(requestMethod.compareTo("GET") == 0) {
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
                // DEPRECATED: ResponseBuilder is now static
//                ResponseBuilder responseBuilder = new ResponseBuilder();

                if(file.canRead()) {
                    System.out.println("can read file");
                    byte[] contentBytes = Files.readAllBytes(Paths.get(file.getPath()));

                    // get MIME type
                    // https://stackoverflow.com/questions/51438/getting-a-files-mime-type-in-java
                    String contentType = URLConnection.guessContentTypeFromName(file.getName());
                    System.out.println("MIME: "+contentType);

                    String header = ResponseBuilder.generateHeader(contentType,StatusCode.SUCCESS_200_OK ,contentBytes.length);
                    System.out.println("\n\nheader: \n"+header);

                    outputStream.write(header);
                    outputStream.flush();

                    rawOutputStream.write(contentBytes);
                    outputStream.flush();

                } else {
                    System.err.println("CANNOT READ FILE");
                }
            }

            else if(requestMethod.compareTo("POST") == 0) {
                // TODO: implement x-www-form-urlencoded, multi-part form, binary data.
                // TODO: need to implement the RequestParser to get Content-Length etc...
//                File clientUpload = new File("book.jpg");
                System.out.println("client sent a POST");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


// ----- experiment
//            StringBuilder line = new StringBuilder();
//
//            int readInt = 0;
//            int lineStage = 0; // 0 == null; 1 = \r; 2 = \r\n; 3 = \r\n\r; 4 = \r\n\r\n
//            for (int i = 0; i < totalBytesRead; i++) {
//                switch ((char)requestBuffer[i]) {
//                    case '\n':
//                        if(lineStage == 1) {
//                            lineStage += 1;
//                        } else if(lineStage == 3) {
//                            // END OF HEADER DETECTED
//                            lineStage = 0; // reset tracker
//
//
//                        }
//                        line.append("\\n");
//                        break;
//                    case '\r':
//                        if(lineStage == 0) {
//                            lineStage += 1;
//                        } else if(lineStage == 2) {
//                            lineStage +=1;
//                        }
//                        line.append("\\r");
//                        break;
//                    default:
//                        line.append((char)requestBuffer[i]);
//
//                }
//            }
//            System.out.println(line);

//------- end experiment