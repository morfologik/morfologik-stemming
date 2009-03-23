package morfologik.stemming;

import java.io.IOException;
import java.net.URL;

import morfologik.fsa.Dictionary;
import morfologik.stemming.Lametyzator;
import morfologik.stemming.Stempelator;

import junit.framework.TestCase;
import junitx.framework.ArrayAssert;

/**
 * Tests {@link Lametyzator} class.
 */
public class LametyzatorTest extends TestCase {
    /**
     * 
     */
    public LametyzatorTest(String name) {
        super(name);
    }

    /**
     * 
     */
    public void tearDown() {
        System.setProperty(Lametyzator.PROPERTY_NAME_LAMETYZATOR_DICTIONARY, "");
    }

    /**
     *
     */
    public void testDefaultConstructor() throws Exception {
        new Lametyzator();
    }

    /**
     * 
     */
    public void testDictionaryLocationAsUrl() {
        URL url = this.getClass().getResource("test-polish-1.dict");
        assertNotNull(url);
        System.setProperty(Lametyzator.PROPERTY_NAME_LAMETYZATOR_DICTIONARY, url.toExternalForm());
        try {
            new Stempelator();
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * 
     */
    public void testDictionaryLocationAsResourcePath() {
        System.setProperty(Lametyzator.PROPERTY_NAME_LAMETYZATOR_DICTIONARY, "/morfologik/stemmers/test-polish-1.dict");
        try {
            new Lametyzator();
        } catch (IOException e) {
            fail();
        }

        System.setProperty(Lametyzator.PROPERTY_NAME_LAMETYZATOR_DICTIONARY, "nosuchdict.dict");
        try {
            new Lametyzator();
            fail("Should fail on non-existing dictionary.");
        } catch (IOException e) {
            // ok, expected.
        }
    }

    /**
     * 
     */
    public void testStemming() throws IOException {
        Lametyzator s = new Lametyzator();

        ArrayAssert.assertEquals(new String[] { "żywotopisarstwo" }, s.stem("żywotopisarstwie"));
        ArrayAssert.assertEquals(new String[] { "abradować" }, s.stem("abradowałoby"));

        // This word is not in the dictionary.
        assertNoStemFor(s, "martygalski");
    }

    /**
     * 
     */
    public void testStemmingAndForm() throws IOException {
        Lametyzator s = new Lametyzator();

        ArrayAssert.assertEquals(new String[] { "żywotopisarstwo", "subst:sg:loc:n" }, s.stemAndForm("żywotopisarstwie"));
        ArrayAssert.assertEquals(new String[] { "bazia", "subst:pl:inst:f" }, s.stemAndForm("baziami"));

        // This word is not in the dictionary.
        assertNoStemFor(s, "martygalski");
    }

    /**
     * 
     */
    public void testPrefix() throws IOException {
        final URL url = this.getClass().getResource("test-prefix.dict");
        final Lametyzator s = new Lametyzator(Dictionary.read(url));

        ArrayAssert.assertEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzeczypospolitej"));
        ArrayAssert.assertEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzecząpospolitą"));

        // This word is not in the dictionary.
        assertNoStemFor(s, "martygalski");
    }

    /**
     * 
     */
    public void testInfix() throws IOException {
        final URL url = this.getClass().getResource("test-infix.dict");
        final Lametyzator s = new Lametyzator(Dictionary.read(url));

        ArrayAssert.assertEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzeczypospolitej"));
        ArrayAssert.assertEquals(new String[] { "Rzeczycki", "adj:pl:nom:m" }, s.stemAndForm("Rzeczyccy"));
        ArrayAssert.assertEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzecząpospolitą"));

        // This word is not in the dictionary.
        assertNoStemFor(s, "martygalski");
    }

    /**
     * 
     */
    public void testMultibyteEncodingUTF8() throws IOException {
        final URL url = this.getClass().getResource("test-diacritics-utf8.dict");
        final Lametyzator s = new Lametyzator(Dictionary.read(url));

        ArrayAssert.assertEquals(new String[] { "merge", "001" }, s.stemAndForm("mergeam"));
        ArrayAssert.assertEquals(new String[] { "merge", "002" }, s.stemAndForm("merseserăm"));
    }

    /**
     * 
     */
    public void testSynthesis() throws IOException {
        final URL url = this.getClass().getResource("test-synth.dict");
        final Lametyzator s = new Lametyzator(Dictionary.read(url));

        ArrayAssert.assertEquals(new String[] { "miała" }, s.stem("mieć|verb:praet:sg:ter:f:?perf"));
        ArrayAssert.assertEquals(new String[] { "a" }, s.stem("a|conj"));
        ArrayAssert.assertEquals(null, s.stem("dziecko|subst:sg:dat:n"));

        // This word is not in the dictionary.
        assertNoStemFor(s, "martygalski");
    }

    /**
     * 
     */
    private void assertNoStemFor(Lametyzator s, String word) {
        assertTrue(s.stem(word) == null);
    }
}
