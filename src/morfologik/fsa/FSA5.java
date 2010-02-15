package morfologik.fsa;

import java.io.*;

import morfologik.util.IntHolder;

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
public final class FSA5 extends FSA {
	/**
	 * Bit indicating that an arc corresponds to the last character of a
	 * sequence available when building the automaton.
	 */
	protected static final int BIT_FINAL_ARC = 1 << 0;

	/**
	 * Bit indicating that an arc is the last one of the node's list and the
	 * following one belongs to another node.
	 */
	protected static final int BIT_LAST_ARC = 1 << 1;

	/**
	 * Bit indicating that the target node of this arc follows it in the
	 * compressed automaton structure (no goto field).
	 */
	protected static final int BIT_TARGET_NEXT = 1 << 2;

	/**
	 * An offset in the arc structure, where the address and flags field begins.
	 * In version 5 of FSA automata, this value is constant (1, skip label).
	 */
	protected final static int ADDRESS_OFFSET = 1;

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
	public int nodeDataLength;

	/**
	 * Creates a new automaton reading it from a file in FSA format, version 5.
	 */
	public FSA5(InputStream fsaStream, String dictionaryEncoding)
	        throws IOException {
		super(fsaStream, dictionaryEncoding);
	}

	/**
	 * Perform node/ arcs counting.
	 */
	protected void doCount(IntHolder arcsh, IntHolder nodesh) {
		int nodeCount = 1;
		int arcsCount = 1;

		int offset = getFirstArc(skipArc(nodeDataLength));
		while (offset < arcs.length) {
			boolean atNode = isArcLast(offset);
			offset = skipArc(offset);
			if (atNode && offset < arcs.length) {
				nodeCount++;
				offset = getFirstArc(offset);
			}
			arcsCount++;
		}

		nodesh.value = nodeCount;
		arcsh.value = arcsCount;
	}

	/**
	 * Read the arc's layout and skip as many bytes, as needed.
	 */
	private int skipArc(int offset) {
		return offset + (isNextSet(offset) ? 1 + 1 : 1 + gotoLength);
	}

	/**
	 * Returns the start node of this automaton. May return <code>0</code> if
	 * the start node is also an end node.
	 */
	public int getRootNode() {
		return getEndNode(getFirstArc(skipArc(nodeDataLength)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readHeader(DataInputStream in, long fileSize)
	        throws IOException {
		super.readHeader(in, fileSize);

		// Check if we support such version of the automata.
		if (getVersion() != FSA.VERSION_5) {
			throw new IOException("Cannot read FSA in version "
			        + getVersion()
			        + " (built with flags: "
			        + FSAHelpers.flagsToString(FSAHelpers.getFlags(getVersion()))
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
			this.nodeDataLength = super.gotoLength >>> 4;
			super.gotoLength = (byte) (super.gotoLength & 0x0f);
		}

		final int numberOfArcs = (int) fileSize - /* header size */8;
		arcs = new byte[numberOfArcs];
		in.readFully(arcs);
	}

	/*
     * 
     */
	public final int getFirstArc(int node) {
		return nodeDataLength + node;
	}

	/*
     * 
     */
	public final int getNextArc(int arc) {
		if (isArcLast(arc))
			return 0;
		else
			return skipArc(arc);
	}

	/*
     * 
     */
	public int getArc(int node, byte label) {
		for (int arc = getFirstArc(node); arc != 0; arc = getNextArc(arc)) {
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
		return (arcs[arc + ADDRESS_OFFSET] & BIT_FINAL_ARC) != 0;
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
		return super.getFlags()
		        | (this.nodeDataLength != 0 ? FSAFlags.NUMBERS.bits : 0);
	}

	/**
	 * Returns <code>true</code> if this arc has <code>NEXT</code> bit set.
	 * 
	 * @see #BIT_LAST_ARC
	 */
	public boolean isArcLast(int arc) {
		return (arcs[arc + ADDRESS_OFFSET] & BIT_LAST_ARC) != 0;
	}

	/**
	 * @see #BIT_TARGET_NEXT
	 */
	public boolean isNextSet(int arc) {
		return (arcs[arc + ADDRESS_OFFSET] & BIT_TARGET_NEXT) != 0;
	}

	/**
	 * Returns an integer encoded in byte-packed representation.
	 */
	public static final int decodeFromBytes(final byte[] arcs, final int start,
	        final int n) {
		int r = 0;
		for (int i = n; --i >= 0;) {
			r = r << 8 | (arcs[start + i] & 0xff);
		}
		return r;
	}

	/**
	 * Returns the address of the node pointed to by this arc.
	 */
	protected final int getDestinationNodeOffset(int arc) {
		if (isNextSet(arc)) {
			/* the destination node follows this arc in the array */
			return skipArc(arc);
		} else {
			/*
			 * the destination node address has to be extracted from the arc's
			 * goto field.
			 */
			return decodeFromBytes(arcs, arc + ADDRESS_OFFSET, gotoLength) >>> 3;
		}
	}
}