package morfologik.stemming;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.junit.Test;

/*
 *
 */
public class DictionaryLookupTest {

    /* */
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

    /* */
    @Test
    public void testInfixDictionaries() throws IOException {
	final URL url = this.getClass().getResource("test-infix.dict");
	final IStemmer s = new DictionaryLookup(Dictionary.read(url));

	assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" },
		stem(s, "Rzeczypospolitej"));
	assertArrayEquals(new String[] { "Rzeczycki", "adj:pl:nom:m" }, stem(s,
		"Rzeczyccy"));
	assertArrayEquals(new String[] { "Rzeczpospolita", "subst:irreg" },
		stem(s, "Rzecząpospolitą"));

	// This word is not in the dictionary.
	assertNoStemFor(s, "martygalski");
    }

    /* */
    @Test
    public void testMultibyteEncodingUTF8() throws IOException {
	final URL url = this.getClass()
		.getResource("test-diacritics-utf8.dict");
	final IStemmer s = new DictionaryLookup(Dictionary.read(url));

	assertArrayEquals(new String[] { "merge", "001" }, stem(s, "mergeam"));
	assertArrayEquals(new String[] { "merge", "002" },
		stem(s, "merseserăm"));
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
    public static String[] stem(IStemmer s, String word) {
	ArrayList<String> result = new ArrayList<String>();
	for (WordData wd : s.lookup(word)) {
	    result.add(asString(wd.getStem()));
	    result.add(asString(wd.getTag()));
	}
	return result.toArray(new String[result.size()]);
    }

    /* */
    public static String asString(CharSequence s) {
	if (s == null)
	    return null;
	return s.toString();
    }

    /* */
    public static void assertNoStemFor(IStemmer s, String word) {
	assertArrayEquals(new String[] {}, stem(s, word));
    }
}
