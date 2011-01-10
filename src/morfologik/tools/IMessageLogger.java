package morfologik.tools;

public interface IMessageLogger {

    /**
     * Log progress to the console.
     */
    public void log(String msg);

    /**
     * Log message header and save current time.
     */
    public void startPart(String header);

    /**
     * 
     */
    public void endPart();

    /**
     * Log a two-part message.
     */
    public void log(String header, Object v);

}