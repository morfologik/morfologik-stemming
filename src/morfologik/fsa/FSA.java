package morfologik.fsa;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Iterator;

import morfologik.util.FileUtils;

/**
 * This class implements Finite State Automaton traversal as described in Jan
 * Daciuk's <i>Incremental Construction of Finite-State Automata and
 * Transducers, and Their Use in the Natural Language Processing</i> (PhD
 * thesis, Technical University of Gdansk).
 * 
 * <p>
 * This is an abstract base class for all forms of binary storage present in Jan
 * Daciuk's FSA package.
 */
public abstract class FSA implements Iterable<ByteBuffer> {
    /**
     * Version number for version 5 of the automaton.
     */
    public final static byte VERSION_5 = 5;

    /**
     * Dictionary version (derived from the combination of flags).
     */
    protected byte version;

    /**
     * The meaning of this field is not clear (check the FSA documentation).
     */
    protected byte filler;

    /**
     * Size of transition's destination node "address". This field may also have
     * different interpretation, or may not be used at all. It depends on the
     * combination of flags used for building FSA.
     */
    protected byte gotoLength;

    /**
     * Annotation separator is a special character used for separating "tokens"
     * in a FSA. For instance an inflected form of a word may be separated from
     * the base form.
     */
    private byte annotationSeparator;

    /**
     * The encoding (codepage) in which the dictionary has been compiled;
     * byte-to-character conversion scheme.
     */
    private String dictionaryEncoding;

    /**
     * Creates a new automaton reading the FSA automaton from an input stream.
     * 
     * @param fsaStream
     *            An input stream with FSA automaton.
     * @throws IOException
     *             if the dictionary file cannot be read, or version of the file
     *             is not supported.
     */
    protected FSA(InputStream fsaStream, String dictionaryEncoding)
	    throws IOException {
	if (fsaStream == null) {
	    throw new IllegalArgumentException(
		    "The input stream must not be null.");
	}

	if (dictionaryEncoding == null) {
	    throw new IllegalArgumentException(
		    "Dictionary encoding must not be null.");
	}
	this.dictionaryEncoding = dictionaryEncoding;

	/*
	 * This implementation requires the length of stream to be known in
	 * advance. Preload the dictionary entirely.
	 */
	final byte[] fsa = readFully(fsaStream);
	DataInputStream input = null;
	try {
	    input = new DataInputStream(new ByteArrayInputStream(fsa));
	    readHeader(input, fsa.length);
	} finally {
	    FileUtils.close(input);
	}
    }

    /**
     * Returns the version number of the binary representation of this FSA.
     * 
     * <p>
     * The version number is a derivation of combination of flags and is exactly
     * the same as in Jan Daciuk's FSA package.
     */
    public final int getVersion() {
	return version;
    }

    /**
     * Returns a set of flags for this FSA instance. Each flag is represented by
     * a unique bit in the integer returned. Therefore to check whether the
     * dictionary has been built using {@link FSAFlags#FLEXIBLE} flag, one must
     * perform a bitwise AND:
     * <code>boolean isFlexible = ((dict.getFlags() &amp; FSA.FSA_FLEXIBLE ) != 0)</code>
     */
    public final int getFlags() {
	return FSAHelpers.getFlags(version);
    }

    /**
     * Return the annotation separator character, converted to a character
     * according to the encoding scheme passed in in the constructor of this
     * class.
     */
    public final char getAnnotationSeparator() {
	try {
	    final String annotationChar = new String(
		    new byte[] { this.annotationSeparator },
		    this.dictionaryEncoding);
	    if (annotationChar.length() != 1) {
		throw new RuntimeException(
			"Unexpected annotation character length (should be 1): "
				+ annotationChar.length());
	    }
	    return annotationChar.charAt(0);
	} catch (UnsupportedEncodingException e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Return the filler character, converted to a character according to the
     * encoding scheme passed in in the constructor of this class.
     */
    public final char getFillerCharacter() {
	try {
	    final String fillerChar = new String(new byte[] { this.filler },
		    this.dictionaryEncoding);
	    if (fillerChar.length() != 1) {
		throw new RuntimeException(
			"Unexpected filler character length (should be 1): "
				+ fillerChar.length());
	    }
	    return fillerChar.charAt(0);
	} catch (UnsupportedEncodingException e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Returns the number of arcs in this automaton. <b>Depending on the
     * representation of the automaton, this method may take a long time to
     * finish.</b>
     */
    public abstract int getNumberOfArcs();

    /**
     * Returns the number of nodes in this automaton. <b>Depending on the
     * representation of the automaton, this method may take a long time to
     * finish.</b>
     */
    public abstract int getNumberOfNodes();

    /**
     * Returns an object which can be used to walk the edges of this finite
     * state automaton and match arbitrary sequences against its states.
     */
    public FSATraversalHelper getTraversalHelper() {
	return new FSATraversalHelper(this);
    }

    /**
     * This static method will attempt to instantiate an appropriate
     * implementation of the FSA for the version found in file given in the
     * input argument.
     * 
     * @throws IOException
     *             An exception is thrown if no corresponding FSA parser is
     *             found or if the input file cannot be opened.
     */
    public static FSA getInstance(File fsaFile, String dictionaryEncoding)
	    throws IOException {
	if (!fsaFile.exists()) {
	    throw new IOException("File does not exist: "
		    + fsaFile.getAbsolutePath());
	}

	return getInstance(new FileInputStream(fsaFile), dictionaryEncoding);
    }

    /**
     * This static method will attempt to instantiate an appropriate
     * implementation of the FSA for the version found in file given in the
     * input argument.
     * 
     * @throws IOException
     *             An exception is thrown if no corresponding FSA parser is
     *             found or if the input file cannot be opened.
     */
    public static FSA getInstance(InputStream fsaStream,
	    String dictionaryEncoding) throws IOException {
	if (fsaStream == null)
	    throw new IllegalArgumentException("FSA stream cannot be null.");

	final PushbackInputStream stream = new PushbackInputStream(fsaStream, 5);
	final byte[] header = new byte[5];
	for (int bytesRead = 0; bytesRead < header.length;) {
	    bytesRead += stream.read(header, bytesRead, header.length
		    - bytesRead);
	}

	if (header[0] == '\\' && header[1] == 'f' && header[2] == 's'
		&& header[3] == 'a') {
	    // Read FSA version
	    final byte version = header[4];

	    // put back header info
	    stream.unread(header);

	    switch (version) {
	    case 0x05:
		return new FSAVer5Impl(stream, dictionaryEncoding);
	    }

	    // No supporting implementation found.
	    throw new IOException("Cannot read FSA: support for version "
		    + version + " ("
		    + FSAHelpers.flagsToString(FSAHelpers.getFlags(version))
		    + ") not implemented.");
	} else {
	    throw new IOException(
		    "Cannot read FSA: file does not begin with a valid magic number.");
	}
    }

    /**
     * Reads a FSA header from a stream.
     * 
     * @throws IOException
     *             If the stream is not a dictionary, or if the version is not
     *             supported.
     */
    protected void readHeader(DataInput in, long fileSize) throws IOException {
	final byte[] magic = new byte[4];
	in.readFully(magic);

	if (magic[0] == '\\' && magic[1] == 'f' && magic[2] == 's'
		&& magic[3] == 'a') {
	    version = in.readByte();
	    filler = in.readByte();
	    annotationSeparator = in.readByte();
	    gotoLength = in.readByte();
	} else {
	    throw new IOException(
		    "Cannot read FSA: File does not begin with a valid magic number.");
	}
    }

    /**
     * Reads all bytes from an input stream.
     * 
     * @param stream
     * @return Returns an array of read bytes.
     */
    protected byte[] readFully(InputStream stream) throws IOException {
	final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 16);
	final byte[] buffer = new byte[1024 * 8];
	int bytesCount;
	while ((bytesCount = stream.read(buffer)) > 0) {
	    baos.write(buffer, 0, bytesCount);
	}
	return baos.toByteArray();
    }

    /**
     * Returns an iterator over all binary sequences starting from the initial
     * FSA state and ending in final nodes. The returned iterator is a
     * {@link ByteBuffer} that changes on each call to {@link Iterator#next()},
     * so if the content should be preserved, it must be copied somewhere else.
     * 
     * <p>
     * It is guaranteed that the returned byte buffer is backed by a byte array
     * and that the content of the byte buffer starts at the array's index 0.
     */
    public Iterator<ByteBuffer> iterator() {
	return getTraversalHelper().getAllSubsequences(getRootNode());
    }

    /**
     * Returns the identifier of the root node of this automaton. May return 0
     * if the start node is also the end node.
     * 
     * @see #getTraversalHelper()
     */
    public abstract int getRootNode();

    /**
     * Returns the identifier of the first arc leaving <code>node</code> or 0 if
     * the node has no outgoing arcs.
     * 
     * @see #getTraversalHelper()
     */
    public abstract int getFirstArc(int node);

    /**
     * Returns the identifier of an arc leaving <code>node</code> and labeled
     * with <code>label</code>. An identifier equal to 0 means the node has no
     * outgoing arc labeled <code>label</code>.
     * 
     * @see #getTraversalHelper()
     */
    public abstract int getArc(int node, byte label);

    /**
     * Returns the identifier of the next arc after <code>arc</code> and leaving
     * <code>node</code>. Zero is returned if no more arcs are available for the
     * node.
     * 
     * @see #getTraversalHelper()
     */
    public abstract int getNextArc(int node, int arc);

    /**
     * Return the end node pointed to by a given <code>arc</code>. Terminal arcs
     * (those that point to a terminal state) have no end node representation
     * and throw a runtime exception.
     * 
     * @see #getTraversalHelper()
     */
    public abstract int getEndNode(int arc);

    /**
     * Return the label associated with a given <code>arc</code>.
     */
    public abstract byte getArcLabel(int arc);

    /**
     * Returns <code>true</code> if the destination node at the end of this
     * <code>arc</code> corresponds to an input sequence created when building
     * this automaton.
     */
    public abstract boolean isArcFinal(int arc);

    /**
     * Returns <code>true</code> if this <code>arc</code> does not have a
     * terminating node.
     */
    public abstract boolean isArcTerminal(int arc);
}