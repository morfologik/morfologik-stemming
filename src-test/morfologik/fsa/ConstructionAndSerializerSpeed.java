package morfologik.fsa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import morfologik.util.MinMax;

import org.junit.Test;

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;

public class ConstructionAndSerializerSpeed extends AbstractBenchmark {
    private final static byte [][] input = 
        FSABuilderTest.generateRandom(25000, new MinMax(1, 20), new MinMax(0, 255));

    /**
     * 
     */
    @Test
    public void testConstructionAndSerializationSpeed() throws IOException {
        State s = FSABuilder.build(input);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new FSA5Serializer().withNumbers().serialize(s, os);
    }
}
