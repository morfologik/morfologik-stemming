package morfologik.fsa.characters;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

import morfologik.fsa.Visitor;
import morfologik.fsa.characters.FSABuilder;
import morfologik.fsa.characters.State;
import morfologik.fsa.characters.StateUtils;
import morfologik.util.MinMax;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class FSABuilderTest {

	private static String [] input;
	private static String [] input2;

	@BeforeClass
	public static void prepare() 
	{
		input = generateRandom(25000, 
				new MinMax(1, 20), new MinMax('a', 'z'));

		input2 = generateRandom(25000, 
				new MinMax(1, 20), new MinMax('a', 'c'));
	}

	/**
	 * 
	 */
	@Test
	public void testArtFartFlirtStart() {
		String[] input = { "art", "fart", "flirt", "start" };

		Arrays.sort(input);
		State s = FSABuilder.build(input);
		checkCorrect(Arrays.asList(input), s);
	}

	/**
	 * 
	 */
	@Test
	public void testA_X() {
		String[] input = { 
				"abx", "acx", "adx", "aex", "afx", "agx", "ahx",
				"aix", "ajx", };

		Arrays.sort(input);
		State s = FSABuilder.build(input);
		checkCorrect(Arrays.asList(input), s);
	}
	
	/**
	 * 
	 */
	@Test
	public void testInternalFinalState() {
		String[] input = { "ab", "abc" };

		Arrays.sort(input);
		State s = FSABuilder.build(input);
		checkCorrect(Arrays.asList(input), s);
	}
	
	/**
	 * 
	 */
	@Test
	public void testEmptyInput() {
		String[] input = {};

		Arrays.sort(input);
		State s = FSABuilder.build(input);
		checkCorrect(Arrays.asList(input), s);
	}

	/**
	 * 
	 */
	@Test
	public void testRandom75() {
		String[] input = generateRandom(75, 
				new MinMax(2, 10), new MinMax('a', 'z'));

		Arrays.sort(input);
		State s = FSABuilder.build(input);
		checkCorrect(Arrays.asList(input), s);
		checkMinimal(s);
	}

	/**
	 * 
	 */
	@Test
	public void testRandom25000_largerAlphabet() {
		State s = FSABuilder.build(input);
		checkCorrect(Arrays.asList(input), s);
		checkMinimal(s);
	}

	/**
	 * 
	 */
	@Test
	public void testRandom25000_smallAlphabet() {
		State s = FSABuilder.build(input2);
		checkCorrect(Arrays.asList(input2), s);
		checkMinimal(s);
	}

	/**
	 * Generate a sorted list of random sequences.
	 */
	private static String[] generateRandom(int count, MinMax length, MinMax alphabet) {
		final String[] input = new String[count];
		final Random rnd = new Random(0x11223344);
		for (int i = 0; i < count; i++) {
			input[i] = randomString(rnd, length, alphabet);
		}
		Arrays.sort(input);
		return input;
	}

	/**
	 * Generate a random string.
	 */
	private static String randomString(Random rnd, MinMax length, MinMax alphabet) {
		char [] chars = new char [length.min + rnd.nextInt(length.range())];
		for (int i = 0; i < chars.length; i++)
		{
			chars[i] = (char) (alphabet.min + rnd.nextInt(alphabet.range()));
		}
		return new String(chars);
	}

	/**
	 * Check if the DFSA is correct with respect to the given input.
	 */
	protected void checkCorrect(Collection<String> input, State s) {
		// (1) All input sequences are in the right language.
		HashSet<String> rl = new HashSet<String>(StateUtils.rightLanguage(s));
		HashSet<String> uniqueInput = new HashSet<String>(input);
		for (String sequence : uniqueInput) {
			Assert.assertTrue("Not present in the right language: " + sequence,
					rl.remove(sequence));
		}

		// (2) No other sequence _other_ than the input is in the right
		// language.
		Assert.assertEquals(0, rl.size());
	}

	/**
	 * Check if the DFSA reachable from a given state is minimal. This means
	 * no two states have the same right language.
	 */
	protected void checkMinimal(State s) {
		final HashSet<String> stateLanguages = new HashSet<String>();
		s.postOrder(new Visitor<State>() {
			public void accept(State s) {
				Object [] rl = StateUtils.rightLanguage(s).toArray();
				Arrays.sort(rl);
				String full = Arrays.toString(rl);
				Assert.assertFalse(stateLanguages.contains(full));
				stateLanguages.add(full);
			}
		});
	}
}
