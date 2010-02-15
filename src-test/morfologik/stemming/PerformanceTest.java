package morfologik.stemming;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.concurrent.Callable;

import morfologik.util.PerformanceTimer;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Simple performance testing.
 */
public class PerformanceTest {
	@Test
	public void fsaTraversal() throws IOException {
		final Dictionary dict = Dictionary.getForLanguage("pl");

		final int warmup = 5;
		final int rounds = 20;
		final int sequences = 100000;

		@SuppressWarnings("unused")
		final PerformanceTimer t = PerformanceTimer.run(new Callable<Void>() {
			public Void call() throws Exception {
				int i = sequences;
				for (ByteBuffer sequence : dict.fsa) {
					if (i-- < 0)
						break;
				}
				return null;
			}
		}, warmup, rounds);

		System.out.println("FSA traversal -> " + t + ", Sequences/sec.: "
		        + t.elemsPerSecond(sequences));
	}

	@Test
	public void stemmingPerformance() throws IOException {
		final PolishStemmer stemmer = new PolishStemmer();
		final Dictionary dict = Dictionary.getForLanguage("pl");

		/*
		 * Collect the sequences that will be used for testing.
		 */

		final int warmup = 5;
		final int rounds = 20;
		final int sequences = 100000;

		final String[] testWords = new String[sequences];
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

		/*
		 * Run the test.
		 */
		final PerformanceTimer t = PerformanceTimer.run(new Callable<Void>() {
			public Void call() throws Exception {
				for (String word : testWords) {
					final List<WordData> stems = stemmer.lookup(word);
					for (int i = stems.size() - 1; i >= 0; i--) {
						stems.get(i).getStem();
						stems.get(i).getTag();
					}
				}
				return null;
			}
		}, warmup, rounds);

		System.out.println("Stemming performance -> " + t
		        + ", Sequences/sec.: " + t.elemsPerSecond(sequences));
	}

	/**
	 * This is a simple comparison of performance converting a string to bytes
	 * using String.getBytes and CharsetEncoder (which String.getBytes uses
	 * internally in SUN's JDK).
	 */
	@Ignore
	@Test
	public void charConversion() throws IOException {

		final int warmup = 10;
		final int rounds = 120;
		final int sequences = 100000;

		PerformanceTimer t = PerformanceTimer.run(new Callable<Void>() {
			public Void call() throws Exception {
				for (int i = 0; i < sequences; i++)
					"dbaoidbhoei".getBytes("UTF-8");
				return null;
			}
		}, warmup, rounds);

		System.out.println("getBytes -> " + t + ", Sequences/sec.: "
		        + t.elemsPerSecond(sequences));

		t = PerformanceTimer.run(new Callable<Void>() {
			final CharBuffer chars = CharBuffer.allocate(100);
			final ByteBuffer bytes = ByteBuffer.allocate(100);
			final CharsetEncoder encoder = Charset.forName("UTF-8")
			        .newEncoder();

			public Void call() throws Exception {
				String input = "dbaoidbhoei";

				for (int i = 0; i < sequences; i++) {
					chars.clear();
					for (int j = 0; j < input.length(); j++) {
						chars.put(input.charAt(j));
					}
					chars.flip();

					bytes.clear();
					chars.mark();
					encoder.encode(chars, bytes, true);
					bytes.flip();
					chars.reset();
				}
				return null;
			}
		}, warmup, rounds);

		System.out.println("encoder -> " + t + ", Sequences/sec.: "
		        + t.elemsPerSecond(sequences));
	}
}
