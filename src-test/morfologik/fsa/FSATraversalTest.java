package morfologik.fsa;

import morfologik.fsa.*;
import junit.framework.TestCase;

/**
 * Tests {@link FSATraversalHelper}.
 */
public final class FSATraversalTest extends TestCase {
    private FSA dict;

    /**
     * 
     */
    public void setUp() throws Exception {
        dict = FSA.getInstance(
                this.getClass().getResourceAsStream("en_tst.dict"), "iso8859-2");
    }

    /**
     * 
     */
    public void testTraversalWithIterator() {
        final FSATraversalHelper helper = dict.getTraversalHelper();
        final FSAFinalStatesIterator i = helper.getAllSubsequences(dict.getStartNode());
        
        int count = 0;
        while (i.hasNext()) {
            i.nextState();
            count++;
        }
        assertEquals(346773, count);
    }

    /**
     * 
     */
    public void testTraversalWithRecursion() {
        final int [] counter = new int[] {0};

        class Recursion {
            public void dumpNode(final FSA.Node node)
            {
                FSA.Arc arc = node.getFirstArc();
                do {
                    if (arc.isFinal()) {
                        counter[0]++;
                    }
    
                    if (!arc.isTerminal()) {
                        dumpNode(arc.getDestinationNode());
                    }

                    arc = node.getNextArc(arc);
                } while (arc != null);
            }
        }

        new Recursion().dumpNode(dict.getStartNode());

        assertEquals(346773, counter[0]);
    }
}
