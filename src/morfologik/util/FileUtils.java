package morfologik.util;

import java.io.*;

/**
 * Utility functions.
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
    public static void assertExists(File fsaFile, boolean requireFile,
	    boolean requireDirectory) throws IOException {
	if (!fsaFile.exists()) {
	    throw new IOException("File does not exist: "
		    + fsaFile.getAbsolutePath());
	}

	if (requireFile) {
	    if (!fsaFile.isFile() || !fsaFile.canRead()) {
		throw new IOException("File cannot be read: "
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

    /**
     * Force any non-null closeables.
     */
    public static void close(Closeable... closeables) {
	for (Closeable c : closeables) {
	    if (c != null) {
		try {
		    c.close();
		} catch (IOException e) {
		    // too bad
		}
	    }
	}
    }
}