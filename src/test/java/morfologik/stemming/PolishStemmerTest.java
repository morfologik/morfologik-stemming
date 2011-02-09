package morfologik.stemming;

import static morfologik.stemming.DictionaryLookupTest.assertNoStemFor;
import static morfologik.stemming.DictionaryLookupTest.stem;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashSet;
import java.util.TreeSet;

import org.junit.Ignore;
import org.junit.Test;

/*
 * 
 */
public class PolishStemmerTest {
	/* */
	@Test
	public void testLexemes() throws IOException {
		PolishStemmer s = new PolishStemmer();

		assertEquals("żywotopisarstwo", stem(s, "żywotopisarstwie")[0]);
		assertEquals("abradować", stem(s, "abradowałoby")[0]);

		assertArrayEquals(new String[] { "żywotopisarstwo", "subst:sg:loc:n" },
		        stem(s, "żywotopisarstwie"));
		assertArrayEquals(new String[] { "bazia", "subst:pl:inst:f" }, stem(s,
		        "baziami"));

		// This word is not in the dictionary.
		assertNoStemFor(s, "martygalski");
	}

	/* */
	@Test
	@Ignore
	public void listUniqueTags() throws IOException {
		HashSet<String> forms = new HashSet<String>();
		for (WordData wd : new PolishStemmer()) {
			final CharSequence chs = wd.getTag();
			if (chs == null) {
				System.err.println("Missing tag for: " + wd.getWord());
				continue;
			}
			forms.add(chs.toString());
		}

		for (String s : new TreeSet<String>(forms)) {
			System.out.println(s);
		}
	}
}
