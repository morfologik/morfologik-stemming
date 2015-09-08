package morfologik.stemming;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;

public class EncodersTest extends RandomizedTest {
  @Test
  public void testSharedPrefix() throws IOException {
    Assertions.assertThat(
      BufferUtils.sharedPrefixLength(
          ByteBuffer.wrap(b("abcdef")),
          ByteBuffer.wrap(b("abcd__"))))
      .isEqualTo(4);
    
    Assertions.assertThat(
        BufferUtils.sharedPrefixLength(
            ByteBuffer.wrap(b("")),
            ByteBuffer.wrap(b("_"))))
        .isEqualTo(0);

    Assertions.assertThat(
        BufferUtils.sharedPrefixLength(
            ByteBuffer.wrap(b( "abcdef"), 2, 2),
            ByteBuffer.wrap(b("___cd__"), 3, 2)))
        .isEqualTo(2);
  }

  private static byte[] b(String arg) {
    byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
    Assertions.assertThat(bytes).hasSize(arg.length());
    return bytes;
  }
}
