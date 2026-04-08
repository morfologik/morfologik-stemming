package morfologik.tools;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.carrotsearch.randomizedtesting.jupiter.generators.RandomNumbers;
import com.carrotsearch.randomizedtesting.jupiter.generators.RandomPicks;
import com.carrotsearch.randomizedtesting.jupiter.generators.RandomStrings;
import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.DictionaryMetadata;
import morfologik.stemming.EncoderType;
import morfologik.stemming.WordData;

import net.bytebuddy.utility.RandomString;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

import com.carrotsearch.randomizedtesting.jupiter.RandomizedTest;
import com.carrotsearch.randomizedtesting.jupiter.Randomized;
import org.junit.jupiter.api.io.TempDir;

@Randomized
public class DictCompileTest extends RandomizedTest {
  @RepeatedTest(200)
  public void testRoundTrip(@TempDir Path tempDir, Random rnd) throws Exception {
    final Path input = tempDir.resolve("dictionary.input");
    final Path metadata = DictionaryMetadata.getExpectedMetadataLocation(input);

    char separator = RandomPicks.randomFrom(rnd, new Character [] {
        '|',
        ',',
        '\t',
    });

    try (Writer writer = Files.newBufferedWriter(metadata, StandardCharsets.UTF_8)) {
      DictionaryMetadata.builder()
          .separator(separator)
          .encoder(RandomPicks.randomFrom(rnd, EncoderType.values()))
          .encoding(StandardCharsets.UTF_8)
          .build()
          .write(writer);
    }

    final boolean useTags = rnd.nextBoolean();

    Set<String> sequences = new LinkedHashSet<>();
    for (int seqs = RandomNumbers.randomIntInRange(rnd, 0, 100); --seqs >= 0;) {
      String base;
      switch (RandomNumbers.randomIntInRange(rnd, 0, 5)) {
        case 0:
          base = RandomStrings.randomAsciiLettersOfLength(rnd, ('A' - separator) & 0xff);
          break;

        default:
          base = RandomStrings.randomAsciiLettersOfLengthBetween(rnd,1, 100);
          break;
      }
      
      String inflected;
      switch (RandomNumbers.randomIntInRange(rnd, 0, 5)) {
        case 0:
          inflected = base;
          break;

        case 1:
          inflected = RandomStrings.randomAsciiLettersOfLengthBetween(rnd,0, 5) + base;
          break;
          
        case 3: 
          inflected = base + RandomStrings.randomAsciiLettersOfLengthBetween(rnd,0, 5);
          break;

        case 4:
          inflected = RandomStrings.randomAsciiLettersOfLengthBetween(rnd,0, 5) + base + RandomStrings.randomAsciiLettersOfLengthBetween(rnd,0, 5);
          break;

        default:
          inflected = RandomStrings.randomAsciiLettersOfLengthBetween(rnd,0, 200);
            break;
      }
       

      sequences.add(
          base + separator + 
          inflected +
          (useTags ? (separator + RandomStrings.randomAsciiLettersOfLengthBetween(rnd,0, 10)) : ""));
    }

    final boolean ignoreEmpty = rnd.nextBoolean();
    try (Writer writer = Files.newBufferedWriter(input, StandardCharsets.UTF_8)) {
      for (String in : sequences) {
        writer.write(in);
        writer.write('\n');
        
        if (ignoreEmpty && rnd.nextBoolean()) {
          writer.write('\n');
        }
      }
    }

    boolean validate = rnd.nextBoolean();
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
