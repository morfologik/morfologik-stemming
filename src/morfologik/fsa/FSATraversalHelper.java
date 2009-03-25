package morfologik.fsa;

import java.nio.ByteBuffer;
import java.util.Iterator;
import static morfologik.fsa.FSAMatchType.*;

/**
 * This class implements some common matching and scanning operations on a
 * generic FSA.
 * 
 * <p>
 * Optimized implementations may be provided my specific versions of FSA,
 * therefore objects of this class should be instantiated via
 * {@link FSA#getTraversalHelper()}.
 */
public final class FSATraversalHelper {
    /**
     * Target automaton.
     */
    private final FSA fsa;

    /**
     * Only allow new instances from within the package.
     */
    FSATraversalHelper(FSA fsa) {
	this.fsa = fsa;
    }

    /**
     * Returns an {@link Iterator} of all subsequences available from the given
     * node to all reachable final states.
     */
    public Iterator<ByteBuffer> getAllSubsequences(final int node) {
	if (node == 0) {
	    throw new IllegalArgumentException("Node cannot be zero.");
	}

	// Create a custom iterator in the FSA
	return new FSAFinalStatesIterator(fsa, node);
    }

    /**
     * Returns a new iterator for walking along the final states of this FSA.
     * The iterator is initially set to walk along all final states reachable
     * from the root node.
     */
    public FSAFinalStatesIterator getFinalStatesIterator() {
	return new FSAFinalStatesIterator(fsa, fsa.getRootNode());
    }

    /**
     * Same as {@link #matchSequence(byte[], int, int, int)}, but allows passing
     * a reusable {@link FSAMatch} object so that no intermediate garbage is
     * produced.
     * 
     * @return The same object as <code>result</code>, but with reset internal
     *         type and other fields.
     */
    public FSAMatch matchSequence(FSAMatch result, byte[] sequence, int start,
	    int length, int node) {
	if (node == 0) {
	    result.reset(NO_MATCH);
	    return result;
	}

	final int end = start + length;
	for (int i = start; i < end; i++) {
	    final int arc = fsa.getArc(node, sequence[i]);
	    if (arc != 0) {
		if (fsa.isArcFinal(arc)) {
		    if (i + 1 == end) {
			// The word has been found (exact match).
			result.reset(EXACT_MATCH);
			return result;
		    } else {
			/*
			 * A prefix of the word has been found (there are still
			 * characters in the word, but the path is over)
			 */
			result.reset(PREMATURE_PATH_END_FOUND, i + 1, 0);
			return result;
		    }
		} else {
		    // make a transition along the arc.
		    node = fsa.getEndNode(arc);
		}
	    } else {
		// The label was not found. i.e. there possibly are prefixes
		// of the word in the dictionary, but an exact match does not
		// exist.
		// [an empty string is also considered a prefix!]
		result.reset(PREFIX_FOUND, i, node);
		return result;
	    }
	}

	// The word is a prefix of some other sequence(s) present in the
	// dictionary.
	result.reset(PREMATURE_WORD_END_FOUND, 0, node);
	return result;
    }

    /**
     * Finds a matching path in the dictionary for a given sequence of labels
     * from <code>sequence</code> and starting at node <code>node</code>.
     * 
     * @param sequence
     *            An array of labels to follow in the FSA.
     * @param start
     *            Starting index in <code>sequence</code>.
     * @param length
     *            How many symbols to consider from <code>sequence</code>?
     * @param node
     *            Start node identifier in the FSA.
     * 
     * @see #matchSequence(byte [], int)
     */
    public FSAMatch matchSequence(byte[] sequence, int start, int length,
	    int node) {
	return matchSequence(new FSAMatch(), sequence, start, length, node);
    }

    /**
     * @see #matchSequence(byte[], int, int, int)
     */
    public FSAMatch matchSequence(byte[] sequence, int node) {
	return matchSequence(sequence, 0, sequence.length, node);
    }

    /**
     * @see #matchSequence(byte[], int, int, int)
     */
    public FSAMatch matchSequence(byte[] sequence) {
	return matchSequence(sequence, fsa.getRootNode());
    }
}