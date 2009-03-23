
package morfologik.fsa;

import java.util.Iterator;

/**
 * This class implements some common match/ traverse/ find operations on a generic FSA.
 * 
 * <p>Optimized implementations may be provided my specific versions of FSA, therefore objects
 * of this class should be instantiated via {@link FSA#getTraversalHelper()}.
 */
public class FSATraversalHelper {
    /** 
     * Only allow new instances from within the package. 
     */
    FSATraversalHelper() {
        // empty
    }

    /**
     * Returns an {@link Iterator} of all subsequences available from the given node to
     * all reachable final states.
     */
    public FSAFinalStatesIterator getAllSubsequences(final FSA.Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null.");
        }

        // Create a custom iterator in the FSA
        return new FSAFinalStatesIterator(node);
    }


    /**
     * Finds a matching path in the dictionary for a given sequence of labels.
     * Several return values are possible:
     * <ol>
     *      <li>{@link FSAMatch#EXACT_MATCH} - The sequence ends exactly on the final node. A
     *          match has been found.
     *
     *      <li>{@link FSAMatch#PREFIX_FOUND} - The sequence ends on an intermediate
     *          automaton node. The sequence is therefore
     *          a prefix of at least one other sequence stored in the dictionary. The result
     *          Match will contain an index of the first character in the input sequence not present
     *          in the dictionary and a pointer to the FSA.Node where mismatch occurred.
     *
     *      <li>{@link FSAMatch#PREMATURE_PATH_END_FOUND} - Dictionary's path ends before the sequence.
     *          It means a prefix of the input sequence
     *          is stored in the dictionary. (i.e. an empty sequence is a prefix of all other
     *          sequences). The result Match will contain an index of the first character
     *          not present in the dictionary.
     *
     *      <li>{@link FSAMatch#PREMATURE_WORD_END_FOUND} - The input sequence ends on an intermediate 
     *          automaton node. This is a special case of {@link FSAMatch#PREFIX_FOUND}. A Node where the mismatch 
     *          (lack of input sequence's characters) occurred is returned in Match.
     * </ol>
     */
    public FSAMatch matchSequence(byte [] sequence, FSA fsa) {
        return matchSequence(sequence, fsa.getStartNode());
    }

    /**
     * Finds a matching path in the dictionary for a given sequence of labels,
     * starting from some internal dictionary's node.
     *
     * @see #matchSequence(byte [], FSA.Node)
     */
    public FSAMatch matchSequence(byte [] sequence, FSA.Node node) {
        FSA.Arc arc;

        if (node == null) {
            return new FSAMatch(FSAMatch.NO_MATCH);
        }

        for (int i = 0; i < sequence.length; i++) {
            arc = node.getArcLabelledWith(sequence[i]);
            if (arc != null) {
                if (arc.isFinal()) {
                    if (i + 1 == sequence.length) {
                        // the word has been found (exact match).
                        return new FSAMatch(FSAMatch.EXACT_MATCH );
                    } else {
                        // a prefix of the word has been found
                        // (there are still characters in the word, but the path is over)
                        return new FSAMatch(FSAMatch.PREMATURE_PATH_END_FOUND, i + 1, null);
                    }
                } else {
                    // make a transition along the arc.
                    node = arc.getDestinationNode();
                }
            } else {
                // the label was not found. i.e. there possibly are prefixes
                // of the word in the dictionary, but an exact match doesn't exist.
                // [an empty string is also considered a prefix!]
                return new FSAMatch(FSAMatch.PREFIX_FOUND, i, node);
            }
        }

        // the word is a prefix of some other sequence(s) present in the dictionary.
        return new FSAMatch(FSAMatch.PREMATURE_WORD_END_FOUND, 0, node);
    }

}