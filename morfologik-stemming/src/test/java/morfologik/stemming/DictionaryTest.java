package morfologik.stemming;

import static org.junit.jupiter.api.Assertions.*;

import com.carrotsearch.randomizedtesting.jupiter.Randomized;
import com.carrotsearch.randomizedtesting.jupiter.RandomizedTest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Randomized
public class DictionaryTest extends RandomizedTest {
  @Test
  public void testReadFromFile(@TempDir Path tempDir) throws IOException {
    Path dict = tempDir.resolve("odd name.dict");
    Path info = dict.resolveSibling("odd name.info");
    try (InputStream dictInput = this.getClass().getResource("test-infix.dict").openStream();
        InputStream infoInput = this.getClass().getResource("test-infix.info").openStream()) {
      Files.copy(dictInput, dict);
      Files.copy(infoInput, info);
    }

    assertNotNull(Dictionary.read(dict.toUri().toURL()));
    assertNotNull(Dictionary.read(dict));
  }
}
