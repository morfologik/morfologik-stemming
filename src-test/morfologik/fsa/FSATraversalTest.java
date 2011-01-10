package morfologik.fsa;

import static org.junit.Assert.*;
import static morfologik.fsa.MatchResult.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;


import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link FSATraversal}.
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
	public void testPerfectHash() throws IOException {
		byte[][] input = new byte[][] {
				{ 'a' },
				{ 'a', 'b', 'a' },
				{ 'a', 'c' },
		        { 'b' }, 
		        { 'b', 'a' },
		        { 'c' },
		};

		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);
		FSA s = FSABuilder.build(input);

		final byte[] fsaData = 
				new FSA5Serializer()
				.withNumbers()
				.serialize(s, new ByteArrayOutputStream())
				.toByteArray();

		final FSA5 fsa = (FSA5) FSA.read(new ByteArrayInputStream(fsaData));
		final FSATraversal traversal = new FSATraversal(fsa);

		int i = 0;
		for (byte [] seq : input)
		{
			assertEquals(new String(seq), i++, traversal.perfectHash(seq));
		}

		// Check if the total number of sequences is encoded at the root node.
		assertEquals(6, fsa.getRightLanguageCount(fsa.getRootNode()));

		// Check sub/super sequence scenarios.
		assertEquals(AUTOMATON_HAS_PREFIX, traversal.perfectHash("abax".getBytes("UTF-8")));
		assertEquals(SEQUENCE_IS_A_PREFIX, traversal.perfectHash("ab".getBytes("UTF-8")));
		assertEquals(NO_MATCH, traversal.perfectHash("d".getBytes("UTF-8")));
		assertEquals(NO_MATCH, traversal.perfectHash(new byte [] {0}));
		
		assertTrue(AUTOMATON_HAS_PREFIX < 0);
		assertTrue(SEQUENCE_IS_A_PREFIX < 0);
		assertTrue(NO_MATCH < 0);
	}

	/**
     * 
     */
	@Test
	public void testRecursiveTraversal() {
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
     * Test {@link FSATraversal} and matching results.
     */
	@Test
	public void testMatch() throws IOException {
		final FSA5 fsa = FSA.read(this.getClass().getResourceAsStream("abc.fsa"));
		final FSATraversal traversalHelper = new FSATraversal(fsa);

		MatchResult m = traversalHelper.match("ax".getBytes());
		assertEquals(NO_MATCH, m.kind);
		assertEquals(1, m.index);
		assertEquals(new HashSet<String>(Arrays.asList("ba", "c")), 
				suffixes(fsa, m.node));

		assertEquals(EXACT_MATCH, 
				traversalHelper.match("aba".getBytes()).kind);

		m = traversalHelper.match("abalonger".getBytes());
		assertEquals(AUTOMATON_HAS_PREFIX, m.kind);
		assertEquals("longer", "abalonger".substring(m.index));
		
		m = traversalHelper.match("ab".getBytes());
		assertEquals(SEQUENCE_IS_A_PREFIX, m.kind);
		assertEquals(new HashSet<String>(Arrays.asList("a")), 
				suffixes(fsa, m.node));
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
