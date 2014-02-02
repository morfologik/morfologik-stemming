package morfologik.stemming;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/*
 *
 */
public class DictionaryTest {
  /* */

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
}
