
package com.dawidweiss.fsa;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;


/**
 * FSA (Finite State Automaton) dictionary traversal implementation
 * for version 5 of the FSA automaton.
 *
 * Version 5 indicates the dictionary was built with these flags:
 * FSA_FLEXIBLE, FSA_STOPBIT and FSA_NEXTBIT. The internal representation
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
 * The traversal of such FSA could be made extremely fast using pointers only (or integer
 * indices over a byte array in case of Java). However, for the sake of clarity, this class
 * uses explicit Node/ Arc objects.
 *
 * @author Dawid Weiss
 */
public final class FSAVer5Impl
    extends FSA
{
    /** The flag used to indicate that an arc is the last one of node's list and the following one
     *  will start another node.
     */
    protected static final int FLAG_STOPBIT = 0x02;

    /** A size, in bytes, of a single arc */
    protected int       arcSize;

    /** An offset in the arc structure, where 'address' field begins. */
    protected int       gotoOffset;

    /** An array of bytes holding the "packed" representation of this automaton.
     *  PLease see the documentation of this class for a more information on
     *  how this structure is organized.
     */
    protected byte []   arcs;


    /** An arc (a labelled transition between two nodes).
     *
     *  An arc has an explicit representation in the transitions array, a pointer
     *  to that representation is stored in this class.
     *
     *  This class is public, so that it can be used directly without unnecessary
     *  runtime-typing and casting. This should be an extreme case when performance
     *  counts, though. Use interfaces when portability is the objective.
     */
    private final class Arc
        implements FSA.Arc
    {
        /** An index in the dictionary where this arc begins. */
        private int     offset;

        /** Creates an arc, whose representation starts at <code>offset</code> in the arcs array.
         */
        public Arc( int offset )
        {
            this.offset = offset;
        }

        /** Returns the destination node, pointed to by this arc.
         *  If this arc points to a final node, null is returned.
         */
        public FSA.Node getDestinationNode()
        {
            if (this.pointsToFinalNode())
                return null;

            // Return a new object, which is needed simply to hold the value
            // of a pointer in the arcs array.
            return new Node( getDestinationNodeOffset() );
        }

        /** Returns the label of this arc. */
        public byte getLabel()
        {
            // first byte of arc's representation is its label.
            return arcs[ offset ];
        }

        /** Returns true if the destination node is a final node (final state of the FSA). */
        public boolean pointsToFinalNode()
        {
            return (arcs[ offset + gotoOffset] & 0x01) != 0;
        }

        /** Returns true if this arc is the last arc of some node's representation */
        protected boolean isLast()
        {
            return (arcs[ offset + gotoOffset] & 0x02) != 0;
        }

        /** Returns the address of the node pointed to by this arc. */
        private int getDestinationNodeOffset()
        {
            if ((arcs[ offset + gotoOffset ] & 0x04) != 0)
            {
                /* the destination node follows this arc in the array */
                return offset + gotoOffset + 1;
            }
            else
            {
                /* the destination node address has to be extracted from the arc's goto field. */
                return gotoFieldToOffset( arcs, offset + gotoOffset, gotoLength ) >>> 3;
            }
        }
    }


    /** A node (a collection of outgoing arcs).
     *
     *  A node has an implicit representation - it is stored as a consecutive
     *  vector of arcs. The last arc of the node has a special bit set to 1.
     *
     *  This class is public, so that it can be used directly without unnecessary
     *  runtime-typing and casting. This should be an extreme case when performance
     *  counts, though. Use interfaces when portability is the objective.
     */
    private final class Node
        extends FSAAbstractNode
    {
        /** The offset of this node's representation in the arcs array */
        private int offset;

        /** Creates a node, whose representation starts at <code>offset</code> in
         *  the arcs array.
         */
        public Node( int offset )
        {
            this.offset = offset;
        }

        /** Returns the first outgoing arc of this node.
         *  Theoretically, this method should ALWAYS return at least one arc.
         *  (final nodes have no representation).
         */
        public FSA.Arc getFirstArc()
        {
            return new Arc( offset );
        }

        /** Returns a subsequent arc of this node. null is returned when
         *  no more arcs are available.
         *
         *  This method will not verify whether the "input" arc really
         *  belongs to this node. Caution is advised.
         */
        public FSA.Arc getNextArc( FSA.Arc arc )
        {
            Arc myArc = (Arc) arc;
            if (myArc.isLast())
                return null;
            else
            {
                return new Arc( myArc.offset + arcSize );
            }
        }
    }


    /**
     *  Returns the number of arcs in this automaton
     */
    public int getNumberOfArcs()
    {
        FSA.Node startNode = getStartNode();
        FSAVer5Impl.Arc arc = (FSAVer5Impl.Arc) startNode.getFirstArc();

        int arcsNumber = 0;

        while (arc.offset < arcs.length)
        {
            arcsNumber++;

            // next arc.
            if ((arcs[ arc.offset + gotoOffset ] & 0x04) != 0)
            {
                /* the next arc is right after this one. */
                arc = new Arc( arc.offset + gotoOffset + 1);
            }
            else
            {
                /* the destination node address has to be extracted from the arc's goto field. */
                arc = new Arc( arc.offset + gotoOffset + gotoLength );
            }
        }

        return arcsNumber;
    }


    /** Returns the number of nodes in this automaton.
     *  This method is not efficient in this representation of an automaton. It takes
     *  O(N), where N is the number of arcs to calculate the number of nodes.
     */
    public int getNumberOfNodes()
    {
        int offset = gotoOffset;
        int nodes  = 0;
        while (offset < arcs.length)
        {
            // This arc marks an end of the list of node's arrays.
            if ( (arcs[offset] & FLAG_STOPBIT) != 0)
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
        super( fsaStream, dictionaryEncoding );
    }


    /** Returns the start node of this automaton.
     *  May return null if the start node is also an end node.
     */
    public FSA.Node getStartNode()
    {
        return new Node( arcSize ).getFirstArc().getDestinationNode();
    }


    /* *************************************** */
    /* **** PROTECTED AND PRIVATE METHODS **** */
    /* *************************************** */


    /** Reads a FSA from the stream.
     *  Throws an exception if magic number is not found.
     *
     *  @throws IOException
     *  @throws ClassNotFoundException  If the magic number doesn't match.
     */
    protected void readFromStream(DataInput in, long fileSize)
        throws IOException
    {
        super.readFromStream( in, fileSize );

        // check if we support such version of the automata.
        if (version!=5)
            throw new IOException("Cannot read FSA in version " + version
               + " (built with flags: " + FSAHelpers.flagsToString( FSAHelpers.getFlags( version ) ) + "). "
               + "Class " + this.getClass().getName()
               + " supports version 5 only (" + FSAHelpers.flagsToString( FSAHelpers.getFlags( 5 ) ) + ").");

        // read transitions data.
        gotoLength = (byte) (gotoLength & 0x0f);
        arcSize    = gotoLength + 1;
        int numberOfArcs = (int) fileSize - /* header size */ 8;

        arcs = new byte [ numberOfArcs ];
        in.readFully( arcs );

        // This is a constant value for this type of encoding.
        gotoOffset = 1;
    }


    /** Returns an integer offset from a 'packed' representation */
    protected static int gotoFieldToOffset(byte [] bytes, int start, int n)
    {
      int r = 0;
      for (int i = n - 1; i >= 0; --i) {
        r <<= 8; r = r | ((int)bytes[start + i] & 0xff);
      }
      return r;
    }
}