package morfologik.fsa;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link FSATraversalHelper}.
 */
public final class FSATraversalTest {
    private FSA dict;

    /**
     * 
     */
    @Before
    public void setUp() throws Exception {
	dict = FSA.getInstance(this.getClass().getResourceAsStream(
		"en_tst.dict"), "iso8859-2");
    }

    /**
     * 
     */
    @Test
    public void testTraversalWithIterator() {
	final FSATraversalHelper helper = dict.getTraversalHelper();
	final Iterator<ByteBuffer> i = helper.getAllSubsequences(dict.getStartNode());

	int count = 0;
	while (i.hasNext()) {
	    ByteBuffer bb = i.next();
	    assertEquals(0, bb.arrayOffset());
	    assertEquals(0, bb.position());
	    count++;
	}
	assertEquals(346773, count);
    }

    /**
     * 
     */
    public void testTraversalWithRecursion() {
	final int[] counter = new int[] { 0 };

	class Recursion {
	    public void dumpNode(final FSA.Node node) {
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
