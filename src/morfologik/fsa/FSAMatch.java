package morfologik.fsa;

/**
 * A matching result returned from {@link FSATraversalHelper}.
 * 
 * @see FSATraversalHelper
 */
public final class FSAMatch {
	private FSAMatchType matchType;
	private int index;
	private int node;

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
	 * Return the index whose interpretation depends on the {@link #getMatchType()}.
	 * 
	 * @see FSAMatchType
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Return the node whose interpretation depends on the {@link #getMatchType()}.
	 * 
	 * @see FSAMatchType
	 */
	public int getNode() {
		return node;
	}

	/*
     * 
     */
	void reset(FSAMatchType type, int mismatchAtIndex, int mismatchAtNode) {
		this.matchType = type;
		this.index = mismatchAtIndex;
		this.node = mismatchAtNode;
	}

	/*
     * 
     */
	void reset(FSAMatchType type) {
		this.matchType = type;
		this.index = 0;
		this.node = 0;
	}
}
