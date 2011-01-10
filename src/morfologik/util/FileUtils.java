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

	/**
	 * Reads all bytes from an input stream (until EOF).
	 */
	public static byte[] readFully(InputStream stream) throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 16);
		final byte[] buffer = new byte[1024 * 8];
		int bytesCount;
		while ((bytesCount = stream.read(buffer)) > 0) {
			baos.write(buffer, 0, bytesCount);
		}
		return baos.toByteArray();
	}

	/**
	 * Read enough bytes to fill <code>array</code> If there are not enough
	 * bytes, throw an exception.
	 */
	public static void readFully(InputStream in, byte[] array)
	        throws IOException {
		int offset = 0;
		int cnt;
		while ((cnt = in.read(array, offset, array.length - offset)) > 0) {
			offset += cnt;

			if (offset == array.length)
				break;
		}

		if (cnt < 0)
			throw new EOFException();
	}

	/**
	 * Read exactly 4 bytes from the input stream.
	 */
	public static int readInt(InputStream in) throws IOException {
		int v = 0;
		for (int i = 0; i < 4; i++) {
			v = (v << 8) | (readByte(in) & 0xff);
		}
	    return v;
    }

    /**
     * 
     */
    public static void writeInt(OutputStream os, int v) throws IOException {
        os.write( v >>> 24);
        os.write((v >>> 16) & 0xff);
        os.write((v >>> 8)  & 0xff);
        os.write( v         & 0xff);
    }
	
	/**
     * Read exactly 2 bytes from the input stream.
     */
    public static short readShort(InputStream in) throws IOException {
        return (short) (readByte(in) << 8 |
                        readByte(in) & 0xff);
    }

	/**
	 * Read exactly one byte from the input stream.
	 * 
	 * @throws EOFException if EOF is reached.
	 */
	public static byte readByte(InputStream in) throws IOException {
		int b = in.read();
		if (b == -1)
			throw new EOFException();
		return (byte) b;
	}

    /**
     * 
     */
    public static void writeShort(OutputStream os, short v) throws IOException {
        os.write((v >>> 8)  & 0xff);
        os.write( v         & 0xff);
    }
}