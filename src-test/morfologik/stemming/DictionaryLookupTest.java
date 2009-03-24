package morfologik.stemming;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;
import static org.junit.Assert.*;

/*
 *
 */
public class DictionaryLookupTest {

    /* */
    @Test
    public void testPrefixDictionaries() throws IOException {
        final URL url = this.getClass().getResource("test-prefix.dict");
        final IStemmer s = new DictionaryLookup(Dictionary.read(url));

        assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzeczypospolitej"));
        assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzecząpospolitą"));

        // This word is not in the dictionary.
        assertNoStemFor(s, "martygalski");
    }

    /* */
    @Test
    public void testInfixDictionaries() throws IOException {
        final URL url = this.getClass().getResource("test-infix.dict");
        final IStemmer s = new DictionaryLookup(Dictionary.read(url));

        assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzeczypospolitej"));
        assertArrayEquals(new String[] { "Rzeczycki", "adj:pl:nom:m" }, s.stemAndForm("Rzeczyccy"));
        assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzecząpospolitą"));

        // This word is not in the dictionary.
        assertNoStemFor(s, "martygalski");
    }

    /* */
    @Test
    public void testMultibyteEncodingUTF8() throws IOException {
        final URL url = this.getClass().getResource("test-diacritics-utf8.dict");
        final IStemmer s = new DictionaryLookup(Dictionary.read(url));

        assertArrayEquals(new String[] { "merge", "001" }, s.stemAndForm("mergeam"));
        assertArrayEquals(new String[] { "merge", "002" }, s.stemAndForm("merseserăm"));
    }

    /* */
    @Test
    public void testSynthesis() throws IOException {
        final URL url = this.getClass().getResource("test-synth.dict");
        final IStemmer s = new DictionaryLookup(Dictionary.read(url));

        assertArrayEquals(new String[] { "miała" }, s.stem("mieć|verb:praet:sg:ter:f:?perf"));
        assertArrayEquals(new String[] { "a" }, s.stem("a|conj"));
        assertArrayEquals(null, s.stem("dziecko|subst:sg:dat:n"));

        // This word is not in the dictionary.
        assertNoStemFor(s, "martygalski");
    }

    /* */
    private void assertNoStemFor(IStemmer s, String word) {
        assertTrue(s.stem(word) == null);
    }
}
