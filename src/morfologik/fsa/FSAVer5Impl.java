package morfologik.fsa;

import java.io.*;

/**
 * FSA (Finite State Automaton) dictionary traversal implementation for version
 * 5 of the FSA automaton.
 * 
 * <p>
 * Version 5 indicates the dictionary was built with these flags:
 * {@link FSAFlags#FLEXIBLE}, {@link FSAFlags#STOPBIT} and
 * {@link FSAFlags#NEXTBIT}. The internal representation of the FSA must
 * therefore follow this description (please note this format describes only a
 * single transition (arc), not the entire dictionary file).
 * 
 * <pre>
 * ---- this header only if automaton was compiled with NUMBERS option.
 * Byte
 *        +-+-+-+-+-+-+-+-+\
 *      0 | | | | | | | | | \  LSB
 *        +-+-+-+-+-+-+-+-+  +
 *      1 | | | | | | | | |  |      number of strings recognized
 *        +-+-+-+-+-+-+-+-+  +----- by the automaton starting
 *        : : : : : : : : :  |      from this node.
 *        +-+-+-+-+-+-+-+-+  +
 *  ctl-1 | | | | | | | | | /  MSB
 *        +-+-+-+-+-+-+-+-+/
 * ---- 
 * Byte
 *       +-+-+-+-+-+-+-+-+\
 *     0 | | | | | | | | | +------ label
 *       +-+-+-+-+-+-+-+-+/
 * 
 *                  +------------- node pointed to is next
 *                  | +----------- the last arc of the node
 *                  | | +--------- the arc is final
 *                  | | |
 *             +-----------+
 *             |    | | |  |
 *         ___+___  | | |  |
 *        /       \ | | |  |
 *       MSB           LSB |
 *        7 6 5 4 3 2 1 0  |
 *       +-+-+-+-+-+-+-+-+ |
 *     1 | | | | | | | | | \ \
 *       +-+-+-+-+-+-+-+-+  \ \  LSB
 *       +-+-+-+-+-+-+-+-+     +
 *     2 | | | | | | | | |     |
 *       +-+-+-+-+-+-+-+-+     |
 *     3 | | | | | | | | |     +----- target node address (in bytes)
 *       +-+-+-+-+-+-+-+-+     |      (not present except for the byte
 *       : : : : : : : : :     |       with flags if the node pointed to
 *       +-+-+-+-+-+-+-+-+     +       is next)
 *   gtl | | | | | | | | |    /  MSB
 *       +-+-+-+-+-+-+-+-+   /
 * gtl+1                           (gtl = gotoLength)
 * </pre>
 */
public final class FSAVer5Impl extends FSA {
    /**
     * Bitmask indicating that an arc is the last one of the node's list and the
     * following one belongs to another node.
     */
    private static final int BITMASK_LASTARC = 1 << 1;

    /**
     * Bitmask indicating that an arc corresponds to the last character of a
     * sequence available when building the automaton.
     */
    private static final int BITMASK_FINALARC = 1 << 0;

    /**
     * Bitmask indicating that the next node follows this arc in the compressed
     * automaton structure.
     */
    private static final int BITMASK_NEXTBIT = 1 << 2;

    /**
     * Size of a single arc (in bytes).
     */
    protected int arcSize;

    /**
     * An offset in the arc structure, where the address field begins. For this
     * version of the automaton, this is a constant value.
     */
    protected final static int gotoOffset = 1;

    /**
     * An array of bytes with the internal representation of the automaton.
     * Please see the documentation of this class for more information on how
     * this structure is organized.
     */
    protected byte[] arcs;

    /**
     * The length of the node prefix field if the automaton was
     * compiled with <code>NUMBERS</code> option. 
     */
    protected int ctl;

    /**
     * Creates a new automaton reading it from a file in FSA format, version 5.
     */
    public FSAVer5Impl(InputStream fsaStream, String dictionaryEncoding)
	    throws IOException {
	super(fsaStream, dictionaryEncoding);
    }


    /**
     * Returns the number of arcs in this automaton. This method performs a full
     * scan of all arcs in this automaton.
     */
    public int getNumberOfArcs() {
	final int startNode = getRootNode();

	int arcOffset = getFirstArc(startNode);
	int arcsNumber = 0;
	byte flags;
	while (arcOffset < arcs.length) {
	    arcsNumber++;

	    // Follow to the next arc.
	    flags = arcs[arcOffset + gotoOffset];
	    if ((flags & BITMASK_NEXTBIT) != 0) {
		/* The next arc is right after this one (no address). */
		arcOffset += gotoOffset + 1;
	    } else {
		/* The next arc is after the address. */
		arcOffset += gotoOffset + gotoLength;
	    }

	    if ((flags & BITMASK_LASTARC) != 0) {
		/* This is the last arc of the current node, 
		 * skip the next node's data. */
		arcOffset += ctl;
	    }	    
	}

	return arcsNumber;
    }

    /**
     * Returns the number of nodes in this automaton. This method performs a
     * full scan of all arcs in this automaton.
     */
    public int getNumberOfNodes() {
	final int startNode = getRootNode();

	int nodes = 1;
	int arcOffset = getFirstArc(startNode);
	int arcsNumber = 0;

	byte flags;
	while (arcOffset < arcs.length) {
	    arcsNumber++;

	    // Follow to the next arc.
	    flags = arcs[arcOffset + gotoOffset];
	    if ((flags & BITMASK_NEXTBIT) != 0) {
		/* The next arc is right after this one (no address). */
		arcOffset += gotoOffset + 1;
	    } else {
		/* The next arc is after the address. */
		arcOffset += gotoOffset + gotoLength;
	    }

	    if ((flags & BITMASK_LASTARC) != 0) {
		/* This is the last arc of the current node, 
		 * skip the next node's data. */
		arcOffset += ctl;
		nodes++;
	    }	    
	}

	return nodes;
    }

    /**
     * Returns the start node of this automaton. May return <code>0</code> if
     * the start node is also an end node.
     */
    public int getRootNode() {
	return getEndNode(getFirstArc(ctl + arcSize));
    }

    /**
     * {@inheritDoc}
     */
    protected void readHeader(DataInput in, long fileSize) throws IOException {
	super.readHeader(in, fileSize);

	// Check if we support such version of the automata.
	if (version != FSA.VERSION_5) {
	    throw new IOException("Cannot read FSA in version "
		    + version
		    + " (built with flags: "
		    + FSAHelpers.flagsToString(FSAHelpers.getFlags(version))
		    + ")."
		    + " Class "
		    + this.getClass().getName()
		    + " supports version "
		    + FSA.VERSION_5
		    + " only ("
		    + FSAHelpers.flagsToString(FSAHelpers
			    .getFlags(FSA.VERSION_5)) + ").");
	}

	/* 
	 * Determine if the automaton was compiled with NUMBERS. If so, modify
	 * ctl and goto fields accordingly.
	 */
	if ((super.gotoLength & 0xf0) != 0) {
	    this.ctl = super.gotoLength >>> 4;
	    super.gotoLength = (byte) (super.gotoLength & 0x0f);
	}
	arcSize = gotoLength + 1;

	final int numberOfArcs = (int) fileSize - /* header size */8;
	arcs = new byte[numberOfArcs];
	in.readFully(arcs);
    }

    /*
     * 
     */
    public final int getFirstArc(int node) {
	return ctl + node;
    }

    /*
     * 
     */
    public final int getNextArc(int node, int arc) {
	if (isArcLast(arc))
	    return 0;
	else
	    return arc + arcSize;
    }

    /*
     * 
     */
    public int getArc(int node, byte label) {
        for (int arc = getFirstArc(node); arc != 0; arc = getNextArc(node, arc)) {
            if (getArcLabel(arc) == label)
        	return arc;
        }

        // An arc labeled with "label" not found.
        return 0;
    }

    /*
     * 
     */
    public int getEndNode(int arc) {
	final int nodeOffset = getDestinationNodeOffset(arc);
	if (0 == nodeOffset) {
	    throw new RuntimeException("This is a terminal arc [" + arc + "]");
	}
	return nodeOffset;
    }

    /*
     * 
     */
    public byte getArcLabel(int arc) {
	return arcs[arc];
    }

    /*
     * 
     */
    public boolean isArcFinal(int arc) {
        return (arcs[arc + gotoOffset] & BITMASK_FINALARC) != 0;
    }

    /*
     * 
     */
    public boolean isArcTerminal(int arc) {
	return (0 == getDestinationNodeOffset(arc));
    }

    /**
     * {@inheritDoc}
     * 
     * For this automaton version, an additional {@link FSAFlags#NUMBERS} flag
     * may be set to indicate the automaton contains extra fields for each node.
     */
    @Override
    public int getFlags() {
	return super.getFlags() | (this.ctl != 0 ? FSAFlags.NUMBERS.bits : 0);
    }

    /*
     * 
     */
    private boolean isArcLast(int arc) {
        return (arcs[arc + gotoOffset] & BITMASK_LASTARC) != 0;
    }

    /**
     * Returns an integer offset from bit-packed representation.
     */
    private final int gotoFieldToOffset(final int start, final int n) {
        int r = 0;
        for (int i = n - 1; i >= 0; --i) {
            r <<= 8;
            r = r | (arcs[start + i] & 0xff);
        }
        return r;
    }

    /** 
     * Returns the address of the node pointed to by this arc. 
     */
    private final int getDestinationNodeOffset(int arc) {
	if ((arcs[arc + gotoOffset] & BITMASK_NEXTBIT) != 0) {
	    /* the destination node follows this arc in the array */
	    return arc + gotoOffset + 1;
	} else {
	    /*
	     * the destination node address has to be extracted from the arc's
	     * goto field.
	     */
	    return gotoFieldToOffset(arc + gotoOffset, gotoLength) >>> 3;
	}
    }
}