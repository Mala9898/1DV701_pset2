package test_pack;

import java.io.*;
import java.nio.charset.Charset;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-19
 */
public class StreamTester {
    public static void main(String[] args) throws IOException {
        InputStream inputStream = new ByteArrayInputStream("--BOUNDARY\r\nline2\r\nlinethree\r\nline_four\r\n--BOUNDARY\r\nline_five\r\n--BOUNDARY--".getBytes());

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "US-ASCII"));
        String boundary = "--BOUNDARY";
        String boundaryEND = "--BOUNDARY--";
        String line;

        boolean isPart = false;
        StringBuilder part = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            if(line.compareTo(boundary) == 0) {
                if(!isPart) {
                    // first part we enconter
                    isPart = true;
                    continue;
                } else {
                    // we're done with the previous part. start a new one
                    System.out.printf("final part {%s} %n", part.toString());
                    part.setLength(0);
                    continue;
                }
            }
            if(line.compareTo(boundaryEND) == 0) {
                // TODO finish up
                System.out.printf("final part {%s} %n", part.toString());
                break;
            }

            part.append(line);
        }
//        final int BUF_LEN = 18;
//        byte[] buffer = new byte[BUF_LEN];
//        int bytesRead = 0;
//        try {
//            while( (bytesRead = inputStream.read(buffer)) != -1) {
//                String fragment = new String(buffer, 0, BUF_LEN);
//                System.out.printf("part: {%s} %n", fragment);
//            }
//        } catch (IOException e){
//            e.printStackTrace();
//        }

    }
}
