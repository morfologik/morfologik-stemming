package morfologik.fsa;

import java.io.*;

/**
 * CFSA (Compact Finite State Automaton) traversal implementation. This is a
 * slightly reorganized version of {@link FSA5} offering smaller automata size
 * at some performance penalty.
 * 
 * <p>
 * This automaton version is not supported nor produced by the original
 * <code>fsa</code> package. Use {@link CFSAEncoder} to convert FSA version 5
 * automata to this compact representation.
 * </p>
 * 
 * <p>
 * The encoding of automaton body is as follows.
 * </p>
 * 
 * <pre>
 * ---- FSA header (standard)
 * Byte                            Description 
 *       +-+-+-+-+-+-+-+-+\
 *     0 | | | | | | | | | +------ '\'
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     1 | | | | | | | | | +------ 'f'
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     2 | | | | | | | | | +------ 's'
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     3 | | | | | | | | | +------ 'a'
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     4 | | | | | | | | | +------ version (fixed 0xc5)
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     5 | | | | | | | | | +------ filler character
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     6 | | | | | | | | | +------ annot character
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     7 |C|C|C|C|G|G|G|G| +------ C - node data size (ctl), G - address size (gotoLength)
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *  8-32 | | | | | | | | | +------ labels mapped for type (1) of arc encoding. 
 *       : : : : : : : : : |
 *       +-+-+-+-+-+-+-+-+/
 * 
 * ---- Start of a node; only if automaton was compiled with NUMBERS option.
 * 
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
 *        
 * ---- A vector of node's arcs. Conditional format, depending on flags.
 * 
 * 1) NEXT bit set, mapped arc label. 
 * 
 *                +--------------- arc's label mapped in M bits if value > 0
 *                | +------------- node pointed to is next
 *                | | +----------- the last arc of the node
 *         _______| | | +--------- the arc is final
 *        /       | | | |
 *       +-+-+-+-+-+-+-+-+\
 *     0 |M|M|M|M|M|1|L|F| +------ flags + (M) index of the mapped label.
 *       +-+-+-+-+-+-+-+-+/
 * 
 * 2) NEXT bit set, label separate
 * 
 *                +--------------- arc's label stored separately
 *                | +------------- node pointed to is next
 *                | | +----------- the last arc of the node
 *                | | | +--------- the arc is final
 *                | | | |
 *       +-+-+-+-+-+-+-+-+\
 *     0 |0|0|0|0|0|1|L|F| +------ flags
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     1 | | | | | | | | | +------ label
 *       +-+-+-+-+-+-+-+-+/
 * 
 * 3) NEXT bit not set. Full arc.
 * 
 *                  +------------- node pointed to is next
 *                  | +----------- the last arc of the node
 *                  | | +--------- the arc is final
 *                  | | |
 *       +-+-+-+-+-+-+-+-+\
 *     0 |A|A|A|A|A|N|L|F| +------ flags + (A) address field, lower bits
 *       +-+-+-+-+-+-+-+-+/
 *       +-+-+-+-+-+-+-+-+\
 *     1 | | | | | | | | | +------ label
 *       +-+-+-+-+-+-+-+-+/
 *       : : : : : : : : :       
 *       +-+-+-+-+-+-+-+-+\
 * gtl-1 |A|A|A|A|A|A|A|A| +------ address, continuation (MSB)
 *       +-+-+-+-+-+-+-+-+/
 * </pre>
 */
public final class CFSA extends FSA {
	/**
	 * Bitmask indicating that an arc corresponds to the last character of a
	 * sequence available when building the automaton.
	 */
	protected static final int BIT_FINAL_ARC = 1 << 0;

	/**
	 * Bitmask indicating that an arc is the last one of the node's list and the
	 * following one belongs to another node.
	 */
	protected static final int BIT_LAST_ARC = 1 << 1;

	/**
	 * Bitmask indicating that the target node of this arc follows it in the
	 * compressed automaton structure (no goto field).
	 */
	protected static final int BIT_TARGET_NEXT = 1 << 2;

	/**
	 * An array of bytes with the internal representation of the automaton.
	 * Please see the documentation of this class for more information on how
	 * this structure is organized.
	 */
	protected byte[] arcs;

	/**
	 * The length of the node prefix field if the automaton was compiled with
	 * <code>NUMBERS</code> option.
	 */
	protected int ctl;

	/**
	 * Label mapping for arcs of type (1) (see class documentation). The array
	 * is indexed by mapped label's value and contains the original label.
	 */
	private byte[] labelMapping;

	/**
	 * Creates a new automaton reading it from a file in FSA format, version 5.
	 */
	public CFSA(InputStream fsaStream, String dictionaryEncoding)
	        throws IOException {
		super(fsaStream, dictionaryEncoding);
	}

	/**
	 * Returns the start node of this automaton. May return <code>0</code> if
	 * the start node is also an end node.
	 */
	public int getRootNode() {
		return getEndNode(getFirstArc(skipArc(ctl)));
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
			return skipArc(arc);
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
	 * TODO: layout-dependence here.
	 */
	public byte getArcLabel(int arc) {
		if (isNextSet(arc) && isLabelCompressed(arc)) {
			return this.labelMapping[(arcs[arc] >>> 3) & 0x1f];
		} else {
			return arcs[arc + 1];
		}
	}

	/*
	 * TODO: layout-dependence here.
	 */
	public boolean isArcFinal(int arc) {
		return (arcs[arc] & BIT_FINAL_ARC) != 0;
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

	/**
	 * Returns <code>true</code> if this arc has <code>NEXT</code> bit set.
	 * 
	 * @see #BIT_LAST_ARC
	 */
	public boolean isArcLast(int arc) {
		// TODO: layout-dependence here.
		return (arcs[arc] & BIT_LAST_ARC) != 0;
	}

	/**
	 * @see #BIT_TARGET_NEXT
	 */
	public boolean isNextSet(int arc) {
		// TODO: layout-dependence here.
		return (arcs[arc] & BIT_TARGET_NEXT) != 0;
	}

	/**
	 * Returns <code>true</code> if the label is compressed inside flags byte.
	 */
	public boolean isLabelCompressed(int arc) {
		assert isNextSet(arc) : "Only applicable to arcs with NEXT bit.";
		return (arcs[arc] & (-1 << 3)) != 0;
	}

	/**
	 * Returns the address of the node pointed to by this arc.
	 */
	protected final int getDestinationNodeOffset(int arc) {
		// TODO: layout-dependence here.
		if (isNextSet(arc)) {
			/* the destination node follows this arc in the array */
			return skipArc(arc);
		} else {
			/*
			 * the destination node address has to be extracted from the arc's
			 * goto field.
			 */
			int r = 0;
			for (int i = gotoLength; --i >= 1;) {
				r = r << 8 | (arcs[arc + 1 + i] & 0xff);
			}
			r = r << 8 | (arcs[arc] & 0xff);
			return r >>> 3;
		}
	}

	/**
	 * Perform node/ arcs counting.
	 */
	protected void doCount() {
		int nodeCount = 1;
		int arcsCount = 1;

		int offset = getFirstArc(skipArc(ctl));
		while (offset < arcs.length) {
			boolean atNode = isArcLast(offset);
			offset = skipArc(offset);
			if (atNode && offset < arcs.length) {
				nodeCount++;
				offset = getFirstArc(offset);
			}
			arcsCount++;
		}

		this.nodeCount = nodeCount;
		this.arcsCount = arcsCount;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readHeader(DataInputStream in, long fileSize)
	        throws IOException {
		super.readHeader(in, fileSize);

		// Check if we support such version of the automata.
		if (version != FSA.VERSION_CFSA) {
			throw new IOException("Cannot read FSA in version "
			        + version
			        + " (built with flags: "
			        + FSAHelpers.flagsToString(FSAHelpers.getFlags(version))
			        + ")."
			        + " Class "
			        + this.getClass().getName()
			        + " supports version "
			        + FSA.VERSION_CFSA
			        + " only (compressed FSA5: "
			        + FSAHelpers.flagsToString(FSAHelpers
			                .getFlags(FSA.VERSION_CFSA)) + ").");
		}

		/*
		 * Determine if the automaton was compiled with NUMBERS. If so, modify
		 * ctl and goto fields accordingly.
		 */
		if ((super.gotoLength & 0xf0) != 0) {
			this.ctl = super.gotoLength >>> 4;
			super.gotoLength = (byte) (super.gotoLength & 0x0f);
		}

		/*
		 * Read mapping dictionary.
		 */
		labelMapping = new byte[1 << 5];
		in.readFully(labelMapping);

		/*
		 * Read arcs' data.
		 */
		arcs = FSA.readFully(in);
	}

	/**
	 * Read the arc's layout and skip as many bytes, as needed.
	 */
	private int skipArc(int offset) {
		// TODO: layout-dependence here.
		if (isNextSet(offset)) {
			if (isLabelCompressed(offset)) {
				offset++;
			} else {
				offset += 1 + 1;
			}
		} else {
			offset += 1 + gotoLength;
		}
		return offset;
	}
}