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
     * An exact match found for the input sequence of symbols (the last symbol
     * ends on terminating node). 
     */
    EXACT_MATCH,

    /**
     * Premature path end found (terminating symbol found somewhere along the input
     * sequence).
     */
    PREMATURE_PATH_END_FOUND,
    
    /**
     * TODO: what was this one?
     */
    PREFIX_FOUND,

    /**
     * TODO: what was this one?
     */
    PREMATURE_WORD_END_FOUND;
}
