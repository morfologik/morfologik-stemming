package com.dawidweiss.fsa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.dawidweiss.fsa.FSA.Node;

/**
 * <p>An iterator that traverses all final states reachable
 * from a given {@link FSA.Node} and returns {@link String} objects 
 * corresponding to final states.
 * 
 * @author Dawid Weiss
 */
public final class FSAFinalStatesIterator implements Iterator {
    /**
     * Default expected depth of the recursion stack (estimated longest
     * sequence in the automaton). Buffers expand by the same value if exceeded.
     */
    private final static int EXPECTED_MAX_STATES = 15;

    /** An internal cache for the next element in the FSA */
    private byte[] nextElement = null;

    /**
     * A buffer for the current sequence of bytes from the current node
     * to the root.
     */
    private byte [] buffer = new byte[EXPECTED_MAX_STATES];

    private final ArrayList nodes = new ArrayList(EXPECTED_MAX_STATES);
    private final ArrayList arcs = new ArrayList(EXPECTED_MAX_STATES);

    /**
     * Create an instance of the iterator for a given
     * node. 
     */
    FSAFinalStatesIterator(FSA.Node node) {
        if (node.getFirstArc() != null) {
            pushNode(node);
        }
    }

    /** Returns <code>true</code> if there are still elements in this iterator. */
    public boolean hasNext()
    {
        if (nextElement == null) {
            nextElement = advance();
        }

        return nextElement != null;
    }

    /**
     * Overriden from {@link Iterator}. Use {@link #nextState()} if you
     * can to avoid cast.
     * 
     * @return Returns <code>byte[]</code> with the next final state
     * in the automaton.
     */
    public Object next()
    {
        return nextState();
    }
    
    /**
     * Returns the next available element.
     * 
     * @throws NoSuchElementException If this method is called after {@link #hasNext()} 
     *      returned <code>false</code>.
     */
    public byte [] nextState() {
        if (nextElement != null)
        {
            final byte[] cache = nextElement;
            nextElement  = null;
            return cache;
        } else {
            final byte[] cache = advance();
            if (cache == null) {
                throw new NoSuchElementException("No next element to traverse.");
            }
            return cache;
        }
    }

    /** 
     * Advances to the next available final state. 
     */
    private final byte[] advance()
    {
        if (arcs.isEmpty()) {
            return null;
        }

        while (!arcs.isEmpty()) {
            final int lastIndex = arcs.size() - 1;
            final FSA.Arc arc = (FSA.Arc) arcs.get(lastIndex);
            final FSA.Node node = (FSA.Node) nodes.get(lastIndex);

            if (arc == null) {
                // remove the current node from the queue
                arcs.remove(lastIndex);
                nodes.remove(lastIndex);
                continue;
            }

            // Go to the next arc, but leave it on the stack
            // so that we keep the recursion depth level accurate. 
            arcs.set(lastIndex, node.getNextArc(arc));

            // expand buffer if needed.
            final int bufferLength = this.buffer.length;
            if (lastIndex >= bufferLength) {
                this.buffer = FSAHelpers.resizeByteBuffer(buffer, bufferLength + EXPECTED_MAX_STATES);
            }
            buffer[lastIndex] = arc.getLabel();

            if (!arc.isTerminal()) {
                // recursively descend into the arc's node.
                pushNode(arc.getDestinationNode());
            }

            if (arc.isFinal()) {
                final byte [] tempBuffer = new byte [lastIndex + 1];
                System.arraycopy(buffer, 0, tempBuffer, 0, tempBuffer.length);
                return tempBuffer;
            }
        }

        return null;
    }

    public final void remove()
    {
        throw new UnsupportedOperationException("Read-only iterator.");
    }
    
    /**
     * Descends to a given node, adds its arcs to the stack to
     * be traversed.
     */
    private void pushNode(Node node) {
        nodes.add(node);
        arcs.add(node.getFirstArc());
    }
}