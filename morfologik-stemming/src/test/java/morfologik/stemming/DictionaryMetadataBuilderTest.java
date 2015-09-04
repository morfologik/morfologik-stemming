package morfologik.stemming;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DictionaryMetadataBuilderTest {
  @Test
  public void testAllConstantsHaveBuilderMethods() throws IOException {
    Set<DictionaryAttribute> keySet = new DictionaryMetadataBuilder()
      .convertCase()
      .encoding(Charset.defaultCharset())
      .encoding("UTF-8")
      .frequencyIncluded()
      .ignoreAllUppercase()
      .ignoreCamelCase()
      .ignoreDiacritics()
      .ignoreNumbers()
      .ignorePunctuation()
      .separator('+')
      .supportRunOnWords()
      .encoder(EncoderType.SUFFIX)
      .withEquivalentChars(Collections.<Character,List<Character>>emptyMap())
      .withReplacementPairs(Collections.<String,List<String>>emptyMap())
      .withInputConversionPairs(Collections.<String,String>emptyMap())
      .withOutputConversionPairs(Collections.<String,String>emptyMap())
      .locale(Locale.getDefault())
      .license("")
      .author("")
      .creationDate("")
      .toMap()
      .keySet();

    Set<DictionaryAttribute> all = EnumSet.allOf(DictionaryAttribute.class);
    all.removeAll(keySet);

    Assertions.assertThat(all).isEmpty();
  }
}
