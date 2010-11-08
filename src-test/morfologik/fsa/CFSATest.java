package morfologik.fsa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import morfologik.util.Arrays;

import org.junit.Test;

/**
 * Test {@link CFSA} encoder and representation.
 */
public class CFSATest {
	@Test
	public void testIdentityLarge() throws IOException {
		checkIdentity("en_tst.dict");
	}

	@Test
	public void testIdentitySmall() throws IOException {
		checkIdentity("abc.fsa");
	}

	@Test
	public void testIdentityWithNumbers() throws IOException {
		checkIdentity("abc-numbers.fsa");
	}

	private void checkIdentity(String resource) throws IOException {
		final FSA fsa = FSA.read(this.getClass().getResourceAsStream(resource));

		ArrayList<byte[]> input = new ArrayList<byte[]>(); 
		for (ByteBuffer bb : fsa) {
		    input.add(Arrays.copyOf(bb.array(), bb.remaining()));
		}
		Collections.sort(input, FSABuilder.LEXICAL_ORDERING);

		// Rebuild the automaton to make sure we keep the same order of arcs.
		State root = FSABuilder.build(input);
		
		// Serializers
		FSA5Serializer ser1 = new FSA5Serializer();
		CFSASerializer ser2 = new CFSASerializer();

		if (fsa.getFlags().contains(FSAFlags.NUMBERS)) {
		    ser1.withNumbers();
		    ser2.withNumbers();
		}

		FSA fsa5 = FSA.read(new ByteArrayInputStream(
		        ser1.serialize(root, new ByteArrayOutputStream()).toByteArray()));
        FSA cfsa = FSA.read(new ByteArrayInputStream(
                ser2.serialize(root, new ByteArrayOutputStream()).toByteArray()));

		assertTrue(cfsa instanceof CFSA);
		assertIdentical(fsa5, cfsa);
	}

	/**
	 * Assert the automatons encode an identical set of paths to terminal
	 * states.
	 */
	private void assertIdentical(FSA fsa1, FSA fsa2) throws IOException {
		FSAInfo info1 = new FSAInfo(fsa1);
		FSAInfo info2 = new FSAInfo(fsa2);

		assertEquals(info1.nodeCount, info2.nodeCount);
		assertEquals(info1.arcsCount, info2.arcsCount);

		Iterator<ByteBuffer> fsi1 = fsa1.getSequences().iterator();
		Iterator<ByteBuffer> fsi2 = fsa2.getSequences().iterator();

		while (true) {
			assertEquals(fsi1.hasNext(), fsi2.hasNext());
			if (!fsi1.hasNext())
				break;

			ByteBuffer bb1 = fsi1.next();
			ByteBuffer bb2 = fsi2.next();

			assertEquals(0, bb1.arrayOffset());
			assertEquals(0, bb1.position());
			assertEquals(0, bb2.arrayOffset());
			assertEquals(0, bb2.position());

			assertEquals(bb1.remaining(), bb2.remaining());

			final byte[] bb1a = bb1.array();
			final byte[] bb2a = bb2.array();
			for (int i = 0; i < bb1.remaining(); i++)
				assertEquals(bb1a[i], bb2a[i]);
		}
	}
}
