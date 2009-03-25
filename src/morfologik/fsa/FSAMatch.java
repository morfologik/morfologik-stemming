package morfologik.fsa;

/**
 * A matching result returned from {@link FSATraversalHelper}.
 * 
 * @see FSATraversalHelper
 */
public final class FSAMatch {
    private FSAMatchType matchType;
    private int mismatchAtIndex;
    private int mismatchAtNode;

    /*
     * 
     */
    FSAMatch(FSAMatchType type, int mismatchAtIndex, int mismatchAtNode) {
	reset(type, mismatchAtIndex, mismatchAtNode);
    }

    /*
     * 
     */
    FSAMatch(FSAMatchType type) {
	this(type, 0, 0);
    }

    /*
     * 
     */
    public FSAMatch() {
	this(FSAMatchType.NO_MATCH, 0, 0);
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

    /*
     * 
     */
    void reset(FSAMatchType type, int mismatchAtIndex, int mismatchAtNode) {
	this.matchType = type;
	this.mismatchAtIndex = mismatchAtIndex;
	this.mismatchAtNode = mismatchAtNode;
    }

    /*
     * 
     */
    void reset(FSAMatchType type) {
	this.matchType = type;
	this.mismatchAtIndex = 0;
	this.mismatchAtNode = 0;
    }
}
