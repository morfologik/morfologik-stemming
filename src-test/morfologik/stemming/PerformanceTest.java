package morfologik.stemming;

import java.io.IOException;
import java.util.concurrent.Callable;

import morfologik.util.PerformanceTimer;

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
	final int sequences = 50000;

	@SuppressWarnings("unused")
	final PerformanceTimer t = PerformanceTimer.run(new Callable<Void>() {
	    public Void call() throws Exception {
		int i = sequences;
		for (byte[] sequence : dict.fsa) {
		    if (i-- < 0) break;
		}
		return null;
	    }
	}, warmup, rounds);

	System.out.println("FSA traversal -> " 
		+ t + ", Sequences/sec.: " + t.elemsPerSecond(sequences));
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
	final int sequences = 50000;

	final String [] testWords = new String [sequences];
	int i = 0;
	for (byte[] sequence : dict.fsa) {
	    testWords[i++] = new String(sequence, dict.metadata.encoding);
	    if (i == testWords.length) break;
	}

	/*
	 * Run the test.
	 */
	
	final PerformanceTimer t = PerformanceTimer.run(new Callable<Void>() {
	    public Void call() throws Exception {
		for (String word : testWords) {
		    stemmer.stemAndForm(word);
		}
		return null;
	    }
	}, warmup, rounds);

	System.out.println("Stemming performance -> " 
		+ t + ", Sequences/sec.: " + t.elemsPerSecond(sequences));
    }
}
