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
	public void testWordDataIterator() throws IOException {
		final URL url = this.getClass().getResource("test-infix.dict");
		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));

		final HashSet<String> entries = new HashSet<String>();
		for (WordData wd : s) {
			entries.add(wd.getWord() + " " + wd.getStem() + " " + wd.getTag());
		}

		// Make sure a sample of the entries is present.
		assertTrue(entries.contains("Rzekunia Rzekuń subst:sg:gen:m"));
		assertTrue(entries
		        .contains("Rzeczkowskie Rzeczkowski adj:sg:nom.acc.voc:n+adj:pl:acc.nom.voc:f.n"));
		assertTrue(entries
		        .contains("Rzecząpospolitą Rzeczpospolita subst:irreg"));
		assertTrue(entries
		        .contains("Rzeczypospolita Rzeczpospolita subst:irreg"));
		assertTrue(entries
		        .contains("Rzeczypospolitych Rzeczpospolita subst:irreg"));
		assertTrue(entries
		        .contains("Rzeczyckiej Rzeczycki adj:sg:gen.dat.loc:f"));
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
			assertEqualSequences(clone.wordCharSequence, wd.wordCharSequence);
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
	public void testWordDataFields() throws IOException {
		final IStemmer s = new PolishStemmer();

		final String word = "liga";
		final List<WordData> response = s.lookup(word);
		assertEquals(2, response.size());

		final HashSet<String> stems = new HashSet<String>();
		final HashSet<String> tags = new HashSet<String>();
		for (WordData wd : response) {
			stems.add(wd.getStem().toString());
			tags.add(wd.getTag().toString());
			assertSame(word, wd.getWord());
		}
		assertTrue(stems.contains("ligać"));
		assertTrue(stems.contains("liga"));
		assertTrue(tags.contains("subst:sg:nom:f"));
		assertTrue(tags.contains("verb:fin:sg:ter:imperf"));

		// Repeat to make sure we get the same values consistently.
		for (WordData wd : response) {
			stems.contains(wd.getStem().toString());
			tags.contains(wd.getTag().toString());
		}

		// Run the same consistency check for the returned buffers.
		final ByteBuffer temp = ByteBuffer.allocate(100);
		for (WordData wd : response) {
			// Buffer should be copied.
			final ByteBuffer copy = wd.getStemBytes(null);
			final String stem = new String(copy.array(), copy.arrayOffset()
			        + copy.position(), copy.remaining(), "iso-8859-2");
			// The buffer should be present in stems set.
			assertTrue(stem, stems.contains(stem));
			// Buffer large enough to hold the contents.
			temp.clear();
			assertSame(temp, wd.getStemBytes(temp));
			// The copy and the clone should be identical.
			assertEquals(0, copy.compareTo(temp));
		}

		for (WordData wd : response) {
			// Buffer should be copied.
			final ByteBuffer copy = wd.getTagBytes(null);
			final String tag = new String(copy.array(), copy.arrayOffset()
			        + copy.position(), copy.remaining(), "iso-8859-2");
			// The buffer should be present in tags set.
			assertTrue(tag, tags.contains(tag));
			// Buffer large enough to hold the contents.
			temp.clear();
			assertSame(temp, wd.getTagBytes(temp));
			// The copy and the clone should be identical.
			assertEquals(0, copy.compareTo(temp));
		}

		for (WordData wd : response) {
			// Buffer should be copied.
			final ByteBuffer copy = wd.getWordBytes(null);
			assertNotNull(copy);
			assertEquals(0, copy.compareTo(ByteBuffer.wrap(word
			        .getBytes("iso-8859-2"))));
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
