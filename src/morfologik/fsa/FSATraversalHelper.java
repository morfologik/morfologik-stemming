
package morfologik.fsa;

import java.util.Iterator;
import static morfologik.fsa.FSAMatchType.*;

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
    public Iterator<byte[]> getAllSubsequences(final FSA.Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null.");
        }

        // Create a custom iterator in the FSA
        return new FSAFinalStatesIterator(node);
    }


    /**
     * Finds a matching path in the dictionary for a given sequence of labels.
     * Several scenarios are possible and are described in 
     * {@link FSAMatchType}.
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
            return new FSAMatch(NO_MATCH);
        }

        for (int i = 0; i < sequence.length; i++) {
            arc = node.getArcLabelledWith(sequence[i]);
            if (arc != null) {
                if (arc.isFinal()) {
                    if (i + 1 == sequence.length) {
                        // the word has been found (exact match).
                        return new FSAMatch(EXACT_MATCH );
                    } else {
                        // a prefix of the word has been found
                        // (there are still characters in the word, but the path is over)
                        return new FSAMatch(PREMATURE_PATH_END_FOUND, i + 1, null);
                    }
                } else {
                    // make a transition along the arc.
                    node = arc.getDestinationNode();
                }
            } else {
                // The label was not found. i.e. there possibly are prefixes
                // of the word in the dictionary, but an exact match does not exist.
                // [an empty string is also considered a prefix!]
                return new FSAMatch(PREFIX_FOUND, i, node);
            }
        }

        // The word is a prefix of some other sequence(s) present in the dictionary.
        return new FSAMatch(PREMATURE_WORD_END_FOUND, 0, node);
    }

}