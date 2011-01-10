package morfologik.stemming;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import org.junit.Ignore;
import org.junit.Test;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;

@BenchmarkOptions(callgc = false, warmupRounds = 5, benchmarkRounds = 20)
@Ignore
public class StringDecoderBenchmarkTest extends AbstractBenchmark {
	/* Guard against escape analysis and HotSpot opts. */
	public volatile int guard;

	private final int sequences = 1000000;

	final String input = "dbaoidbhoei";
	final CharBuffer chars = CharBuffer.allocate(100);
	final ByteBuffer bytes = ByteBuffer.allocate(100);
	final CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();

	/**
	 * This is a simple comparison of performance converting a string to bytes
	 * using String.getBytes and CharsetEncoder (which String.getBytes uses
	 * internally in SUN's JDK).
	 */
	@Test
	public void stringGetBytes() throws Exception {
		int guard = 0;
		for (int i = 0; i < sequences; i++) {
			guard += input.getBytes("UTF-8").length;
		}
		this.guard = guard;
	}

	@Test
	public void charsetEncoder() throws Exception {
		int guard = 0;
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
			
			guard += chars.remaining();
		}
		
		this.guard = guard;
	}
}
