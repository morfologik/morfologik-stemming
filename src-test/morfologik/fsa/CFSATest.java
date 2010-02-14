package morfologik.fsa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

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
	final FSA fsa = FSA.getInstance(this.getClass().getResourceAsStream(
		resource), "UTF-8");

	final ByteArrayOutputStream baos = new ByteArrayOutputStream();

	final CFSAEncoder encoder = new CFSAEncoder(fsa);
	encoder.doLabelMapping();
	encoder.updateOffsets();
	encoder.serialize(baos);

	final byte [] input = baos.toByteArray();
	FSA cfsa = FSA.getInstance(new ByteArrayInputStream(input), "UTF-8");

	assertTrue(cfsa instanceof CFSA);
	assertIdentical(fsa, cfsa);
    }

    /**
     * Assert the automatons encode an identical set of paths to terminal
     * states.
     */
    private void assertIdentical(FSA fsa, FSA fsa2) throws IOException {
	FSAFinalStatesIterator fsi1 = fsa.getTraversalHelper()
		.getFinalStatesIterator();
	FSAFinalStatesIterator fsi2 = fsa2.getTraversalHelper()
		.getFinalStatesIterator();

	assertEquals(fsa.getNodeCount(), fsa2.getNodeCount());
	assertEquals(fsa.getArcsCount(), fsa2.getArcsCount());

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
