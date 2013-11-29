package morfologik.fsa;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/**
 * This is a top abstract class for handling finite state automata. These
 * automata are arc-based, a design described in Jan Daciuk's <i>Incremental
 * Construction of Finite-State Automata and Transducers, and Their Use in the
 * Natural Language Processing</i> (PhD thesis, Technical University of Gdansk).
 * 
 * <p>
 * Concrete subclasses (implementations) provide varying tradeoffs and features:
 * traversal speed vs. memory size, for example.
 * </p>
 * 
 * @see FSABuilder
 */
public abstract class FSA implements Iterable<ByteBuffer> {
    /**
     * @return Returns the identifier of the root node of this automaton.
     *         Returns 0 if the start node is also the end node (the automaton
     *         is empty).
     */
    public abstract int getRootNode();

    /**
     * @return Returns the identifier of the first arc leaving <code>node</code>
     *         or 0 if the node has no outgoing arcs.
     */
    public abstract int getFirstArc(int node);

    /**
     * @return Returns the identifier of the next arc after <code>arc</code> and
     *         leaving <code>node</code>. Zero is returned if no more arcs are
     *         available for the node.
     */
    public abstract int getNextArc(int arc);

    /**
     * @return Returns the identifier of an arc leaving <code>node</code> and
     *         labeled with <code>label</code>. An identifier equal to 0 means
     *         the node has no outgoing arc labeled <code>label</code>.
     */
    public abstract int getArc(int node, byte label);

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
     * terminating node (@link {@link #getEndNode(int)} will throw an
     * exception). Implies {@link #isArcFinal(int)}.
     */
    public abstract boolean isArcTerminal(int arc);

    /**
     * Return the end node pointed to by a given <code>arc</code>. Terminal arcs
     * (those that point to a terminal state) have no end node representation
     * and throw a runtime exception.
     */
    public abstract int getEndNode(int arc);

    /**
     * Returns a set of flags for this FSA instance.
     */
    public abstract Set<FSAFlags> getFlags();

    /**
     * Calculates the number of arcs of a given node. Unless really required,
     * use the following idiom for looping through all arcs:
     * <pre>
     * for (int arc = fsa.getFirstArc(node); arc != 0; arc = fsa.getNextArc(arc)) {
     * }
     * </pre>
     */
    public int getArcCount(int node) {
        int count = 0;
        for (int arc = getFirstArc(node); arc != 0; arc = getNextArc(arc)) {
            count++;
        }
        return count;
    }

    /**
     * @return Returns the number of sequences reachable from the given state if
     * the automaton was compiled with {@link FSAFlags#NUMBERS}. The size of
     * the right language of the state, in other words.
     * 
     * @throws UnsupportedOperationException If the automaton was not compiled with
     * {@link FSAFlags#NUMBERS}. The value can then be computed by manual count
     * of {@link #getSequences(int)}.
     */
    public int getRightLanguageCount(int node) {
        throw new UnsupportedOperationException("Automaton not compiled with " + FSAFlags.NUMBERS);
    }

    /**
     * Returns an iterator over all binary sequences starting at the given FSA
     * state (node) and ending in final nodes. This corresponds to a set of
     * suffixes of a given prefix from all sequences stored in the automaton.
     * 
     * <p>
     * The returned iterator is a {@link ByteBuffer} whose contents changes on
     * each call to {@link Iterator#next()}. The keep the contents between calls
     * to {@link Iterator#next()}, one must copy the buffer to some other
     * location.
     * </p>
     * 
     * <p>
     * <b>Important.</b> It is guaranteed that the returned byte buffer is
     * backed by a byte array and that the content of the byte buffer starts at
     * the array's index 0.
     * </p>
     * 
     * @see Iterable
     */
    public Iterable<ByteBuffer> getSequences(final int node) {
        if (node == 0) {
            return Collections.<ByteBuffer> emptyList();
        }

        return new Iterable<ByteBuffer>() {
            public Iterator<ByteBuffer> iterator() {
                return new FSAFinalStatesIterator(FSA.this, node);
            }
        };
    }

    /**
     * An alias of calling {@link #iterator} directly ({@link FSA} is also
     * {@link Iterable}).
     */
    public final Iterable<ByteBuffer> getSequences() {
        return getSequences(getRootNode());
    }

    /**
     * Returns an iterator over all binary sequences starting from the initial
     * FSA state (node) and ending in final nodes. The returned iterator is a
     * {@link ByteBuffer} whose contents changes on each call to
     * {@link Iterator#next()}. The keep the contents between calls to
     * {@link Iterator#next()}, one must copy the buffer to some other location.
     * 
     * <p>
     * <b>Important.</b> It is guaranteed that the returned byte buffer is
     * backed by a byte array and that the content of the byte buffer starts at
     * the array's index 0.
     * </p>
     * 
     * @see Iterable
     */
    public final Iterator<ByteBuffer> iterator() {
        return getSequences().iterator();
    }

    /**
     * Visit all states. The order of visiting is undefined. This method may be faster
     * than traversing the automaton in post or preorder since it can scan states 
     * linearly. Returning false from {@link StateVisitor#accept(int)}
     * immediately terminates the traversal.
     */
    public <T extends StateVisitor> T visitAllStates(T v) {
        return visitInPostOrder(v);
    }

    /**
     * Same as {@link #visitInPostOrder(StateVisitor, int)}, 
     * starting from root automaton node.
     */
    public <T extends StateVisitor> T visitInPostOrder(T v) {
        return visitInPostOrder(v, getRootNode());
    }

    /**
     * Visits all states reachable from <code>node</code> in postorder. 
     * Returning false from {@link StateVisitor#accept(int)}
     * immediately terminates the traversal.
     */
    public <T extends StateVisitor> T visitInPostOrder(T v, int node) {
        visitInPostOrder(v, node, new BitSet());
        return v;
    }

    /** Private recursion. */
    private boolean visitInPostOrder(StateVisitor v, int node, BitSet visited) {
        if (visited.get(node))
            return true;
        visited.set(node);

        for (int arc = getFirstArc(node); arc != 0; arc = getNextArc(arc)) {
            if (!isArcTerminal(arc)) {
                if (!visitInPostOrder(v, getEndNode(arc), visited))
                    return false;
            }
        }

        return v.accept(node);
    }

    /**
     * Same as {@link #visitInPreOrder(StateVisitor, int)}, starting from root automaton node.
     */
    public <T extends StateVisitor> T visitInPreOrder(T v) {
        return visitInPreOrder(v, getRootNode());
    }

    /**
     * Visits all states in preorder. Returning false from {@link StateVisitor#accept(int)}
     * skips traversal of all sub-states of a given state.
     */
    public <T extends StateVisitor> T visitInPreOrder(T v, int node) {
        visitInPreOrder(v, node, new BitSet());
        return v;
    }

    /** Private recursion. */
    private void visitInPreOrder(StateVisitor v, int node, BitSet visited) {
        if (visited.get(node))
            return;
        visited.set(node);

        if (v.accept(node)) {
            for (int arc = getFirstArc(node); arc != 0; arc = getNextArc(arc)) {
                if (!isArcTerminal(arc)) {
                    visitInPreOrder(v, getEndNode(arc), visited);
                }
            }
        }
    }

    /**
     * A factory for reading automata in any of the supported versions. If
     * possible, explicit constructors should be used.
     * 
     * @see FSA5#FSA5(InputStream)
     */
    @SuppressWarnings("unchecked")
    public static <T extends FSA> T read(InputStream in) throws IOException {
        if (!in.markSupported()) {
            in = new BufferedInputStream(in, Math.max(FSAHeader.MAX_HEADER_LENGTH + 1, 1024));
        }

        in.mark(FSAHeader.MAX_HEADER_LENGTH);
        FSAHeader header = FSAHeader.read(in);
        in.reset();

        if (header.version == FSA5.VERSION)
            return (T) new FSA5(in);
        
        if (header.version == CFSA.VERSION)
            return (T) new CFSA(in);

        if (header.version == CFSA2.VERSION)
            return (T) new CFSA2(in);

        throw new IOException("Unsupported automaton version: "
                + header.version);
    }

    public static FSA read(File fsa) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(fsa));
        try {
            return read(is);
        } finally {
            is.close();
        }
    }
}
