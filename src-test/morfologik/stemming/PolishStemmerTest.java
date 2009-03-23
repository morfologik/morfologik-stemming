package morfologik.stemming;

import java.io.IOException;

import junit.framework.TestCase;
import junitx.framework.ArrayAssert;

/*
 * 
 */
public class PolishStemmerTest extends TestCase {

    /* */
    public PolishStemmerTest(String name) {
        super(name);
    }

    /* */
    public void testPolishStemmer() throws IOException {
        PolishStemmer s = new PolishStemmer();

        ArrayAssert.assertEquals(new String[] { "żywotopisarstwo" }, s.stem("żywotopisarstwie"));
        ArrayAssert.assertEquals(new String[] { "abradować" }, s.stem("abradowałoby"));

        ArrayAssert.assertEquals(new String[] { "żywotopisarstwo", "subst:sg:loc:n" }, s.stemAndForm("żywotopisarstwie"));
        ArrayAssert.assertEquals(new String[] { "bazia", "subst:pl:inst:f" }, s.stemAndForm("baziami"));

        // This word is not in the dictionary.
        assertNoStemFor(s, "martygalski");
    }

    /* */
    private void assertNoStemFor(IStemmer s, String word) {
        assertTrue(s.stem(word) == null);
    }
}
