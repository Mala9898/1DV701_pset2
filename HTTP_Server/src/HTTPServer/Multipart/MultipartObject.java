package HTTPServer.Multipart;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: Stanislaw J. Malec  (sm223ak@student.lnu.se)
 * @author: Love Samuelsson     (ls223qx@student.lnu.se)
 * 2020-02-20
 */

/**
 * Metadata and content of an multipart object
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

        // capture three groups: disposition (form-data), name, and filename.

        // ^ : start capturing at the start of each line
        // match "Content-Disposition:" literally
        // match an optional whitespace ([\s]{0,1} means match whitespace between 0 and 1 times (inclusive) )
        // (?<disposition> : this creates a capture group named "disposition", the value of which can be referenced by this name
        //      [\w/-] match any alphanumeric character, "/" character and "-" character. The "+" at the end means "match at least one character"
        // the filename capture group allows all characters except for quotation marks. This is done by creating a negated set: [^\"] "everything but a literal quote mark"
        // Two other capture groups are defined in a similar way.
        // Pattern.MULTILINE is used to enable "^" start of line and  "$" end of line anchoring
        // Pattern.CASE_INSENSITIVE because header field names are case insensitive according to RFC7230. This means that "CoNtEnT-TyPe" is valid :)
        Pattern pattern = Pattern.compile( "^Content-Disposition:[\\s]{0,1}(?<disposition>[\\w\\/-]+)(?:;\\s{0,1}name=\"(?<name>[^\\\"]+)\")(?:;\\s{0,1}filename=\"(?<filename>[^\\\"]+)\")?", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);

        while (matcher.find()) {
            if(matcher.groupCount()==3) {
                // replace spaces with %20 in the filename
                String to20ify = matcher.group("filename");
                if (to20ify == null) {
                    throw new IllegalArgumentException("Filename not supported, simplify!");
                }
                dispositionFilename = to20ify.replace(" ", "%20");
                dispositionType = matcher.group("disposition");
                dispositionName = matcher.group("name");
                hasDisposition = true;

            } else {
                throw new IllegalArgumentException("Image was not found in multipart/form-data");
            }
        }

        // capture Content-Type value in a regex group.
        Pattern pattern2 = Pattern.compile( "^Content-Type:\\s{0,1}(?<contentType>[\\w\\/]+)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pattern2.matcher(line);

        while (matcher2.find()) {
            if(matcher2.groupCount() == 1) {
                dispositionContentType = matcher2.group("contentType");
                hasContentType = true;
            }
        }


    }

    /**
     * Get actual content bytes
     * @return
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Name header
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Content-Type header value
     * @return
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Does this multipart-object have a disposition
     * @return
     */
    public boolean isHasDisposition() {
        return hasDisposition;
    }

    /**
     * Do we have a Content-Type header value?
     * @return
     */
    public boolean isHasContentType() {
        return hasContentType;
    }

    /**
     * Disposition type
     * @return
     */
    public String getDispositionType() {
        return dispositionType;
    }

    /**
     * Disposition name
     * @return
     */
    public String getDispositionName() {
        return dispositionName;
    }

    /**
     * disposition filename
     * @return
     */
    public String getDispositionFilename() {
        return dispositionFilename;
    }

    /**
     * disposition Content-Type value
     * @return
     */
    public String getDispositionContentType() {
        return dispositionContentType;
    }

    /**
     * override the disposition filename (used with POST if the file already exists since POST creates a new resource)
     * @param dispositionFilename
     */
    public void setDispositionFilename(String dispositionFilename) {
        this.dispositionFilename = dispositionFilename;
    }
}
