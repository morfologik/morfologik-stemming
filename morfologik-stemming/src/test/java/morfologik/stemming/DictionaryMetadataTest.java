package morfologik.stemming;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;

public class DictionaryMetadataTest extends RandomizedTest {
  @Test
  public void testEscapeSeparator() throws IOException {
    DictionaryMetadata m = DictionaryMetadata.read(getClass().getResourceAsStream("escape-separator.info"));
    Assertions.assertThat(m.getSeparator()).isEqualTo((byte) '\t');
  }

  @Test
  public void testUnicodeSeparator() throws IOException {
    DictionaryMetadata m = DictionaryMetadata.read(getClass().getResourceAsStream("unicode-separator.info"));
    Assertions.assertThat(m.getSeparator()).isEqualTo((byte) '\t');
  }

  @Test
  public void testWriteMetadata() throws IOException {
    StringWriter sw = new StringWriter();

    EncoderType encoder = randomFrom(EncoderType.values());
    Charset encoding = randomFrom(Arrays.asList(
        StandardCharsets.UTF_8,
        StandardCharsets.ISO_8859_1,
        StandardCharsets.US_ASCII));
    
    DictionaryMetadata.builder()
      .encoding(encoding)
      .encoder(encoder)
      .separator('|')
      .build()
      .write(sw);
    
    DictionaryMetadata other = 
        DictionaryMetadata.read(new ByteArrayInputStream(sw.toString().getBytes(StandardCharsets.UTF_8)));

    Assertions.assertThat(other.getSeparator()).isEqualTo((byte) '|');
    Assertions.assertThat(other.getDecoder().charset()).isEqualTo(encoding);
    Assertions.assertThat(other.getEncoder().charset()).isEqualTo(encoding);
    Assertions.assertThat(other.getSequenceEncoderType()).isEqualTo(encoder);
  }
}
