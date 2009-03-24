package morfologik.util;

import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Simple, simple performance checking.
 */
public final class PerformanceTimer {
    private int rounds;

    private long startTime;
    private long stopTime;

    private double totalTime;

    private double perRoundTime;

    /**
     * Run the task with a given number of warm-up rounds and the given number
     * of cycles.
     */
    public static PerformanceTimer run(final Callable<Void> task,
	    int warmupRounds, int cycles) {
	final PerformanceTimer t = new PerformanceTimer();

	try {
	    while (warmupRounds-- > 0) {
		task.call();
	    }

	    t.start();
	    t.rounds = cycles;
	    while (cycles-- > 0) {
		task.call();
	    }
	    t.stop();
	} catch (Exception e) {
	    throw new RuntimeException(e);
	}

	return t;
    }

    /*
     * 
     */
    private void start() {
	startTime = System.currentTimeMillis();
    }

    /*
     * 
     */
    private void stop() {
	stopTime = System.currentTimeMillis();

	totalTime = (stopTime - startTime) / 1000.0d;
	perRoundTime = totalTime / rounds;
    }
    
    /*
     * 
     */
    @Override
    public String toString() {
	return String.format(Locale.ENGLISH, "Rounds: %d, Time: %.3f, Time/round: %.3f", 
		rounds, totalTime, perRoundTime);
    }

    /*
     * 
     */
    public long elemsPerSecond(int sequences) {
	return (long) (sequences / perRoundTime);
    }
}
