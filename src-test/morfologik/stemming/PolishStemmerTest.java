package morfologik.stemming;

import java.io.IOException;

import org.junit.Test;
import static org.junit.Assert.*;
import static morfologik.stemming.DictionaryLookupTest.*;

/*
 * 
 */
public class PolishStemmerTest {

    /* */
    @Test
    public void testPolishStemmer() throws IOException {
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
}
