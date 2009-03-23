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

import morfologik.util.FileUtils;

/**
 * FSA (Finite State Automaton) traversal implementation, abstract base class
 * for all versions of FSA.
 * 
 * <p>
 * This class implements Finite State Automaton traversal as described in Jan
 * Daciuk's <i>Incremental Construction of Finite-State Automata and
 * Transducers, and Their Use in the Natural Language Processing</i> (PhD
 * thesis, Technical University of Gdansk).
 * 
 * <p>
 * This is a Java port of the original <code>FSA</code> class, implemented by
 * Jan Daciuk in the FSA package. Major redesign has been done, however, to fit
 * this implementation to the specifics of Java language and its coding style.
 */
public abstract class FSA {
    /**
     * Version number for version 5 of the automaton.
     */
    public final static byte VERSION_5 = 5;

    /**
     * A node of the FSA.
     * 
     * The operations of this interface should be implemented by all
     * version-specific implementations.
     */
    public interface Node {
	/**
	 * Returns the first outgoing arc of this node. Theoretically, this
	 * method should ALWAYS return at least one arc. (final nodes have no
	 * representation).
	 */
	public Arc getFirstArc();

	/**
	 * Returns a subsequent arc of this node, <code>null</code> is returned
	 * when no more arcs are available.
	 */
	public Arc getNextArc(Arc arc);

	/**
	 * Returns an arc with a given label, if it exists in this node. The
	 * default implementation in FSAAbstractNode simply traverses the list
	 * of arcs from the first, to the last - implementations of Node may
	 * implement a more efficient algorithm, if possible.
	 */
	public Arc getArcLabelledWith(byte label);
    }

    /**
     * An arc (a labeled transition between two nodes) of the FSA.
     * 
     * The operations of this interface should be implemented by all
     * version-specific implementations.
     */
    public interface Arc {
	/**
	 * Returns the destination node, pointed to by this arc. Terminal nodes
	 * throw a {@link RuntimeException} on this method.
	 */
	public Node getDestinationNode();

	/**
	 * Returns the label of this arc.
	 */
	public byte getLabel();

	/**
	 * @return Returns <code>true</code> if the destination node corresponds
	 *         to an input sequence of this automaton.
	 */
	public boolean isFinal();

	/**
	 * @return Returns <code>true</code> if this arc is the last one of the
	 *         owner node's arcs.
	 */
	public boolean isLast();

	/**
	 * @return Returns <code>true</code> if this arc does not have a
	 *         terminating {@link FSA.Node}, a call to
	 *         {@link #getDestinationNode()} should thrown an exception on
	 *         this arc.
	 */
	public boolean isTerminal();
    }

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
     * Returns a version number of this FSA.
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
     * .
     */
    public final int getFlags() {
	return FSAHelpers.getFlags(version);
    }

    /**
     * @return Return the annotation separator character, converted to a
     *         character according to the encoding scheme passed in in the
     *         constructor of this class.
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
     * @return Return the filler character, converted to a character according
     *         to the encoding scheme passed in in the constructor of this
     *         class.
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
     * Returns the start node of this automaton. May return null if the start
     * node is also an end node.
     */
    public abstract FSA.Node getStartNode();

    /**
     * @return Returns an object which can be used to traverse a finite state
     *         automaton.
     */
    public FSATraversalHelper getTraversalHelper() {
	return new FSATraversalHelper();
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
	if (!fsaFile.exists())
	    throw new IOException("File does not exist: "
		    + fsaFile.getAbsolutePath());

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
	for (int bytesRead = 0; bytesRead < header.length;)
	{
	    bytesRead += stream.read(header, bytesRead, header.length - bytesRead);	    
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

	if (magic[0] == '\\' && magic[1] == 'f' && magic[2] == 's' && magic[3] == 'a') {
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
}