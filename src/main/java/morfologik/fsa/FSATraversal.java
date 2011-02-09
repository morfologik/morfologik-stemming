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
	 * Calculate perfect hash for a given input sequence of bytes. The perfect hash requires
	 * that {@link FSA} is built with {@link FSAFlags#NUMBERS} and corresponds to the sequential
	 * order of input sequences used at automaton construction time.
	 * 
	 * @param start Start index in the sequence array. 
	 * @param length Length of the byte sequence, must be at least 1.
	 * 
	 * @return Returns a unique integer assigned to the input sequence in the automaton (reflecting
	 * the number of that sequence in the input used to build the automaton). Returns a negative
	 * integer if the input sequence was not part of the input from which the automaton was created.
	 * The type of mismatch is a constant defined in {@link MatchResult}.
	 */
	public int perfectHash(byte[] sequence, int start, int length, int node) {
		assert fsa.getFlags().contains(FSAFlags.NUMBERS) : "FSA not built with NUMBERS option.";
		assert length > 0 : "Must be a non-empty sequence.";

		int hash = 0;
		final int end = start + length - 1;

		int seqIndex = start;
		byte label = sequence[seqIndex];

		// Seek through the current node's labels, looking for 'label', update hash.
		for (int arc = fsa.getFirstArc(node); arc != 0;) {
			if (fsa.getArcLabel(arc) == label) {
				if (fsa.isArcFinal(arc)) {
					if (seqIndex == end)
						return hash;

					hash++;
				}

				if (fsa.isArcTerminal(arc)) {
					/* The automaton contains a prefix of the input sequence. */
					return AUTOMATON_HAS_PREFIX;
				}

				// The sequence is a prefix of one of the sequences stored in the automaton.
				if (seqIndex == end) {
					return SEQUENCE_IS_A_PREFIX;
				}

				// Make a transition along the arc, go the target node's first arc.
				arc = fsa.getFirstArc(fsa.getEndNode(arc));
				label = sequence[++seqIndex];
				continue;
			} else {
				if (fsa.isArcFinal(arc))
					hash++;
				if (!fsa.isArcTerminal(arc))
					hash += fsa.getRightLanguageCount(fsa.getEndNode(arc));
			}

			arc = fsa.getNextArc(arc);
		}

		// Labels of this node ended without a match on the sequence. 
		// Perfect hash does not exist.
		return NO_MATCH;
	}
	
	/**
	 * @see #perfectHash(byte[], int, int, int)  
	 */
	public int perfectHash(byte[] sequence) {
		return perfectHash(sequence, 0, sequence.length, fsa.getRootNode());
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