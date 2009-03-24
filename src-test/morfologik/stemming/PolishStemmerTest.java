package morfologik.stemming;

import java.io.IOException;

import org.junit.Test;
import static org.junit.Assert.*;

/*
 * 
 */
public class PolishStemmerTest {

    /* */
    @Test
    public void testPolishStemmer() throws IOException {
        PolishStemmer s = new PolishStemmer();

        assertArrayEquals(new String[] { "żywotopisarstwo" }, s.stem("żywotopisarstwie"));
        assertArrayEquals(new String[] { "abradować" }, s.stem("abradowałoby"));

        assertArrayEquals(new String[] { "żywotopisarstwo", "subst:sg:loc:n" }, s.stemAndForm("żywotopisarstwie"));
        assertArrayEquals(new String[] { "bazia", "subst:pl:inst:f" }, s.stemAndForm("baziami"));

        // This word is not in the dictionary.
        assertNoStemFor(s, "martygalski");
    }

    /* */
    private void assertNoStemFor(IStemmer s, String word) {
        assertTrue(s.stem(word) == null);
    }
}
