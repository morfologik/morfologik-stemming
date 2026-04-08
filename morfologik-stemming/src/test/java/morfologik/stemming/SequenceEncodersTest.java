package morfologik.stemming;

import com.carrotsearch.randomizedtesting.jupiter.Randomized;
import com.carrotsearch.randomizedtesting.jupiter.RandomizedTest;
import com.carrotsearch.randomizedtesting.jupiter.generators.RandomStrings;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;

@Randomized
@ParameterizedClass
@EnumSource(EncoderType.class)
public class SequenceEncodersTest extends RandomizedTest {
  private final ISequenceEncoder coder;

  public SequenceEncodersTest(EncoderType coderType) {
    this.coder = coderType.get();
  }

  @Test
  public void testEncodeSuffixOnRandomSequences(Random rnd) {
    for (int i = 0; i < 10000; i++) {
      assertRoundtripEncode(
          rnd,
          RandomStrings.randomAsciiLettersOfLengthBetween(rnd, 0, 500),
          RandomStrings.randomAsciiLettersOfLengthBetween(rnd, 0, 500));
    }
  }

  @Test
  public void testEncodeSamples(Random rnd) {
    assertRoundtripEncode(rnd, "", "");
    assertRoundtripEncode(rnd, "abc", "ab");
    assertRoundtripEncode(rnd, "abc", "abx");
    assertRoundtripEncode(rnd, "ab", "abc");
    assertRoundtripEncode(rnd, "xabc", "abc");
    assertRoundtripEncode(rnd, "axbc", "abc");
    assertRoundtripEncode(rnd, "axybc", "abc");
    assertRoundtripEncode(rnd, "axybc", "abc");
    assertRoundtripEncode(rnd, "azbc", "abcxy");

    assertRoundtripEncode(rnd, "Niemcami", "Niemiec");
    assertRoundtripEncode(rnd, "Niemiec", "Niemcami");
  }

  private void assertRoundtripEncode(Random rnd, String srcString, String dstString) {
    ByteBuffer source = ByteBuffer.wrap(srcString.getBytes(StandardCharsets.UTF_8));
    ByteBuffer target = ByteBuffer.wrap(dstString.getBytes(StandardCharsets.UTF_8));

    ByteBuffer encoded = coder.encode(ByteBuffer.allocate(rnd.nextInt(30)), source, target);
    ByteBuffer decoded = coder.decode(ByteBuffer.allocate(rnd.nextInt(30)), source, encoded);

    if (!decoded.equals(target)) {
      System.out.println("src: " + BufferUtils.toString(source, StandardCharsets.UTF_8));
      System.out.println("dst: " + BufferUtils.toString(target, StandardCharsets.UTF_8));
      System.out.println("enc: " + BufferUtils.toString(encoded, StandardCharsets.UTF_8));
      System.out.println("dec: " + BufferUtils.toString(decoded, StandardCharsets.UTF_8));
      Assertions.fail("Mismatch.");
    }
  }
}
