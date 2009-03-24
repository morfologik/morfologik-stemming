package morfologik.fsa;

/**
 * A result returned from {@link FSATraversalHelper}.
 */
public final class FSAMatch {
    private final FSAMatchType matchType;
    private final int mismatchAtIndex;
    private final int mismatchAtNode;

    FSAMatch(FSAMatchType type, int mismatchAtIndex, int mismatchAtNode) {
	this.matchType = type;
	this.mismatchAtIndex = mismatchAtIndex;
	this.mismatchAtNode = mismatchAtNode;
    }

    protected FSAMatch(FSAMatchType type) {
	this(type, 0, 0);
    }

    /**
     * Return match type.
     */
    public FSAMatchType getMatchType() {
	return matchType;
    }

    /** 
     * Return the index at which a mismatch occurred.
     * 
     * @see FSAMatchType
     */
    public int getMismatchIndex() {
	return mismatchAtIndex;
    }

    /**
     * Return the node at which mismatch occurred.
     * 
     * @see FSAMatchType
     */
    public int getMismatchNode() {
	return mismatchAtNode;
    }
}
