
package com.dawidweiss.fsa;


import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;


/**
 * FSA (Finite State Automaton) traversal implementation, abstract base class
 * for all versions of FSA.
 *
 * This class implements Finite State Automaton traversal as described in Jan Daciuk's
 * <i>Incremental Construction of Finite-State Automata and Transducers, and Their
 * Use in the Natural Language Processing</i> (PhD thesis, Technical University of Gdansk).
 *
 * This is a Java port of the original <code>fsa</code> class, implemented by
 * Jan Daciuk in the FSA package. Major redesign has been done, however, to fit this
 * implementation to the specifics of Java language and its coding style.
 *
 * @author Dawid Weiss
 */
public abstract class FSA
{
    /** 
     * These flags control the internal representation of a FSA.
     * They indicate how transitions (arcs) and nodes are stored. More info
     * in the original FSA package.
     */
    public final transient static int FSA_FLEXIBLE            = 1 << 0;
    public final transient static int FSA_STOPBIT             = 1 << 1;
    public final transient static int FSA_NEXTBIT             = 1 << 2;
    public final transient static int FSA_TAILS               = 1 << 3;
    public final transient static int FSA_WEIGHTED            = 1 << 4;
    public final transient static int FSA_LARGE_DICTIONARIES  = 1 << 5;

    /** Dictionary version (derived from the combination of flags). */
    protected  byte     version;

    /** The meaning of this field is not clear (check the FSA docs). */
    protected  byte     filler;

    /** Annotation separator is a special character used for separating "tokens" in a FSA.
     *  For instance an inflected form of a word may be separated from the base form.
     */
    protected  byte     annotationSeparator;

    /** Size of transition's destination node "address". This field may also
     *  have different interpretation, or may not be used at all. It depends on
     *  the combination of flags used for building FSA.
     */
    protected  byte     gotoLength;


    /**
     * Creates a new automaton reading the FSA automaton from an input stream.
     *
     * @param  fsaStream An input stream with FSA automaton.
     * @throws IOException if the dictionary file cannot be read, or version of the
     *         file is not supported.
     */
    protected FSA(InputStream fsaStream, String dictionaryEncoding)
        throws IOException
    {
        if (fsaStream == null)
            throw new IllegalArgumentException("The input stream must not be null.");

        /* This implementation requires the length of stream to be known in advance.
           Preload the dictionary entirely. */
        byte [] fsa = readFullyAndCloseInput(fsaStream);

        DataInputStream input = null;
        try
        {
            input = new DataInputStream( new ByteArrayInputStream( fsa ));
            readFromStream( input, fsa.length );
        }
        finally
        {
            if (input!=null)
                try { input.close(); } catch (IOException e) { /* Don't do anything. */ }
        }
    }


    /** Returns a version number of this FSA.
     *  The version number is a derivation of combination of flags and is exactly
     *  the same as in Jan Daciuk's FSA package.
     */
    public int getVersion()
    {
        return version;
    }


    /** 
     * Returns a set of flags for this FSA instance. Each flag is represented by a unique bit in
     * the integer returned. Therefore to check whether the dictionary has been built 
     * using {@link #FSA_FLEXIBLE}
     * flag, one must perform a bitwise AND:
     * <code>boolean isFlexible = ((dict.getFlags() & FSA.FSA_FLEXIBLE ) != 0)</code>
     */
    public int getFlags()
    {
        return FSAHelpers.getFlags( version );
    }


    /** 
     * Returns the number of arcs in this automaton. <b>Depending on the representation
     * of the automaton, this method may take a long time to finish.</b>
     */
    public abstract int getNumberOfArcs();


    /**
     * Returns the number of nodes in this automaton. <b>Depending on the representation
     * of the automaton, this method may take a long time to finish.</b>
     */
    public abstract int getNumberOfNodes();


    /**
     * Returns the start node of this automaton.
     * May return null if the start node is also an end node.
     */
    public abstract FSA.Node getStartNode();


    /**
     * Returns an object which can be used to traverse FSA.
     * The default implementation simply uses the public methods
     * of FSA, FSA.Node and FSA.Arc classes, but specific implementations
     * of FSA may return an optimized version of this class.
     */
    public FSATraverseHelper getTraverseHelper()
    {
        return new FSATraverseHelper();
    }


    /**
     * A node of the FSA.
     *
     * The operations of this interface should be implemented by
     * all version-specific implementations.
     */
    public interface Node
    {
        /**
         * Returns the first outgoing arc of this node.
         * Theoretically, this method should ALWAYS return at least one arc.
         * (final nodes have no representation).
         */
        public Arc getFirstArc();

        /**
         * Returns a subsequent arc of this node. null is returned when
         * no more arcs are available.
         */
        public Arc getNextArc( Arc arc );

        /**
         * Returns an arc with a given label, if it exists in this node.
         * The default implementation in FSAAbstractNode simply traverses
         * the list of arcs from the first, to the last - implementations
         * of Node may implement a more efficient algorithm, if possible.
         */
        public Arc getArcLabelledWith( byte label );
    }


    /** 
     * An arc (a labelled transition between two nodes) of the FSA.
     *
     * The operations of this interface should be implemented by
     * all version-specific implementations.
     */
    public interface Arc
    {
        /** 
         * Returns the destination node, pointed to by this arc.
         * If this arc points to a final node, null is returned.
         */
        public Node getDestinationNode();

        /** Returns the label of this arc. */
        public byte getLabel();

        /** Returns true if the destination node is a final node (final state of the FSA). */
        public boolean pointsToFinalNode();
    }


    /**
     * This static method will attempt to instantiate an appropriate implementation
     * of the FSA for the version found in file given in the input argument.
     *
     * An exception is thrown if no version is found.
     */
    public static FSA getInstance( File fsaFile, String dictionaryEncoding )
        throws IOException
    {
        if (!fsaFile.exists())
            throw new IOException(fsaFile.getCanonicalPath() + " (The system cannot find the file specified)");

        return getInstance( new FileInputStream( fsaFile ), dictionaryEncoding );
    }


    /**
     * This static method will attempt to instantiate an appropriate implementation
     * of the FSA for the version found in file given in the input argument.
     *
     * An exception is thrown if no version is found.
     */
    public static FSA getInstance( InputStream fsaStream, String dictionaryEncoding )
        throws IOException
    {
        if (fsaStream==null)
            throw new IllegalArgumentException("FSA stream cannot be null.");

        PushbackInputStream stream = new PushbackInputStream( fsaStream, 5 );

        byte [] header = new byte[5];
        stream.read(header);

        if ( header[0]=='\\' && header[1]=='f' && header[2]=='s' && header[3]=='a' )
        {
            // read FSA version
            byte version = header[4];

            // put back header info.
            stream.unread(header);

            switch (version)
            {
                case 0x05:  return new FSAVer5Impl( stream, dictionaryEncoding );
            }

            // no implementation found.
            throw new IOException("Cannot read FSA: support for version " + version
                + " (" + FSAHelpers.flagsToString( FSAHelpers.getFlags(version)) + ") not implemented.");
        }
        else
        {
            throw new IOException("Cannot read FSA: File does not begin with a valid magic number.");
        }
    }


    /* *************************************** */
    /* **** PROTECTED AND PRIVATE METHODS **** */
    /* *************************************** */


    /** 
     * Reads a FSA header from a stream.
     *
     * @throws IOException If the stream is not a dictionary,
     *         or if the version is not supported.
     */
    protected void readFromStream(DataInput in, long fileSize)
        throws IOException
    {
        final byte [] magic = new byte[4];
        in.readFully( magic );

        if ( magic[0]=='\\' && magic[1]=='f' && magic[2]=='s' && magic[3]=='a' )
        {
            // read FSA signature
            version             = in.readByte();
            filler              = in.readByte();
            annotationSeparator = in.readByte();
            gotoLength          = in.readByte();
        }
        else
        {
            throw new IOException("Cannot read FSA: File does not begin with a valid magic number.");
        }
    }


    /**
     * Reads all remaining data from the stream and closes
     * the stream.
     */
    private static byte[] readFullyAndCloseInput(InputStream input)
        throws IOException
    {
        byte result[] = null;
        try {
            result = readFully(input);
        } finally {
            try {
                input.close();
            } catch(IOException e) { }
        }
        return result;
    }


    /**
     * Reads all remaining data from the stream. Does not close
     * the input stream.
     */
    private static byte[] readFully(InputStream input)
        throws IOException
    {
    	ArrayList chunks = new ArrayList();
    	
        final int MIN_BUFFER_SIZE = 16 * 1024;
        byte buffer[] = null;
        do
        {
        	if (buffer == null)
        		buffer = new byte[MIN_BUFFER_SIZE];
            int bytesRead = input.read(buffer);
            if(bytesRead == -1)
            {
                break;
            }
            if(bytesRead == 0)
                break;
			
			if (bytesRead == buffer.length) {
				// store entire chunk
				chunks.add( buffer );
				buffer = null;
			} else {
				// store part of the chunk.
                byte part[] = new byte[bytesRead];
                System.arraycopy(buffer, 0, part, 0, bytesRead);
                chunks.add(part);
                // reuse buffer.
			}
        } while(true);
        
        // calculate overall size
        int size = 0;
        for (int i=chunks.size()-1;i>=0;i--) {
        	size += ((byte []) chunks.get(i)).length;
        }

		// allocate output
		byte [] output = new byte [size];
		
		// and copy it.
		int position = 0;
		int max = chunks.size();
        for (int i=0;i<max;i++) {
        	byte [] part = (byte []) chunks.get(i);
        	System.arraycopy(part, 0, output, position, part.length);
        	position += part.length;
        }

        return output;
    }
}