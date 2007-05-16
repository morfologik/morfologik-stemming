package morfologik.fsa.core;

/**
 * A result returned from 
 * {@link FSATraversalHelper#matchSequence(byte[], morfologik.fsa.core.FSA.Node)}.
 */
public final class FSAMatch
{
    public static final int NO_MATCH = 0;
    public static final int EXACT_MATCH = 1;
    public static final int PREMATURE_PATH_END_FOUND = 2;
    public static final int PREFIX_FOUND = 3;
    public static final int PREMATURE_WORD_END_FOUND = 4;

    private int matchResult;
    private int mismatchAtIndex;
    private FSA.Node mismatchAtNode;

    protected FSAMatch(int result, int mismatchAtIndex, FSA.Node mismatchAtNode) {
        this.matchResult = result;
        this.mismatchAtIndex = mismatchAtIndex;
        this.mismatchAtNode = mismatchAtNode;
    }

    protected FSAMatch(final int result) {
        this.matchResult = result;
    }

    public int getMatchResult() { 
        return matchResult; 
    }

    public int getMismatchIndex() { 
        return mismatchAtIndex; 
    }

    public FSA.Node getMismatchNode() {
        return mismatchAtNode;
    }
}
