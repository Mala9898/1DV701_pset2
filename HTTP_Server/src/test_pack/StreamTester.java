package test_pack;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-19
 */
public class StreamTester {
    public static void main(String[] args) throws IOException {
        String testData1 = "--XYZ\r\n" +
                "someheader#1\r\n" +
                "\r\n" +
                "payload#1\r\n" +
                "--XYZ\r\n" +
                "someheader#2\r\n" +
                "\r\n" +
                "payload#2" +
                "--XYZ--";
        String testData2 = "--XYZ\r\nsomeheader\r\n\r\npayload content\r\nline_four\r\n--XYZ\r\nline_five\r\n--XYZ--";
        InputStream inputStream = new ByteArrayInputStream(testData1.getBytes("US-ASCII"));

        BufferedInputStream reader = new BufferedInputStream(inputStream);

        String boundarySTART = "--XYZ";
        String boundary = "--XYZ\r\n";
        String boundaryEND = "--XYZ--";
        String payloadStart = "\r\n\r\n";
        String boundaryAnotherPart = "\r\n";
        String boundaryEndPart = "--";
        int contentBufferLength = boundary.length()+4;

        ArrayList<byte[]> toReturn = new ArrayList<byte[]>();
        ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();
//        ByteBuffer contentBuffer = ByteBuffer.allocate(contentBufferLength);
        ByteBuffer tempBuffer = ByteBuffer.allocate(contentBufferLength);

        StringBuilder part = new StringBuilder();
        int readByte = 0;

        boolean isPart = false;
        boolean isPartPayload = false;
        boolean boundaryCheckingMode = false;
        boolean canOnlyBeEnd = false;
        int matchCounter = 0;
        int partPayloadStartMatchCounter = 0;
        int partPayloadEndMatchCounter = 0;

        int boundaryAnotherPartCounter = 0;
        int boundaryEndPartCounter = 0;
        while ((readByte = reader.read()) != -1) {
            // detect start boundary
            if(!isPart) {
                if(readByte == (int)boundary.charAt(matchCounter)) {
                    matchCounter+= 1;
                    if(matchCounter == boundary.length()){
                        isPart = true;
                        System.out.println("Starting boundary detected!");
                        continue;
                    }
                } else {
                    matchCounter = 0;
                    continue;
                }
            }

            // we're at least past the header start boundary
            if(isPart) {
                if(!isPartPayload) {
                    // check if we're at start of payload "\r\n\r\n"
                    if(readByte == (int)payloadStart.charAt(partPayloadStartMatchCounter)) {
                        partPayloadStartMatchCounter += 1;
                        if(partPayloadStartMatchCounter == payloadStart.length()){
                            isPartPayload = true;
                            System.out.println("Start of part payload detected!");
                            continue;
                        }
                    } else {
                        partPayloadStartMatchCounter = 0;
                        continue;
                    }
                }

                if(isPartPayload) {

                    // here -> --XYZ|
                    if(partPayloadEndMatchCounter < boundarySTART.length()) {
                        if(readByte == (int)boundarySTART.charAt(partPayloadEndMatchCounter)) {
//                        boundaryCheckingMode = true;
                            partPayloadEndMatchCounter += 1;

                            // --XYZ detected: enter boundary mode
                            if(partPayloadEndMatchCounter == boundarySTART.length()){
                                boundaryCheckingMode = true;

                                System.out.println("--XYZ detected ... entering boundary checking mode");
                                continue;
                            } else {
                                // save byte to a temporary buffer in case it turns out to be a false alarm
                                tempBuffer.put((byte)readByte);
                                continue;
                            }
                        }
                    }
                    // --XYZ| <-- here
                    else {
                        // three options
                        // 1. --XYZ (another part)
                        // 2. --XYZ-- (end of multipart/form-data)
                        // 3. --XYZ{anything} false alarm
                        if(boundaryCheckingMode) {
                            if(readByte == (int)boundaryEndPart.charAt(boundaryEndPartCounter)) {
                                canOnlyBeEnd = true;
                                boundaryEndPartCounter += 1;
                                if(boundaryEndPartCounter == boundaryEndPart.length()){
                                    System.out.println("END of multipart found");
                                    boundaryCheckingMode = false;
                                    tempBuffer = ByteBuffer.allocate(contentBufferLength);
                                    partPayloadEndMatchCounter = 0;
                                    boundaryEndPartCounter = 0;
                                    partPayloadStartMatchCounter = 0;

                                    toReturn.add(contentBuffer.toByteArray());
                                    isPartPayload = false;
                                    matchCounter = 0;
                                    continue;
                                }
                                continue;
                            }
                            else if(!canOnlyBeEnd) {
                                if(readByte == (int)boundaryAnotherPart.charAt(boundaryAnotherPartCounter)) {
                                    boundaryAnotherPartCounter += 1;
                                    if(boundaryAnotherPartCounter == boundaryAnotherPart.length()){
                                        System.out.println("another multipart section found");
                                        boundaryCheckingMode = false;
                                        tempBuffer = ByteBuffer.allocate(contentBufferLength);
                                        partPayloadEndMatchCounter = 0;
                                        boundaryAnotherPartCounter = 0;
                                        partPayloadStartMatchCounter = 0;

                                        isPartPayload = false;
                                        matchCounter = 0;

                                        toReturn.add(contentBuffer.toByteArray());
                                        contentBuffer.reset();
                                        continue;
                                    }
                                    continue;
                                }
                            }

                        }
                    }

                    if(partPayloadEndMatchCounter > 0){
                        // check if we have to write what a false alarm when detecting the end
                        tempBuffer.flip(); // make the end of buffer the position of the last element
                        while (tempBuffer.hasRemaining()) {
                            contentBuffer.write(tempBuffer.get());
                        }
                        tempBuffer = ByteBuffer.allocate(contentBufferLength);
                        partPayloadEndMatchCounter = 0;
                    }

                    // current byte is part of a payload
                    contentBuffer.write((byte) readByte);

                }
            }
        }
        int i = 0;
        System.out.println("\tparts found: "+toReturn.size());
        for(byte[] bytes : toReturn) {
            StringBuilder stringPayload = new StringBuilder();
            for(byte b : bytes) {
                switch (Character.getType((char)b)) {
                    case Character.CONTROL:
                        stringPayload.append(Integer.toString((char)b));
                        break;
                    default:
                        stringPayload.append(Character.toString((char)b));
                }
            }
            System.out.printf("payload part #%d {%s} %n",i, stringPayload.toString());
            i++;
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
