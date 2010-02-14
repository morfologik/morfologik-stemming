package morfologik.fsa;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.junit.Test;

/**
 * Additional tests for {@link FSA5}.
 */
public final class FSA5Test {
    public ArrayList<String> expected = new ArrayList<String>(
	    Arrays.asList("a", "aba", "ac", "b", "ba", "c"));
    public ArrayList<String> actual = new ArrayList<String>();

    @Test
    public void testVersion5() throws IOException {
	final FSA fsa = FSA.getInstance(this.getClass().getResourceAsStream(
		"abc.fsa"), "UTF-8");

	verifyContent(fsa);
	assertTrue(FSAHelpers.flagsToString(fsa.getFlags()).indexOf("NUMBERS") < 0);
    }

    @Test
    public void testVersion5WithNumbers() throws IOException {
	final FSA fsa = FSA.getInstance(this.getClass().getResourceAsStream(
		"abc-numbers.fsa"), "UTF-8");
	verifyContent(fsa);
	assertTrue(FSAHelpers.flagsToString(fsa.getFlags()).indexOf("NUMBERS") >= 0);
    }

    @Test
    public void testArcsAndNodes() throws IOException {
	final FSA fsa1 = FSA.getInstance(this.getClass().getResourceAsStream(
		"abc.fsa"), "UTF-8");
	final FSA fsa2 = FSA.getInstance(this.getClass().getResourceAsStream(
		"abc-numbers.fsa"), "UTF-8");
	assertEquals(fsa1.getArcsCount(), fsa2.getArcsCount());
	assertEquals(fsa1.getNodeCount(), fsa2.getNodeCount());

	assertEquals(4, fsa2.getNodeCount());
	assertEquals(8, fsa2.getArcsCount());
    }

    private void verifyContent(FSA fsa) throws IOException {
        final FSATraversalHelper helper = fsa.getTraversalHelper();
        final Iterator<ByteBuffer> i = helper.getAllSubsequences(fsa.getRootNode());
    
        actual.clear();
        int count = 0;
        while (i.hasNext()) {
            ByteBuffer bb = i.next();
            assertEquals(0, bb.arrayOffset());
            assertEquals(0, bb.position());
            actual.add(new String(bb.array(), 0, bb.remaining(), "UTF-8"));
            count++;
        }
        assertEquals(expected.size(), count);
        Collections.sort(actual);
        assertEquals(expected, actual);
    }
}
