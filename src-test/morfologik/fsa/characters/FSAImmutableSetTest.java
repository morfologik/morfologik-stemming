package morfologik.fsa.characters;

import java.util.Arrays;
import java.util.Set;

import morfologik.util.MinMax;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class FSAImmutableSetTest {
	private static String [] large;

	@BeforeClass
	public static void prepare() 
	{
		large = FSABuilderTest.generateRandom(25000, 
				new MinMax(1, 20), new MinMax('a', 'z'));
	}

	/**
	 * 
	 */
	@Test
	public void testSimple() {
		String [] input = {"art", "fart", "flirt", "start"};
		Set<CharSequence> set = FSAImmutableSet.fromUnsorted(input);
		for (String s : input) {
			Assert.assertTrue(s, set.contains(s));
		}
		Assert.assertTrue(set.containsAll(Arrays.asList(input)));
		Assert.assertFalse(set.contains("ar"));
		Assert.assertFalse(set.contains("arta"));
		Assert.assertFalse(set.contains("flirta"));
	}
	
	/**
	 * 
	 */
	@Test
	public void testEmpty() {
		Set<CharSequence> set = FSAImmutableSet.fromUnsorted();
		Assert.assertFalse(set.contains("ar"));
		Assert.assertFalse(set.contains("arta"));
		Assert.assertFalse(set.contains("flirta"));
	}
	
	/**
	 * 
	 */
	@Test
	public void testGenerated() {
		Set<CharSequence> set = FSAImmutableSet.fromUnsorted(large);
		for (String s : large) {
			Assert.assertTrue(s, set.contains(s));
		}
		Assert.assertTrue(set.containsAll(Arrays.asList(large)));
	}
}
