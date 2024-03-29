package HTTPServer.Abstractions;

import HTTPServer.Multipart.MultipartObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-23
 */
public class BodyParser {

    public BodyParser() {
        // Empty constructor, use this class as generic object
    }

    /**
     * get raw body bytes
     *
     * @param in            InputStream where content is located
     * @param contentLength Expected content length
     * @return A byte array containing the data of the body.
     * @throws IOException on read failure
     */
    public byte[] getBinaryContent(InputStream in, int contentLength) throws IOException {
        byte[] content = new byte[contentLength];

        // credit: the use of ByteArrayOutputStream: https://www.baeldung.com/convert-input-stream-to-array-of-bytes
        ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();

        int totalRead = 0;
        int bytesRead;
        while ((bytesRead = in.read(content, 0, contentLength)) != -1) {
            contentBuffer.write(content, 0, bytesRead);
            totalRead += bytesRead;
            System.out.printf("read  %10d/%d %n", totalRead, contentLength);
            if (totalRead >= contentLength) {
                System.out.println("got all the data");
                break;
            }
        }
        contentBuffer.close();
        return contentBuffer.toByteArray();
    }

    /**
     * Get multipart objects from a stream. Runs in O(n) time.
     *
     * @param inputStream incoming data
     * @param _boundary   Starting boundary
     * @return A list containing the multipart payloads with each payload stored in a MultipartObject
     * @throws IOException On IO Error
     */
    public List<MultipartObject> getMultipartContent(InputStream inputStream, String _boundary) throws IOException {

        BufferedInputStream reader = new BufferedInputStream(inputStream);

        String boundarySTART = _boundary; // "--XYZ"
        String boundary = _boundary + "\r\n";
        String payloadStart = "\r\n\r\n";
        String boundaryAnotherPart = "\r\n";
        String boundaryEndPart = "--";
        int contentBufferLength = boundary.length() + 4;

        ArrayList<MultipartObject> toReturn = new ArrayList<>();
        ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
        ByteBuffer tempHeaderBuffer = ByteBuffer.allocate(1024);
        ByteBuffer tempBuffer = ByteBuffer.allocate(contentBufferLength);

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
            if (!isPart) {
                if (readByte == (int) boundary.charAt(matchCounter)) {
                    matchCounter += 1;
                    if (matchCounter == boundary.length()) {
                        isPart = true;
                        continue;
                    }
                }
                else {
                    matchCounter = 0;
                    continue;
                }
            }

            // we're at least past the header start boundary
            if (isPart) {
                if (!isPartPayload) {
                    tempHeaderBuffer.put((byte) readByte);

                    // check if we're at start of payload "\r\n\r\n"
                    if (readByte == (int) payloadStart.charAt(partPayloadStartMatchCounter)) {
                        partPayloadStartMatchCounter += 1;
                        if (partPayloadStartMatchCounter == payloadStart.length()) {
                            isPartPayload = true;

                            int endCondition = tempHeaderBuffer.position() - 4;
                            tempHeaderBuffer.flip();
                            for (int i = 0; i < endCondition; i++) {
                                byte toCopy = tempHeaderBuffer.get();
                                headerBuffer.write(toCopy);
                            }
                        }
                        continue;
                    }
                    else {
                        partPayloadStartMatchCounter = 0;
                        continue;
                    }
                }

                if (isPartPayload) {

                    // here -> --XYZ|
                    if (partPayloadEndMatchCounter < boundarySTART.length()) {
                        if (readByte == (int) boundarySTART.charAt(partPayloadEndMatchCounter)) {
                            partPayloadEndMatchCounter += 1;

                            // --XYZ detected: enter boundary mode
                            if (partPayloadEndMatchCounter == boundarySTART.length()) {
                                boundaryCheckingMode = true;
                            }
                            else {
                                // save byte to a temporary buffer in case it turns out to be a false alarm
                                tempBuffer.put((byte) readByte);
                            }
                            continue;
                        }
                    }
                    // --XYZ| <-- here
                    else {
                        // three options
                        // 1. --XYZ-- (end of multipart/form-data)
                        // 2. --XYZ (another part)
                        // 3. --XYZ{anything} false alarm
                        if (boundaryCheckingMode) {
                            if (readByte == (int) boundaryEndPart.charAt(boundaryEndPartCounter)) {
                                canOnlyBeEnd = true;
                                boundaryEndPartCounter += 1;
                                if (boundaryEndPartCounter == boundaryEndPart.length()) {
                                    boundaryCheckingMode = false;
                                    tempBuffer = ByteBuffer.allocate(contentBufferLength);
                                    partPayloadEndMatchCounter = 0;
                                    boundaryEndPartCounter = 0;
                                    partPayloadStartMatchCounter = 0;

                                    MultipartObject multipartObject = new MultipartObject(headerBuffer.toByteArray(), contentBuffer.toByteArray());
                                    toReturn.add(multipartObject);

                                    headerBuffer.reset();
                                    tempHeaderBuffer = ByteBuffer.allocate(1024);

                                    isPartPayload = false;
                                    matchCounter = 0;
                                    break;
                                }
                                continue;
                            }
                            else if (!canOnlyBeEnd) {
                                if (readByte == (int) boundaryAnotherPart.charAt(boundaryAnotherPartCounter)) {
                                    boundaryAnotherPartCounter += 1;
                                    if (boundaryAnotherPartCounter == boundaryAnotherPart.length()) {
                                        boundaryCheckingMode = false;
                                        tempBuffer = ByteBuffer.allocate(contentBufferLength);
                                        partPayloadEndMatchCounter = 0;
                                        boundaryAnotherPartCounter = 0;
                                        partPayloadStartMatchCounter = 0;

                                        isPartPayload = false;
                                        matchCounter = 0;

                                        MultipartObject multipartObject = new MultipartObject(headerBuffer.toByteArray(), contentBuffer.toByteArray());
                                        toReturn.add(multipartObject);

                                        headerBuffer.reset();
                                        tempHeaderBuffer = ByteBuffer.allocate(1024);

                                        contentBuffer.reset();
                                    }
                                    continue;
                                }
                            }

                        }
                    }

                    if (partPayloadEndMatchCounter > 0) {
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

        return toReturn;

    }

}
