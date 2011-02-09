package morfologik.fsa;

import java.io.IOException;
import java.io.InputStream;

import morfologik.util.FileUtils;
import static morfologik.util.FileUtils.*;

/**
 * Standard FSA file header, as described in <code>fsa</code> package documentation. 
 */
final class FSAHeader {
	/**
	 * FSA magic (4 bytes).
	 */
	public final static int FSA_MAGIC = ('\\' << 24) | ('f' << 16) | ('s' << 8) | ('a');

	/**
	 * Maximum length of the header block.
	 */
	public static final int MAX_HEADER_LENGTH = 4 + 8;
	
	/** FSA version number. */
	public byte version;

	/** Filler character. */
	public byte filler;
	
	/** Annotation character. */
	public byte annotation;
	
	/** Goto field (may be a compound, depending on the automaton version). */
	public byte gtl;

	/**
	 * Read FSA header from a stream, consuming its bytes.
	 * 
	 * @throws IOException If the stream ends prematurely or if it contains invalid data.
	 */
	public static FSAHeader read(InputStream in) throws IOException {
		if (FSA_MAGIC != FileUtils.readInt(in))
			throw new IOException("Invalid file header magic bytes.");

		final FSAHeader h = new FSAHeader();
		h.version = readByte(in);
		h.filler = readByte(in);
		h.annotation = readByte(in);
		h.gtl = readByte(in);

	    return h;
    }
}
