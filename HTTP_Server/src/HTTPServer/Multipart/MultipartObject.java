package HTTPServer.Multipart;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-20
 */
public class MultipartObject {
    private String name;
    private String contentType;

    private byte[] header;
    private byte[] data;

    private boolean hasDisposition = false;
    private boolean hasContentType = false;

    private String dispositionType = "";
    private String dispositionName = "";
    private String dispositionFilename = "";
    private String dispositionContentType = "";

    public MultipartObject(byte[] header, byte[] data) {
        this.data = data;
        this.header = header;

        String line = new String(header);

        Pattern pattern = Pattern.compile( "^Content-Disposition:[\\s]{0,1}(?<disposition>[\\w\\/-]+)(?:;\\s{0,1}name=\"(?<name>[\\w-\\[\\]]+)\")(?:;\\s{0,1}filename=\"(?<filename>[\\w._ \\-]+)\")?", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {
//            System.out.printf("group count: %d %n", matcher.groupCount());
            if(matcher.groupCount() >= 2) {
                dispositionType = matcher.group("disposition");
                dispositionName = matcher.group("name");
                hasDisposition = true;
            }
            if(matcher.groupCount()==3) {
                dispositionFilename = matcher.group("filename");
                hasDisposition = true;
            }
            continue;
        }

        Pattern pattern2 = Pattern.compile( "^Content-Type:\\s{0,1}(?<contentType>[\\w\\/]+)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(line);

        while (matcher2.find()) {
//            System.out.printf("group count: %d %n", matcher2.groupCount());
            if(matcher2.groupCount() == 1) {
                dispositionContentType = matcher2.group("contentType");
                hasContentType = true;
                continue;
            }
        }


    }
    public byte[] getData() {
        return data;
    }
    public String getName() {
        return name;
    }
    public String getContentType() {
        return contentType;
    }
    public boolean isHasDisposition() {
        return hasDisposition;
    }

    public boolean isHasContentType() {
        return hasContentType;
    }

    public String getDispositionType() {
        return dispositionType;
    }

    public String getDispositionName() {
        return dispositionName;
    }

    public String getDispositionFilename() {
        return dispositionFilename;
    }

    public String getDispositionContentType() {
        return dispositionContentType;
    }

    public void setDispositionFilename(String dispositionFilename) {
        this.dispositionFilename = dispositionFilename;
    }
}
