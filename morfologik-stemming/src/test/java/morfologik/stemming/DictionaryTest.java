package morfologik.stemming;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;

public class DictionaryTest extends RandomizedTest {
  @Test
  public void testConvertText() {
    Map<String, String> conversion = new HashMap<String, String>();
    conversion.put("'", "`");
    conversion.put("fi", "ﬁ");
    conversion.put("\\a", "ą");
    conversion.put("Barack", "George");
    assertEquals("ﬁlut", Dictionary.convertText("filut", conversion));
    assertEquals("ﬁzdrygałką", Dictionary.convertText("fizdrygałk\\a", conversion));
    assertEquals("George Bush", Dictionary.convertText("Barack Bush", conversion));
  }

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
    assertNotNull(Dictionary.read(dict.toFile()));
  }
}
