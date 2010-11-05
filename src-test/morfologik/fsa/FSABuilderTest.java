package morfologik.fsa;

import java.nio.ByteBuffer;
import java.util.*;

import morfologik.util.BufferUtils;
import morfologik.util.MinMax;

import org.junit.*;

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
	public void testEmptyInput() {
		byte[][] input = {};
		State s = FSABuilder.build(input);
		checkCorrect(input, s);
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

			public void accept(State s) {
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
			}
		});
	}
}
