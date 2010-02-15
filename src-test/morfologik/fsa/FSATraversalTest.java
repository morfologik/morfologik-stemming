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
	private FSA fsa;

	/**
     * 
     */
	@Before
	public void setUp() throws Exception {
		fsa = FSA.getInstance(this.getClass()
		        .getResourceAsStream("en_tst.dict"), "iso8859-2");
	}

	/**
     * 
     */
	@Test
	public void testTraversalWithIterator() {
		final FSATraversalHelper helper = fsa.getTraversalHelper();
		final Iterator<ByteBuffer> i = helper.getAllSubsequences(fsa
		        .getRootNode());

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
	@Test
	public void testTraversalWithRecursion() {
		final int[] counter = new int[] { 0 };

		class Recursion {
			public void dumpNode(final int node) {
				int arc = fsa.getFirstArc(node);
				do {
					if (fsa.isArcFinal(arc)) {
						counter[0]++;
					}

					if (!fsa.isArcTerminal(arc)) {
						dumpNode(fsa.getEndNode(arc));
					}

					arc = fsa.getNextArc(arc);
				} while (arc != 0);
			}
		}

		new Recursion().dumpNode(fsa.getRootNode());

		assertEquals(346773, counter[0]);
	}
}
