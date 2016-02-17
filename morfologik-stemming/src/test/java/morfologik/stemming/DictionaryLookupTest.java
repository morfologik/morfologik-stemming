package morfologik.stemming;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;

import morfologik.fsa.FSA;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

public class DictionaryLookupTest {
  @Test
  public void testApplyReplacements() {
    LinkedHashMap<String, String> conversion = new LinkedHashMap<>();
    conversion.put("'", "`");
    conversion.put("fi", "ﬁ");
    conversion.put("\\a", "ą");
    conversion.put("Barack", "George");
    conversion.put("_", "xx");
    assertEquals("ﬁlut", DictionaryLookup.applyReplacements("filut", conversion));
    assertEquals("ﬁzdrygałką", DictionaryLookup.applyReplacements("fizdrygałk\\a", conversion));
    assertEquals("George Bush", DictionaryLookup.applyReplacements("Barack Bush", conversion));
    assertEquals("xxxxxxxx", DictionaryLookup.applyReplacements("____", conversion));
  }

  @Test
  public void testRemovedEncoderProperties() throws IOException {
    final URL url = this.getClass().getResource("test-removed-props.dict");
    try {
      new DictionaryLookup(Dictionary.read(url));
      Assert.fail();
    } catch (IOException e) {
      assertThat(e).hasMessageContaining(
          DictionaryAttribute.ENCODER.propertyName);
    }
  }

  @Test
  public void testPrefixDictionaries() throws IOException {
    final URL url = this.getClass().getResource("test-prefix.dict");
    final IStemmer s = new DictionaryLookup(Dictionary.read(url));

    assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" },
        stem(s, "Rzeczypospolitej"));
    assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" },
        stem(s, "Rzecząpospolitą"));

    // This word is not in the dictionary.
    assertNoStemFor(s, "martygalski");
  }

  @Test
  public void testInputConversion() throws IOException {
    final URL url = this.getClass().getResource("test-prefix.dict");
    final IStemmer s = new DictionaryLookup(Dictionary.read(url));

    assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" },
        stem(s, "Rzecz\\apospolit\\a"));

    assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" },
        stem(s, "krowa\\apospolit\\a"));
  }

  /* */
  @Test
  public void testInfixDictionaries() throws IOException {
    final URL url = this.getClass().getResource("test-infix.dict");
    final IStemmer s = new DictionaryLookup(Dictionary.read(url));

    Assertions.assertThat(stem(s, "Rzeczypospolitej"))
      .containsExactly("Rzeczpospolita", "subst:irreg");

    Assertions.assertThat(stem(s, "Rzeczyccy"))
      .containsExactly("Rzeczycki", "adj:pl:nom:m");

    Assertions.assertThat(stem(s, "Rzecząpospolitą"))
      .containsExactly("Rzeczpospolita", "subst:irreg");

    // This word is not in the dictionary.
    assertNoStemFor(s, "martygalski");
    
    // This word uses characters that are outside of the encoding range of the dictionary. 
    assertNoStemFor(s, "Rzeczyckiõh");
  }

  /* */
  @Test
  public void testWordDataIterator() throws IOException {
    final URL url = this.getClass().getResource("test-infix.dict");
    final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));

    final HashSet<String> entries = new HashSet<String>();
    for (WordData wd : s) {
      entries.add(wd.getWord() + " " + wd.getStem() + " " + wd.getTag());
    }

    // Make sure a sample of the entries is present.
    Assertions.assertThat(entries)
      .contains(
          "Rzekunia Rzekuń subst:sg:gen:m",
          "Rzeczkowskie Rzeczkowski adj:sg:nom.acc.voc:n+adj:pl:acc.nom.voc:f.n",
          "Rzecząpospolitą Rzeczpospolita subst:irreg",
          "Rzeczypospolita Rzeczpospolita subst:irreg",
          "Rzeczypospolitych Rzeczpospolita subst:irreg",
          "Rzeczyckiej Rzeczycki adj:sg:gen.dat.loc:f");
  }

  /* */
  @Test
  public void testWordDataCloning() throws IOException {
    final URL url = this.getClass().getResource("test-infix.dict");
    final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));

    ArrayList<WordData> words = new ArrayList<WordData>();
    for (WordData wd : s) {
      WordData clone = wd.clone();
      words.add(clone);
    }

    // Reiterate and verify that we have the same entries.
    final DictionaryLookup s2 = new DictionaryLookup(Dictionary.read(url));
    int i = 0;
    for (WordData wd : s2) {
      WordData clone = words.get(i++);
      assertEqualSequences(clone.getStem(), wd.getStem());
      assertEqualSequences(clone.getTag(), wd.getTag());
      assertEqualSequences(clone.getWord(), wd.getWord());
    }

    // Check collections contract.
    final HashSet<WordData> entries = new HashSet<WordData>();
    try {
      entries.add(words.get(0));
      fail();
    } catch (RuntimeException e) {
      // Expected.
    }
  }

  private void assertEqualSequences(CharSequence s1, CharSequence s2) {
    assertEquals(s1.toString(), s2.toString());
  }

  /* */
  @Test
  public void testMultibyteEncodingUTF8() throws IOException {
    final URL url = this.getClass().getResource("test-diacritics-utf8.dict");
    Dictionary read = Dictionary.read(url);
    final IStemmer s = new DictionaryLookup(read);

    assertArrayEquals(new String[] { "merge", "001" }, stem(s, "mergeam"));
    assertArrayEquals(new String[] { "merge", "002" }, stem(s, "merseserăm"));
  }

  /* */
  @Test
  public void testSynthesis() throws IOException {
    final URL url = this.getClass().getResource("test-synth.dict");
    final IStemmer s = new DictionaryLookup(Dictionary.read(url));

    assertArrayEquals(new String[] { "miała", null }, stem(s,
        "mieć|verb:praet:sg:ter:f:?perf"));
    assertArrayEquals(new String[] { "a", null }, stem(s, "a|conj"));
    assertArrayEquals(new String[] {}, stem(s, "dziecko|subst:sg:dat:n"));

    // This word is not in the dictionary.
    assertNoStemFor(s, "martygalski");
  }

  /* */
  @Test
  public void testInputWithSeparators() throws IOException {
    final URL url = this.getClass().getResource("test-separators.dict");
    final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));

    /*
     * Attemp to reconstruct input sequences using WordData iterator.
     */
    ArrayList<String> sequences = new ArrayList<String>();
    for (WordData wd : s) {
      sequences.add("" + wd.getWord() + " " + wd.getStem() + " "
          + wd.getTag());
    }
    Collections.sort(sequences);

    assertEquals("token1 null null", sequences.get(0));
    assertEquals("token2 null null", sequences.get(1));
    assertEquals("token3 null +", sequences.get(2));
    assertEquals("token4 token2 null", sequences.get(3));
    assertEquals("token5 token2 null", sequences.get(4));
    assertEquals("token6 token2 +", sequences.get(5));
    assertEquals("token7 token2 token3+", sequences.get(6));
    assertEquals("token8 token2 token3++", sequences.get(7));
  }

  /* */
  @Test
  public void testSeparatorInLookupTerm() throws IOException {
    FSA fsa = FSA.read(getClass().getResourceAsStream("test-separator-in-lookup.fsa"));

    DictionaryMetadata metadata = new DictionaryMetadataBuilder()
      .separator('+')
      .encoding("iso8859-1")
      .encoder(EncoderType.INFIX)
      .build();

    final DictionaryLookup s = new DictionaryLookup(new Dictionary(fsa, metadata));
    assertEquals(0, s.lookup("l+A").size());
  }

  /* */
  @Test
  public void testGetSeparator() throws IOException {
    final URL url = this.getClass().getResource("test-separators.dict");
    final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));
    assertEquals('+', s.getSeparatorChar());
  }

  /* */
  public static String asString(CharSequence s) {
    if (s == null)
      return null;
    return s.toString();
  }

  /* */
  public static String[] stem(IStemmer s, String word) {
    ArrayList<String> result = new ArrayList<String>();
    for (WordData wd : s.lookup(word)) {
      result.add(asString(wd.getStem()));
      result.add(asString(wd.getTag()));
    }
    return result.toArray(new String[result.size()]);
  }

  /* */
  public static void assertNoStemFor(IStemmer s, String word) {
    assertArrayEquals(new String[] {}, stem(s, word));
  }
}
