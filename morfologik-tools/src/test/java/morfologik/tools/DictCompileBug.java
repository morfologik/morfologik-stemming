package morfologik.tools;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;

import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.DictionaryMetadata;
import morfologik.stemming.EncoderType;
import morfologik.stemming.WordData;

public class DictCompileBug extends RandomizedTest {
  @Test
  public void testSeparatorInEncoded() throws Exception {
    final Path input = newTempDir().toPath().resolve("dictionary.input");
    final Path metadata = DictionaryMetadata.getExpectedMetadataLocation(input);

    char separator = '_';
    try (Writer writer = Files.newBufferedWriter(metadata, StandardCharsets.UTF_8)) {
      DictionaryMetadata.builder()
          .separator(separator)
          .encoder(EncoderType.SUFFIX)
          .encoding(StandardCharsets.UTF_8)
          .build()
          .write(writer);
    }

    Set<String> sequences = new LinkedHashSet<>();
    for (int seqs = randomIntBetween(0, 100); --seqs >= 0;) {
      sequences.add("anfragen_anfragen|VER:1:PLU:KJ1:SFT:NEB");
      sequences.add("Anfragen_anfragen|VER:1:PLU:KJ1:SFT:NEB");
    }

    try (Writer writer = Files.newBufferedWriter(input, StandardCharsets.UTF_8)) {
      for (String in : sequences) {
        writer.write(in);
        writer.write('\n');
      }
    }

    Assertions.assertThat(new DictCompile(input, false, true, false, false, false).call())
      .isEqualTo(ExitStatus.SUCCESS);

    Path dict = input.resolveSibling("dictionary.dict");
    Assertions.assertThat(dict).isRegularFile();

    // Verify the dictionary is valid.
    
    DictionaryLookup dictionaryLookup = new DictionaryLookup(Dictionary.read(dict));
    for (WordData wd : dictionaryLookup) {
      System.out.println(wd);
    }
  }
}
