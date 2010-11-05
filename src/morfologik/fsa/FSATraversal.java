package morfologik.fsa;

import static morfologik.fsa.MatchResult.*;

/**
 * This class implements some common matching and scanning operations on a
 * generic FSA.
 */
public final class FSATraversal {
	/**
	 * Target automaton.
	 */
	private final FSA fsa;

	/**
	 * Traversals of the given FSA.
	 */
	public FSATraversal(FSA fsa) {
		this.fsa = fsa;
	}

	/**
	 * Same as {@link #match(byte[], int, int, int)}, but allows passing
	 * a reusable {@link MatchResult} object so that no intermediate garbage is
	 * produced.
	 * 
	 * @return The same object as <code>result</code>, but with reset internal
	 *         type and other fields.
	 */
	public MatchResult match(MatchResult result, 
			byte[] sequence, int start, int length, int node)
	{
		if (node == 0) {
			result.reset(NO_MATCH, start, node);
			return result;
		}

		final FSA fsa = this.fsa;
		final int end = start + length;
		for (int i = start; i < end; i++) {
			final int arc = fsa.getArc(node, sequence[i]);
			if (arc != 0) {
				if (fsa.isArcFinal(arc) && i + 1 == end) {
					/* The automaton has an exact match of the input sequence. */
					result.reset(EXACT_MATCH, i, node);
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
	 * @see #match(byte [], int)
	 */
	public MatchResult match(byte[] sequence, int start, int length, int node) {
		return match(new MatchResult(), sequence, start, length, node);
	}

	/**
	 * @see #match(byte[], int, int, int)
	 */
	public MatchResult match(byte[] sequence, int node) {
		return match(sequence, 0, sequence.length, node);
	}

	/**
	 * @see #match(byte[], int, int, int)
	 */
	public MatchResult match(byte[] sequence) {
		return match(sequence, fsa.getRootNode());
	}
}