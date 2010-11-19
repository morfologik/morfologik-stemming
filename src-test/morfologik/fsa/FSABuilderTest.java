package morfologik.fsa;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import morfologik.stemming.Dictionary;
import morfologik.util.BufferUtils;
import morfologik.util.MinMax;

import org.junit.*;

import static org.junit.Assert.*;

public class FSABuilderTest {
	private static byte[][] input;
	private static byte[][] input2;

	@BeforeClass
	public static void prepareByteInput() {
		input = generateRandom(25000, new MinMax(1, 20), new MinMax(0, 255));
		input2 = generateRandom(25000, new MinMax(1, 20), new MinMax(0, 3));
	}

	/**
     * 
     */
    @Test
    public void testNormalInput() throws Exception {
        // Extract some real-life input from the built-in dictionary. 
        FSA plDict = Dictionary.getForLanguage("pl").fsa;
        byte [][] input = new byte [100000][];
        int i = 0;
        for (ByteBuffer bb : plDict) {
            input[i++] = Arrays.copyOfRange(bb.array(), 0, bb.remaining());
            if (i == input.length) break;
        }

        // Sort.
        Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);
        State s = FSABuilder.build(input);
        checkCorrect(input, s);
    }

	/**
	 * 
	 */
	@Test
	public void testEmptyInput() {
		byte[][] input = {};
		State s = FSABuilder.build(input);
		checkCorrect(input, s);
	}

	/**
     * Verify absolute byte-value ordering in the comparators and serialized automaton.
     */
    @Test
    public void testLexicographicOrder() throws IOException {
        byte[][] input = { 
                {0},
                {1},
                {(byte) 0xff},
        };
        Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);

        // Check if lexical ordering is consistent with absolute byte value.
        assertEquals(0, input[0][0]);
        assertEquals(1, input[1][0]);
        assertEquals((byte) 0xff, input[2][0]);

        State s = FSABuilder.build(input);
        checkCorrect(input, s);
        
        // Check if arcs are ordered after serialization.
        FSA5 fsa = FSA.read(new ByteArrayInputStream(
                new FSA5Serializer().serialize(s, new ByteArrayOutputStream()).toByteArray()));

        int arc = fsa.getFirstArc(fsa.getRootNode());
        assertEquals(0, fsa.getArcLabel(arc));
        arc = fsa.getNextArc(arc);
        assertEquals(1, fsa.getArcLabel(arc));
        arc = fsa.getNextArc(arc);
        assertEquals((byte) 0xff, fsa.getArcLabel(arc));
    }

	/**
	 * 
	 */
	@Test
	public void testRandom25000_largerAlphabet() {
		State s = FSABuilder.build(input);
		checkCorrect(input, s);
		checkMinimal(s);
	}

	/**
	 * 
	 */
	@Test
	public void testRandom25000_smallAlphabet() {
		State s = FSABuilder.build(input2);
		checkCorrect(input2, s);
		checkMinimal(s);
	}

	/**
	 * Generate a sorted list of random sequences.
	 */
	static byte[][] generateRandom(int count, MinMax length,
	        MinMax alphabet) {
		final byte[][] input = new byte[count][];
		final Random rnd = new Random(0x11223344);
		for (int i = 0; i < count; i++) {
			input[i] = randomByteSequence(rnd, length, alphabet);
		}
		Arrays.sort(input, FSABuilder.LEXICAL_ORDERING);
		return input;
	}

	/**
	 * Generate a random string.
	 */
	private static byte[] randomByteSequence(Random rnd, MinMax length,
	        MinMax alphabet) {
		byte[] bytes = new byte[length.min + rnd.nextInt(length.range())];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) (alphabet.min + rnd.nextInt(alphabet.range()));
		}
		return bytes;
	}

	/**
	 * Check if the DFSA is correct with respect to the given input.
	 */
	protected static void checkCorrect(byte[][] input, State s) {
		// (1) All input sequences are in the right language.
		HashSet<ByteBuffer> rl = new HashSet<ByteBuffer>();
		for (byte[] sequence : StateUtils.rightLanguage(s)) {
			rl.add(ByteBuffer.wrap(sequence));
		}

		HashSet<ByteBuffer> uniqueInput = new HashSet<ByteBuffer>();
		for (byte[] sequence : input) {
			uniqueInput.add(ByteBuffer.wrap(sequence));
		}

		for (ByteBuffer sequence : uniqueInput) {
			Assert.assertTrue("Not present in the right language: "
			        + BufferUtils.toString(sequence), rl.remove(sequence));
		}

		// (2) No other sequence _other_ than the input is in the right
		// language.
		Assert.assertEquals(0, rl.size());
	}

	/**
	 * Check if the DFSA reachable from a given state is minimal. This means no
	 * two states have the same right language.
	 */
	protected static void checkMinimal(State s) {
		final HashSet<String> stateLanguages = new HashSet<String>();
		s.postOrder(new Visitor<State>() {
			private StringBuilder b = new StringBuilder();

			public boolean accept(State s) {
				List<byte[]> rightLanguage = StateUtils.rightLanguage(s);
				Collections.sort(rightLanguage, FSABuilder.LEXICAL_ORDERING);

				b.setLength(0);
				for (byte[] seq : rightLanguage) {
					b.append(Arrays.toString(seq));
					b.append(',');
				}

				String full = b.toString();
				Assert.assertFalse(stateLanguages.contains(full));
				stateLanguages.add(full);
				
				return true;
			}
		});
	}
}
