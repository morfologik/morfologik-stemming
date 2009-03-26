package morfologik.stemming;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;

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
    public void testWordDataFields() throws IOException {
	final IStemmer s = new PolishStemmer();

	final List<WordData> response = s.lookup("liga");
	assertEquals(2, response.size());

	final HashSet<String> stems = new HashSet<String>();
	final HashSet<String> tags = new HashSet<String>();
	for (WordData wd : response)
	{
	    stems.add(wd.getStem().toString());
	    tags.add(wd.getTag().toString());
	}
	assertTrue(stems.contains("ligać"));
	assertTrue(stems.contains("liga"));
	assertTrue(tags.contains("subst:sg:nom:f"));
	assertTrue(tags.contains("verb:fin:sg:ter:imperf")); 

	// Repeat to make sure we get the same values consistently.
	for (WordData wd : response)
	{
	    stems.contains(wd.getStem().toString());
	    tags.contains(wd.getTag().toString());
	}

	// Run the same consistency check for the returned buffers.
	final ByteBuffer temp = ByteBuffer.allocate(100);
	for (WordData wd : response)
	{
	    // Buffer should be copied.
	    final ByteBuffer copy = wd.getStemBytes(null);
	    final String stem = new String(
		    copy.array(), 
		    copy.arrayOffset() + copy.position(), 
		    copy.remaining(), 
		    "iso-8859-2");
	    // The buffer should be present in stems set.
	    assertTrue(stem, stems.contains(stem));
	    // Buffer large enough to hold the contents.
	    temp.clear();
	    assertSame(temp, wd.getStemBytes(temp));
	    // The copy and the clone should be identical.
	    assertEquals(0, copy.compareTo(temp));
	}

	for (WordData wd : response)
	{
	    // Buffer should be copied.
	    final ByteBuffer copy = wd.getTagBytes(null);
	    final String tag = new String(
		    copy.array(), 
		    copy.arrayOffset() + copy.position(), 
		    copy.remaining(), 
		    "iso-8859-2");
	    // The buffer should be present in tags set.
	    assertTrue(tag, tags.contains(tag));
	    // Buffer large enough to hold the contents.
	    temp.clear();
	    assertSame(temp, wd.getTagBytes(temp));
	    // The copy and the clone should be identical.
	    assertEquals(0, copy.compareTo(temp));
	}
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
