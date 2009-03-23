package morfologik.fsa;

/**
 * A result returned from {@link FSATraversalHelper}.
 */
public final class FSAMatch {
    private final FSAMatchType matchType;
    private final int mismatchAtIndex;
    private final FSA.Node mismatchAtNode;

    FSAMatch(FSAMatchType type, int mismatchAtIndex, FSA.Node mismatchAtNode) {
	this.matchType = type;
	this.mismatchAtIndex = mismatchAtIndex;
	this.mismatchAtNode = mismatchAtNode;
    }

    protected FSAMatch(FSAMatchType type) {
	this(type, 0, null);
    }

    public FSAMatchType getMatchType() {
	return matchType;
    }

    public int getMismatchIndex() {
	return mismatchAtIndex;
    }

    public FSA.Node getMismatchNode() {
	return mismatchAtNode;
    }
}
