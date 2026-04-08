package morfologik.tools;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import com.carrotsearch.randomizedtesting.jupiter.generators.RandomNumbers;
import com.carrotsearch.randomizedtesting.jupiter.generators.RandomPicks;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.carrotsearch.randomizedtesting.jupiter.RandomizedTest;
import com.carrotsearch.randomizedtesting.jupiter.Randomized;

import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.DictionaryMetadata;
import morfologik.stemming.EncoderType;
import morfologik.stemming.WordData;
import org.junit.jupiter.api.io.TempDir;

@Randomized
public class DictCompileBug extends RandomizedTest {
  @Test
  public void testSeparatorInEncoded(@TempDir Path tempDir, Random rnd) throws Exception {
    final Path input = tempDir.resolve("dictionary.input");
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
    for (int seqs = RandomNumbers.randomIntInRange(rnd, 0, 100); --seqs >= 0;) {
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
