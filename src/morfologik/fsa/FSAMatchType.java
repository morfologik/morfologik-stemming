package morfologik.fsa;

/**
 * Type of the match returned as part of {@link FSAMatch}.
 */
public enum FSAMatchType {
    /**
     * No match for the input sequence of symbols found in the automaton.
     */
    NO_MATCH,

    /**
     * The input sequence ends exactly on the final node.
     */
    EXACT_MATCH,

    /**
     * A terminating node occurs in the dictionary before the end of the input
     * sequence. It effectively means a prefix of the input sequence is stored
     * in the dictionary (e.g., an empty sequence is a prefix of all other
     * sequences). The result {@link FSAMatch} will contain an index of the
     * first character not present in the dictionary.
     */
    PREMATURE_PATH_END_FOUND,

    /**
     * The sequence ends on an intermediate automaton node. The sequence is
     * therefore a prefix of at least one other sequence stored in the
     * dictionary. The result {@link FSAMatch} object will contain an index of
     * the first character in the input sequence not present in the dictionary
     * and a pointer to the {@link FSA}'s <code>node</code> where mismatch
     * occurred.
     */
    PREFIX_FOUND,

    /**
     * The input sequence ends on an intermediate automaton node. This is a
     * special case of {@link #PREFIX_FOUND}. A node where the mismatch (missing
     * input sequence's characters) occurred is returned in the {@link FSAMatch}
     * .
     */
    PREMATURE_WORD_END_FOUND;
}
