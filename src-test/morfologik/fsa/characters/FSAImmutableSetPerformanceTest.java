package morfologik.fsa.characters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import morfologik.util.MinMax;

import org.junit.BeforeClass;
import org.junit.Test;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;

/**
 * 
 */
@BenchmarkOptions(callgc = false, warmupRounds = 10, benchmarkRounds = 50)
public class FSAImmutableSetPerformanceTest extends AbstractBenchmark {
	private static String [] data;
	private static String [] check;

	private static Set<CharSequence> fsaSet;
	private static HashSet<CharSequence> hashSet;

	public static volatile int guard;

	@BeforeClass
	public static void prepare() 
	{
		data = FSABuilderTest.generateRandom(50000,
				new MinMax(1, 20), new MinMax('a', 'z'));

		check = FSABuilderTest.generateRandom(1000000,
				new MinMax(1, 20), new MinMax('a', 'z'));

		fsaSet = FSAImmutableSet.fromUnsorted(data);
		hashSet = new HashSet<CharSequence>(Arrays.asList(data));
	}

	/**
	 * 
	 */
	@Test
	public void testHashSet() {
		int count = 0;
		for (String s : check) {
			count += hashSet.contains(s) ? 1 : 0;
		}
		guard = count;
	}

	/**
	 * 
	 */
	@Test
	public void testFsaSet() {
		int count = 0;
		for (String s : check) {
			count += fsaSet.contains(s) ? 1 : 0;
		}
		guard = count;
	}
}
