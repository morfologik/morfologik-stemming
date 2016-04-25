package morfologik.tools;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.DictionaryMetadata;
import morfologik.stemming.EncoderType;
import morfologik.stemming.WordData;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Repeat;

public class DictCompileTest extends RandomizedTest {
  @Test
  @Repeat(iterations = 50)
  public void testRoundTrip() throws Exception {
    final Path input = newTempDir().toPath().resolve("dictionary.input");
    final Path metadata = DictionaryMetadata.getExpectedMetadataLocation(input);

    char separator = '|';
    try (Writer writer = Files.newBufferedWriter(metadata, StandardCharsets.UTF_8)) {
      DictionaryMetadata.builder()
          .separator(separator)
          .encoder(randomFrom(EncoderType.values()))
          .encoding(StandardCharsets.UTF_8)
          .build()
          .write(writer);
    }

    final boolean useTags = randomBoolean();

    Set<String> sequences = new LinkedHashSet<>();
    for (int seqs = randomIntBetween(0, 100); --seqs >= 0;) {
      sequences.add(
          randomAsciiOfLengthBetween(1, 10) + 
          separator + 
          randomAsciiOfLengthBetween(0, 10) +
          (useTags ? (separator + randomAsciiOfLengthBetween(0, 10)) : ""));
    }

    final boolean ignoreEmpty = randomBoolean();
    try (Writer writer = Files.newBufferedWriter(input, StandardCharsets.UTF_8)) {
      for (String in : sequences) {
        writer.write(in);
        writer.write('\n');
        
        if (ignoreEmpty && randomBoolean()) {
          writer.write('\n');
        }
      }
    }

    boolean validate = randomBoolean();
    Assertions.assertThat(new DictCompile(input, false, validate, false, false, ignoreEmpty).call())
      .isEqualTo(ExitStatus.SUCCESS);

    Path dict = input.resolveSibling("dictionary.dict");
    Assertions.assertThat(dict).isRegularFile();

    // Verify the dictionary is valid.
    
    DictionaryLookup dictionaryLookup = new DictionaryLookup(Dictionary.read(dict));
    Set<String> reconstructed = new LinkedHashSet<>();
    for (WordData wd : dictionaryLookup) {
      reconstructed.add("" +
          wd.getStem() + separator +
          wd.getWord() + 
          (useTags ? separator : "") + 
          (wd.getTag() == null ? "" : wd.getTag()));
    }

    Assertions.assertThat(reconstructed).containsOnlyElementsOf(sequences);

    // Verify decompilation via DictDecompile.
    
    // GH-79: if there's only one sequence and there is no tag the decompiler will
    // drop it.
    if (useTags && sequences.size() == 1) {
      String onlyOne = sequences.iterator().next();
      if (onlyOne.endsWith(Character.toString(separator))) {
        sequences.clear();
        sequences.add(onlyOne.substring(0, onlyOne.length() - 1));
      }
    }

    Files.delete(input);
    Assertions.assertThat(new DictDecompile(dict, null, true, validate).call())
      .isEqualTo(ExitStatus.SUCCESS);

    List<String> allLines = Files.readAllLines(input, StandardCharsets.UTF_8);
    Assertions.assertThat(allLines).containsOnlyElementsOf(sequences);
  }
}
