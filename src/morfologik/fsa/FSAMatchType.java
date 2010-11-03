package morfologik.fsa;

/**
 * Type of the match returned as part of {@link FSAMatch}.
 */
public enum FSAMatchType {
	/**
	 * The automaton has no match for the input sequence.
	 */
	NO_MATCH,

	/**
	 * The automaton has exactly one match for the input sequence.
	 */
	EXACT_MATCH,

	/**
	 * The automaton contains a prefix of the input sequence. That is:
	 * one of the input sequences used to build the automaton is a 
	 * prefix of the input sequence that is shorter than the sequence. 
	 * 
	 * <p>{@link FSAMatch#getIndex()} will contain an index of the
	 * first character of the input sequence not present in the 
	 * dictionary.</p>
	 */
	AUTOMATON_HAS_PREFIX,

	/**
	 * The sequence is a prefix of at least one sequence in the automaton. 
	 * {@link FSAMatch#getNode()} returns the node from which all sequences
	 * with the given prefix start in the automaton. 
	 */
	SEQUENCE_IS_A_PREFIX;
}
