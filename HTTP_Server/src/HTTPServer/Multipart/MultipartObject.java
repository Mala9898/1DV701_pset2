package HTTPServer.Multipart;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-20
 */
public class MultipartObject {
    private String name;
    private String filename;
    private String contentType;

    private byte[] data;

    public MultipartObject(String name, String filename, byte[] data) {
        this.name = name;
        this.filename = filename;
        this.data = data;
    }
    public byte[] getData() {
        return data;
    }
    public String getName() {
        return name;
    }
    public String getFilename() {
        return filename;
    }
    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
