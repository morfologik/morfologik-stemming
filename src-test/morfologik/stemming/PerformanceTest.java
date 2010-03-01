package morfologik.stemming;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.BeforeClass;
import org.junit.Test;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;

/**
 * Simple performance micro-benchmarks.
 */
@BenchmarkOptions(callgc = false, warmupRounds = 5, benchmarkRounds = 10)
public class PerformanceTest extends AbstractBenchmark {
	/* Guard against escape analysis and HotSpot opts. */
	public volatile int guard;

	/* Test data. */
	static final int sequences = 100000;
	static final String[] testWords = new String[sequences];
	static final PolishStemmer stemmer = new PolishStemmer();

	/**
	 * Prepare test data.
	 */
	@BeforeClass
	public static void prepare() throws UnsupportedEncodingException
	{
		final Dictionary dict = Dictionary.getForLanguage("pl");
		int i = 0;
		for (ByteBuffer sequence : dict.fsa) {
			testWords[i] = new String(sequence.array(), 0,
			        sequence.remaining(), dict.metadata.encoding);
			testWords[i] = testWords[i].substring(0, testWords[i]
			        .indexOf(dict.metadata.separator));
			i++;

			if (i == testWords.length)
				break;
		}
	}
	
	@Test
	public void traversal_100000() throws IOException {
		final Dictionary dict = Dictionary.getForLanguage("pl");

		int max = sequences;
		int guard = 0;
		for (ByteBuffer sequence : dict.fsa) {
			guard += sequence.remaining();
			if (--max == 0)
				break;
		}

		this.guard = guard;
	}

	@Test
	public void stemming_100000() throws IOException {
		int guard = 0;
		for (String word : testWords) {
			for (WordData dta : stemmer.lookup(word))
			{
				guard += dta.getStem().length();
				guard += dta.getTag().length();
			}
		}
		this.guard = guard;
	}
}
