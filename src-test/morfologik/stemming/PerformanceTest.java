package morfologik.stemming;

import java.io.IOException;
import java.util.concurrent.Callable;

import morfologik.util.PerformanceTimer;

import org.junit.Test;

/**
 * Simple performance testing.
 */
public class PerformanceTest {

    /* */
    @Test
    public void traversalPerformance() throws IOException {
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
}
