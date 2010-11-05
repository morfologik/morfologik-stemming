package morfologik.fsa;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;

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
		fsa = FSA.read(this.getClass().getResourceAsStream("en_tst.dict"));
	}

	/**
     * 
     */
	@Test
	public void testTraversalWithIterable() {
		int count = 0;
		for (ByteBuffer bb : fsa.getSequences()) {
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

	/**
     * Test {@link FSATraversalHelper} and matching results.
     */
	@Test
	public void testTraversalHelperMatcher() throws IOException {
		final FSA5 fsa = FSA.read(this.getClass().getResourceAsStream("abc.fsa"));
		final FSATraversalHelper traversalHelper = new FSATraversalHelper(fsa);

		FSAMatch m = traversalHelper.matchSequence("ax".getBytes());
		assertEquals(FSAMatchType.NO_MATCH, m.getMatchType());
		assertEquals(1, m.getIndex());
		assertEquals(new HashSet<String>(Arrays.asList("ba", "c")), 
				suffixes(fsa, m.getNode()));

		assertEquals(FSAMatchType.EXACT_MATCH, 
				traversalHelper.matchSequence("aba".getBytes()).getMatchType());

		m = traversalHelper.matchSequence("abalonger".getBytes());
		assertEquals(FSAMatchType.AUTOMATON_HAS_PREFIX, m.getMatchType());
		assertEquals("longer", "abalonger".substring(m.getIndex()));
		
		m = traversalHelper.matchSequence("ab".getBytes());
		assertEquals(FSAMatchType.SEQUENCE_IS_A_PREFIX, m.getMatchType());
		assertEquals(new HashSet<String>(Arrays.asList("a")), 
				suffixes(fsa, m.getNode()));
	}

	/**
	 * Return all sequences reachable from a given node, as strings.  
	 */
	private HashSet<String> suffixes(FSA fsa, int node) {
		HashSet<String> result = new HashSet<String>(); 
		for (ByteBuffer bb : fsa.getSequences(node))
		{
			try {
	            result.add(new String(bb.array(), bb.position(), bb.remaining(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
	            throw new RuntimeException(e);
            }
		}
	    return result;
    }
}
