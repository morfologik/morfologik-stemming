package morfologik.stemming;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;

public class SequenceEncodersTest extends RandomizedTest {
    private final ISequenceEncoder coder;

    public SequenceEncodersTest(@Name("coder") ISequenceEncoder coder)
    {
        this.coder = coder;
    }

    @ParametersFactory
    public static List<Object[]> testFactory() {
        List<Object[]> encoders = new ArrayList<>();
        for (EncoderType t : EncoderType.values()) {    
            encoders.add(new Object [] {t.get()});
        }
        return encoders;
    }

    @Test
    public void testEncodeSuffixOnRandomSequences() {
        for (int i = 0; i < 10000; i++) {
            assertRoundtripEncode(
                randomAsciiOfLengthBetween(0, 500),
                randomAsciiOfLengthBetween(0, 500));
        }
    }

    @Test
    public void testEncodeSamples() {
        assertRoundtripEncode("", "");
        assertRoundtripEncode("abc", "ab");
        assertRoundtripEncode("abc", "abx");
        assertRoundtripEncode("ab", "abc");
        assertRoundtripEncode("xabc", "abc");
        assertRoundtripEncode("axbc", "abc");
        assertRoundtripEncode("axybc", "abc");
        assertRoundtripEncode("axybc", "abc");
        assertRoundtripEncode("azbc", "abcxy");

        assertRoundtripEncode("Niemcami", "Niemiec");
        assertRoundtripEncode("Niemiec", "Niemcami");
    }

    private void assertRoundtripEncode(String srcString, String dstString) {
        ByteBuffer source = ByteBuffer.wrap(srcString.getBytes(UTF8));
        ByteBuffer target = ByteBuffer.wrap(dstString.getBytes(UTF8));

        ByteBuffer encoded = coder.encode(ByteBuffer.allocate(randomInt(30)), source, target);
        ByteBuffer decoded = coder.decode(ByteBuffer.allocate(randomInt(30)), source, encoded);

        if (!decoded.equals(target)) {
            System.out.println("src: " + BufferUtils.toString(source, StandardCharsets.UTF_8));
            System.out.println("dst: " + BufferUtils.toString(target, StandardCharsets.UTF_8));
            System.out.println("enc: " + BufferUtils.toString(encoded, StandardCharsets.UTF_8));
            System.out.println("dec: " + BufferUtils.toString(decoded, StandardCharsets.UTF_8));
            Assertions.fail("Mismatch.");
        }
    }
}
