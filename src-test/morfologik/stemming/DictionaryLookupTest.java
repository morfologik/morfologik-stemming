package morfologik.stemming;

import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;
import junitx.framework.ArrayAssert;

/*
 *
 */
public class DictionaryLookupTest extends TestCase {
    /* */
    public DictionaryLookupTest(String name) {
        super(name);
    }

    /* */
    public void testPrefixDictionaries() throws IOException {
        final URL url = this.getClass().getResource("test-prefix.dict");
        final IStemmer s = new DictionaryLookup(Dictionary.read(url));

        ArrayAssert.assertEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzeczypospolitej"));
        ArrayAssert.assertEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzecząpospolitą"));

        // This word is not in the dictionary.
        assertNoStemFor(s, "martygalski");
    }

    /* */
    public void testInfixDictionaries() throws IOException {
        final URL url = this.getClass().getResource("test-infix.dict");
        final IStemmer s = new DictionaryLookup(Dictionary.read(url));

        ArrayAssert.assertEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzeczypospolitej"));
        ArrayAssert.assertEquals(new String[] { "Rzeczycki", "adj:pl:nom:m" }, s.stemAndForm("Rzeczyccy"));
        ArrayAssert.assertEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzecząpospolitą"));

        // This word is not in the dictionary.
        assertNoStemFor(s, "martygalski");
    }

    /* */
    public void testMultibyteEncodingUTF8() throws IOException {
        final URL url = this.getClass().getResource("test-diacritics-utf8.dict");
        final IStemmer s = new DictionaryLookup(Dictionary.read(url));

        ArrayAssert.assertEquals(new String[] { "merge", "001" }, s.stemAndForm("mergeam"));
        ArrayAssert.assertEquals(new String[] { "merge", "002" }, s.stemAndForm("merseserăm"));
    }

    /* */
    public void testSynthesis() throws IOException {
        final URL url = this.getClass().getResource("test-synth.dict");
        final IStemmer s = new DictionaryLookup(Dictionary.read(url));

        ArrayAssert.assertEquals(new String[] { "miała" }, s.stem("mieć|verb:praet:sg:ter:f:?perf"));
        ArrayAssert.assertEquals(new String[] { "a" }, s.stem("a|conj"));
        ArrayAssert.assertEquals(null, s.stem("dziecko|subst:sg:dat:n"));

        // This word is not in the dictionary.
        assertNoStemFor(s, "martygalski");
    }

    /* */
    private void assertNoStemFor(IStemmer s, String word) {
        assertTrue(s.stem(word) == null);
    }
}
