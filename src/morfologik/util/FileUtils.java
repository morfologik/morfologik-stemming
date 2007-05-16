package morfologik.util;

import java.io.File;
import java.io.IOException;

/**
 * Utility functions.
 * 
 * @author Dawid Weiss
 */
public final class FileUtils {

    /**
     * No instances.
     */
    private FileUtils() {
        // empty
    }

    /**
     * Checks if the given file exists.
     */
    public static void assertExists(File fsaFile, boolean requireFile, boolean requireDirectory)
        throws IOException
    {
        if (!fsaFile.exists()) {
            throw new IOException("File does not exist: "
                    + fsaFile.getAbsolutePath());
        }
        
        if (requireFile) {
            if (!fsaFile.isFile() || !fsaFile.canRead()) {
                throw new IOException("Not a readable file: "
                        + fsaFile.getAbsolutePath());
            }
        }
        
        if (requireDirectory) {
            if (!fsaFile.isDirectory()) {
                throw new IOException("Not a directory: "
                        + fsaFile.getAbsolutePath());
            }
        }
    }
}