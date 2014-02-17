package morfologik.speller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import morfologik.stemming.Dictionary;

import org.fest.assertions.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

public class SpellerTest {
  private static Dictionary dictionary;

  @BeforeClass
  public static void setup() throws Exception {
    final URL url = SpellerTest.class.getResource("slownik.dict");
    dictionary = Dictionary.read(url);
  }

  /*
    @Test
    public void testAbka() throws Exception {
        final Speller spell = new Speller(dictionary, 2);
        System.out.println("Replacements:");
        for (String s : spell.findReplacements("abka")) {
            System.out.println(s);
        }
    }
   */

  @Test
  public void testRunonWords() throws IOException {
    final Speller spell = new Speller(dictionary);
    Assertions.assertThat(spell.replaceRunOnWords("abaka")).isEmpty();
    Assertions.assertThat(spell.replaceRunOnWords("abakaabace")).contains("abaka abace");

    // Test on an morphological dictionary - should work as well
    final URL url1 = getClass().getResource("test-infix.dict");
    final Speller spell1 = new Speller(Dictionary.read(url1));
    assertTrue(spell1.replaceRunOnWords("Rzekunia").isEmpty());
    assertTrue(spell1.replaceRunOnWords("RzekuniaRzeczypospolitej").contains("Rzekunia Rzeczypospolitej"));
    assertTrue(spell1.replaceRunOnWords("RzekuniaRze").isEmpty()); //Rze is not found but is a prefix
  }

  @Test
  public void testIsInDictionary() throws IOException {
    // Test on an morphological dictionary, including separators
    final URL url1 = getClass().getResource("test-infix.dict");
    final Speller spell1 = new Speller(Dictionary.read(url1));
    assertTrue(spell1.isInDictionary("Rzekunia"));
    assertTrue(!spell1.isInDictionary("Rzekunia+"));
    assertTrue(!spell1.isInDictionary("Rzekunia+aaa"));
    // test UTF-8 dictionary
    final URL url = getClass().getResource("test-utf-spell.dict");
    final Speller spell = new Speller(Dictionary.read(url));
    assertTrue(spell.isInDictionary("jaźń"));
    assertTrue(spell.isInDictionary("zażółć"));
    assertTrue(spell.isInDictionary("żółwiową"));
    assertTrue(spell.isInDictionary("ćwikła"));
    assertTrue(spell.isInDictionary("Żebrowski"));
    assertTrue(spell.isInDictionary("Święto"));
    assertTrue(spell.isInDictionary("Świerczewski"));
    assertTrue(spell.isInDictionary("abc"));
  }

  @Test
  public void testFindReplacements() throws IOException {
    final Speller spell = new Speller(dictionary, 1);
    assertTrue(spell.findReplacements("abka").contains("abak"));
    //check if we get only dictionary words...
    List<String> reps = spell.findReplacements("bak");
    for (final String word: reps) {
      assertTrue(spell.isInDictionary(word));
    }
    assertTrue(spell.findReplacements("abka~~").isEmpty()); // 2 characters more -> edit distance too large
    assertTrue(!spell.findReplacements("Rezkunia").contains("Rzekunia"));

    final URL url1 = getClass().getResource("test-infix.dict");
    final Speller spell1 = new Speller(Dictionary.read(url1));
    assertTrue(spell1.findReplacements("Rezkunia").contains("Rzekunia"));
    //diacritics
    assertTrue(spell1.findReplacements("Rzękunia").contains("Rzekunia"));
    //we should get no candidates for correct words
    assertTrue(spell1.isInDictionary("Rzekunia"));
    assertTrue(spell1.findReplacements("Rzekunia").isEmpty());
    //and no for things that are too different from the dictionary
    assertTrue(spell1.findReplacements("Strefakibica").isEmpty());
    //nothing for nothing
    assertTrue(spell1.findReplacements("").isEmpty());
    //nothing for weird characters
    assertTrue(spell1.findReplacements("\u0000").isEmpty());
    //nothing for other characters
    assertTrue(spell1.findReplacements("«…»").isEmpty());
    //nothing for separator
    assertTrue(spell1.findReplacements("+").isEmpty());

  }

  @Test
  public void testFrequencyNonUTFDictionary() throws IOException {
    final URL url1 = getClass().getResource("test_freq_iso.dict");
    final Speller spell = new Speller(Dictionary.read(url1));
    assertTrue(spell.isInDictionary("a"));
    assertTrue(!spell.isInDictionary("aõh")); //non-encodable in UTF-8
  }

  @Test
  public void testFindReplacementsInUTF() throws IOException {
    final URL url = getClass().getResource("test-utf-spell.dict");
    final Speller spell = new Speller(Dictionary.read(url));
    assertTrue(spell.findReplacements("gęslą").contains("gęślą"));
    assertTrue(spell.findReplacements("ćwikla").contains("ćwikła"));
    assertTrue(spell.findReplacements("Swierczewski").contains("Świerczewski"));
    assertTrue(spell.findReplacements("zółwiową").contains("żółwiową"));
    assertTrue(spell.findReplacements("Żebrowsk").contains("Żebrowski"));
    assertTrue(spell.findReplacements("święto").contains("Święto"));
    //note: no diacritics here, but we still get matches!
    assertTrue(spell.findReplacements("gesla").contains("gęślą"));
    assertTrue(spell.findReplacements("swieto").contains("Święto"));
    assertTrue(spell.findReplacements("zolwiowa").contains("żółwiową"));
    //using equivalent characters 'x' = 'ź'
    assertTrue(spell.findReplacements("jexn").contains("jaźń"));
    // 'u' = 'ó', so the edit distance is still small...
    assertTrue(spell.findReplacements("zażulv").contains("zażółć"));
    // 'rz' = 'ż', so the edit distance is still small, but with string replacements...
    assertTrue(spell.findReplacements("zarzulv").contains("zażółć"));
    assertTrue(spell.findReplacements("Rzebrowski").contains("Żebrowski"));
    assertTrue(spell.findReplacements("rzółw").contains("żółw"));
    assertTrue(spell.findReplacements("Świento").contains("Święto"));
    // avoid mixed-case words as suggestions when using replacements ('rz' = 'ż')
    assertTrue(spell.findReplacements("zArzółć").get(0).equals("zażółć"));
  }

  @Test
  public void testFindReplacementsUsingFrequency() throws IOException {
    final URL url = getClass().getResource("dict-with-freq.dict");
    final Speller spell = new Speller(Dictionary.read(url));

    //check if we get only dictionary words...
    List<String> reps = spell.findReplacements("jist");
    for (final String word: reps) {
      assertTrue(spell.isInDictionary(word));
    }
    // get replacements ordered by frequency
    assertTrue(reps.get(0).equals("just"));
    assertTrue(reps.get(1).equals("list"));
    assertTrue(reps.get(2).equals("fist"));
    assertTrue(reps.get(3).equals("mist"));
    assertTrue(reps.get(4).equals("jest"));
    assertTrue(reps.get(5).equals("dist"));
    assertTrue(reps.get(6).equals("gist"));
  }

  @Test
  public void testIsMisspelled() throws IOException {
    final URL url = getClass().getResource("test-utf-spell.dict");
    final Speller spell = new Speller(Dictionary.read(url));
    assertTrue(!spell.isMisspelled("Paragraf22"));  //ignorujemy liczby
    assertTrue(!spell.isMisspelled("!"));  //ignorujemy znaki przestankowe
    assertTrue(spell.isMisspelled("dziekie"));  //test, czy znajdujemy błąd
    assertTrue(!spell.isMisspelled("SłowozGarbem"));  //ignorujemy słowa w stylu wielbłąda
    assertTrue(!spell.isMisspelled("Ćwikła"));  //i małe litery
    assertTrue(!spell.isMisspelled("TOJESTTEST"));  //i wielkie litery
    final Speller oldStyleSpell = new Speller(dictionary, 1);
    assertTrue(oldStyleSpell.isMisspelled("Paragraf22"));  // nie ignorujemy liczby
    assertTrue(oldStyleSpell.isMisspelled("!"));  //nie ignorujemy znaków przestankowych
    // assertTrue(oldStyleSpell.isMisspelled("SłowozGarbem"));  //ignorujemy słowa w stylu wielbłąda
    assertTrue(oldStyleSpell.isMisspelled("Abaka"));  //i małe litery
    final URL url1 = getClass().getResource("test-infix.dict");
    final Speller spell1 = new Speller(Dictionary.read(url1));
    assertTrue(!spell1.isMisspelled("Rzekunia"));
    assertTrue(spell1.isAllUppercase("RZEKUNIA"));
    assertTrue(spell1.isMisspelled("RZEKUNIAA")); // finds a typo here
    assertTrue(!spell1.isMisspelled("RZEKUNIA")); // but not here
  }

  @Test
  public void testCamelCase() {
    final Speller spell = new Speller(dictionary, 1);
    assertTrue(spell.isCamelCase("CamelCase"));
    assertTrue(!spell.isCamelCase("Camel"));
    assertTrue(!spell.isCamelCase("CAMEL"));
    assertTrue(!spell.isCamelCase("camel"));
    assertTrue(!spell.isCamelCase("cAmel"));
    assertTrue(!spell.isCamelCase("CAmel"));
    assertTrue(!spell.isCamelCase(""));
    assertTrue(!spell.isCamelCase(null));
  }
  
  @Test
  public void testCapitalizedWord() {
    final Speller spell = new Speller(dictionary, 1);
    assertTrue(spell.isNotCapitalizedWord("CamelCase"));
    assertTrue(!spell.isNotCapitalizedWord("Camel"));
    assertTrue(spell.isNotCapitalizedWord("CAMEL"));
    assertTrue(spell.isNotCapitalizedWord("camel"));
    assertTrue(spell.isNotCapitalizedWord("cAmel"));
    assertTrue(spell.isNotCapitalizedWord("CAmel"));
    assertTrue(spell.isNotCapitalizedWord(""));
  }

  @Test
  public void testGetAllReplacements() throws IOException {
    final URL url = getClass().getResource("test-utf-spell.dict");
    final Speller spell = new Speller(Dictionary.read(url));
    assertTrue(spell.isMisspelled("rzarzerzarzu"));
    assertEquals("[rzarzerzarzu]",
        Arrays.toString(spell.getAllReplacements("rzarzerzarzu", 0, 0).toArray()));
  }

  @Test
  public void testEditDistanceCalculation() throws IOException {
    final Speller spell = new Speller(dictionary, 5);
    //test examples from Oflazer's paper
    assertTrue(getEditDistance(spell, "recoginze", "recognize") == 1);
    assertTrue(getEditDistance(spell, "sailn", "failing") == 3);
    assertTrue(getEditDistance(spell, "abc", "abcd") == 1);
    assertTrue(getEditDistance(spell, "abc", "abcde") == 2);
    //test words from fsa_spell output
    assertTrue(getEditDistance(spell, "abka", "abaka") == 1);
    assertTrue(getEditDistance(spell, "abka", "abakan") == 2);
    assertTrue(getEditDistance(spell, "abka", "abaką") == 2);
    assertTrue(getEditDistance(spell, "abka", "abaki") == 2);
  }

  @Test
  public void testCutOffEditDistance() throws IOException {
    final Speller spell2 = new Speller(dictionary, 2); //note: threshold = 2
    //test cut edit distance - reprter / repo from Oflazer
    assertTrue(getCutOffDistance(spell2, "repo", "reprter") == 1);
    assertTrue(getCutOffDistance(spell2, "reporter", "reporter") == 0);
  }

  private int getCutOffDistance(final Speller spell, final String word, final String candidate) {
    // assuming there is no pair-replacement 
    spell.setWordAndCandidate(word, candidate);
    final int [] ced = new int[spell.getCandLen() - spell.getWordLen()];
    for (int i = 0; i < spell.getCandLen() - spell.getWordLen(); i++) {
      
      ced[i] = spell.cuted(spell.getWordLen() + i, spell.getWordLen() + i, spell.getWordLen() + i);
    }
    Arrays.sort(ced);
    //and the min value...
    if (ced.length > 0) {
      return ced[0];
    }
    return 0;
  }

  private int getEditDistance(final Speller spell, final String word, final String candidate) {
    // assuming there is no pair-replacement
    spell.setWordAndCandidate(word, candidate);
    final int maxDistance = spell.getEffectiveED();
    final int candidateLen = spell.getCandLen();
    final int wordLen = spell.getWordLen();
    int ed = 0;
    for (int i = 0; i < candidateLen; i++) {
      if (spell.cuted(i, i, i) <= maxDistance) {
        if (Math.abs(wordLen - 1 - i) <= maxDistance) {
          ed = spell.ed(wordLen - 1, i, wordLen - 1, i);
        }
      }
    }
    return ed;
  }
}