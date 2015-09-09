package morfologik.stemming;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;

public class DictionaryTest extends RandomizedTest {
  @Test
  public void testReadFromFile() throws IOException {
    Path tempDir = super.newTempDir().toPath();

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
