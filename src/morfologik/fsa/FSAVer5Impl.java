
package morfologik.fsa;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;


/**
 * FSA (Finite State Automaton) dictionary traversal implementation
 * for version 5 of the FSA automaton.
 *
 * <p>Version 5 indicates the dictionary was built with these flags:
 * {@link FSAFlags#FLEXIBLE}, {@link FSAFlags#STOPBIT} and {@link FSAFlags#NEXTBIT}. 
 * The internal representation
 * of the FSA must therefore follow this description (please note this
 * format describes only a single transition (arc), not the entire dictionary file).
 * <pre>
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
 *
 * <p>The traversal of such FSA could be made extremely fast using pointers only (or integer
 * indices over a byte array in case of Java). However, for the sake of clarity, this class
 * uses explicit Node/ Arc objects.
 */
public final class FSAVer5Impl extends FSA {
    /** 
     * Bitmask indicating that an arc is the last one of the node's list and the following one
     * belongs to another node.
     */
    private static final int BITMASK_LASTARC = 1 << 1;

    /** 
     * Bitmask indicating that an arc corresponds to the last character of a sequence available
     * when building the automaton. 
     */
    private static final int BITMASK_FINALARC = 1 << 0;

    /** 
     * Bitmask indicating that the next node follows this arc in the compressed automaton 
     * structure. 
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
     * Please see the documentation of this class for more information on
     * how this structure is organized.
     */
    protected byte [] arcs;


    /** 
     * An arc in version 5 of the automaton structure.
     */
    private final class Arc
        implements FSA.Arc
    {
        /** An index in the dictionary where this arc begins. */
        private final int offset;

        /** 
         * Creates an arc, whose representation starts at <code>offset</code> in the arcs array.
         */
        public Arc(final int offset)
        {
            this.offset = offset;
        }

        /** 
         * Returns the destination node, pointed to by this arc.
         */
        public FSA.Node getDestinationNode()
        {
            final int nodeOffset = getDestinationNodeOffset();

            if (0 == nodeOffset) {
                throw new RuntimeException("This is a terminal arc [" + offset + "]");
            }

            // Return a new object, which is needed simply to hold the value
            // of a pointer in the arcs array.
            return new Node(nodeOffset);
        }

        /** Returns the label of this arc. */
        public byte getLabel()
        {
            // first byte of arc's representation is its label.
            return arcs[offset];
        }

        /** 
         * {@inheritDoc} 
         */
        public boolean isLast()
        {
            return (arcs[offset + gotoOffset] & BITMASK_LASTARC) != 0;
        }

        /**
         * {@inheritDoc} 
         */
        public boolean isFinal() 
        {
            return (arcs[offset + gotoOffset] & BITMASK_FINALARC) != 0;
        }

        /**
         * {@inheritDoc}
         */
        public boolean isTerminal() {
            return (0 == getDestinationNodeOffset());
        }

        /** Returns the address of the node pointed to by this arc. */
        private int getDestinationNodeOffset()
        {
            if ((arcs[offset + gotoOffset] & BITMASK_NEXTBIT) != 0) {
                /* the destination node follows this arc in the array */
                return offset + gotoOffset + 1;
            } else {
                /* the destination node address has to be extracted from the arc's goto field. */
                return gotoFieldToOffset(offset + gotoOffset, gotoLength) >>> 3;
            }
        }
    }


    /** 
     * A node in version 5 of the automaton structure.
     *
     * @see FSA.Node
     */
    private final class Node implements FSA.Node {
        /** 
         * The offset of this node's representation in the arcs array 
         */
        private final int offset;

        /**
         * Creates a node at the given offset (first arc's).  
         */
        public Node(final int offset)
        {
            this.offset = offset;
        }

        /**
         * {@inheritDoc} 
         */
        public FSA.Arc getFirstArc()
        {
            return new Arc(offset);
        }

        /** 
         * {@inheritDoc} 
         */
        public FSA.Arc getNextArc(FSA.Arc arc)
        {
            final Arc myArc = (Arc) arc;
            if (myArc.isLast())
                return null;
            else
                return new Arc(myArc.offset + arcSize);
        }

        /** 
         * Returns an arc with a given label, if it exists in this node.
         */
        public final FSA.Arc getArcLabelledWith(byte label)
        {
            FSA.Arc arc;
            for (arc = getFirstArc(); arc != null; arc = getNextArc(arc)) {
                if (arc.getLabel() == label)
                    return arc;
            }

            // arc labelled with "label" not found.
            return null;
        }
    }


    /**
     * Returns the number of arcs in this automaton. 
     * This method performs a full scan of all arcs in this automaton.
     */
    public int getNumberOfArcs()
    {
        final FSA.Node startNode = getStartNode();

        FSAVer5Impl.Arc arc = (FSAVer5Impl.Arc) startNode.getFirstArc();
        int arcOffset = arc.offset;
        int arcsNumber = 0;
        while (arcOffset < arcs.length) {
            arcsNumber++;

            // go to next arc.
            if ((arcs[arcOffset + gotoOffset] & BITMASK_NEXTBIT) != 0) {
                /* the next arc is right after this one. */
                arcOffset = arcOffset + gotoOffset + 1;
            } else {
                /* the destination node address has to be extracted from the arc's goto field. */
                arcOffset = arcOffset + gotoOffset + gotoLength;
            }
        }

        return arcsNumber;
    }


    /** 
     * Returns the number of nodes in this automaton.
     * This method performs a full scan of all arcs in this automaton.
     */
    public int getNumberOfNodes()
    {
        int offset = gotoOffset;
        int nodes  = 0;
        while (offset < arcs.length) {
            // This arc marks an end of the list of node's arrays.
            if ((arcs[offset] & BITMASK_LASTARC) != 0)
                nodes++;
            offset += arcSize;
        }

        return nodes;
    }


    /**
     * Creates a new automaton reading it from a file in FSA format, version 5.
     */
    public FSAVer5Impl(InputStream fsaStream, String dictionaryEncoding)
        throws IOException
    {
        super(fsaStream, dictionaryEncoding);
    }


    /** 
     * Returns the start node of this automaton.
     * May return <code>null</code> if the start node 
     * is also an end node.
     */
    public FSA.Node getStartNode()
    {
        return new Node(arcSize).getFirstArc().getDestinationNode();
    }


    /** 
     * {@inheritDoc}
     */
    protected void readHeader(DataInput in, long fileSize)
        throws IOException
    {
        super.readHeader(in, fileSize);

        // Check if we support such version of the automata.
        if (version != FSA.VERSION_5) {
            throw new IOException("Cannot read FSA in version " + version
               + " (built with flags: " + FSAHelpers.flagsToString(FSAHelpers.getFlags( version )) + ")."
               + " Class " + this.getClass().getName()
               + " supports version " + FSA.VERSION_5 + " only (" + FSAHelpers.flagsToString(FSAHelpers.getFlags(FSA.VERSION_5)) + ").");
        }

        // Read transitions data.
        gotoLength = (byte) (gotoLength & 0x0f);
        arcSize = gotoLength + 1;

        final int numberOfArcs = (int) fileSize - /* header size */ 8;
        arcs = new byte [ numberOfArcs ];
        in.readFully( arcs );
    }


    /**
     * Returns an integer offset from bit-packed representation.
     */
    protected final int gotoFieldToOffset(final int start, final int n) {
	int r = 0;
	for (int i = n - 1; i >= 0; --i) {
	    r <<= 8;
	    r = r | (arcs[start + i] & 0xff);
	}
	return r;
    }
}