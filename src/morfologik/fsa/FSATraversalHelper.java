package morfologik.fsa;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import static morfologik.fsa.FSAMatchType.*;

/**
 * This class implements some common matching and scanning operations on a
 * generic FSA.
 */
@Deprecated
public final class FSATraversalHelper {
	/**
	 * Target automaton.
	 */
	private final FSA fsa;

	/**
	 * 
	 */
	public FSATraversalHelper(FSA fsa) {
		this.fsa = fsa;
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
			result.reset(NO_MATCH, start, node);
			return result;
		}

		final int end = start + length;
		for (int i = start; i < end; i++) {
			final int arc = fsa.getArc(node, sequence[i]);
			if (arc != 0) {
				if (fsa.isArcFinal(arc) && i + 1 == end) {
					/* The automaton has an exact match of the input sequence. */
					result.reset(EXACT_MATCH);
					return result;
				}

				if (fsa.isArcTerminal(arc)) {
					/* The automaton contains a prefix of the input sequence. */
					result.reset(AUTOMATON_HAS_PREFIX, i + 1, 0);
					return result;
				}

				// Make a transition along the arc.
				node = fsa.getEndNode(arc);
			} else {
				result.reset(NO_MATCH, i, node);
				return result;
			}
		}

		/* The sequence is a prefix of at least one sequence in the automaton. */
		result.reset(SEQUENCE_IS_A_PREFIX, 0, node);
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
	public FSAMatch matchSequence(byte[] sequence, int start, int length, int node) {
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