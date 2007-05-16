package morfologik.util;


/**
 * Utility functions.
 * 
 * @author Dawid Weiss
 */
public final class StringUtils {

    /**
     * No instances.
     */
    private StringUtils() {
        // empty
    }

    /**
     * Creates a chain of messages from nested exceptions.
     */
    public static String chainExceptionMessages(Throwable e) {
        final StringBuffer buf = new StringBuffer();
        while (e != null) {
            if (buf.length() > 0) buf.append(" (caused by ->) ");
            if (e.getMessage() == null) {
                buf.append(e.toString());
            } else {
                buf.append(e.getMessage());
            }
            e = e.getCause();
        }
        return buf.toString();
    }
}