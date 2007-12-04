package morfologik.stemmers;

import java.io.IOException;
import java.net.URL;

import morfologik.stemmers.Stempelator;

import junit.framework.TestCase;
import junitx.framework.ArrayAssert;

/**
 * Tests {@link Stempelator} stemmer.
 * 
 * @author Dawid Weiss
 */
public class StempelatorTest extends TestCase {

    /**
     * 
     */
    public StempelatorTest(String s) {
        super(s);
    }

    /**
     * 
     */
    public void tearDown() {
        System.setProperty(Stempel.PROPERTY_NAME_STEMPEL_TABLE, "");
    }

    /**
     * 
     */
    public void testDefaultConstructor() {
        try {
            new Stempelator();
        } catch (IOException e) {
            fail();
        }
    }

    /**
     * 
     */
    public void testDictionaryLocationAsUrl() {
        final URL url = this.getClass().getResource("test-stempel.table");
        assertNotNull(url);
        System.setProperty(Stempel.PROPERTY_NAME_STEMPEL_TABLE, url.toExternalForm());
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
        System.setProperty(Stempel.PROPERTY_NAME_STEMPEL_TABLE, "/morfologik/stemmers/test-stempel.table");
        try {
            new Stempelator();
        } catch (IOException e) {
            fail();
        }

        System.setProperty(Stempel.PROPERTY_NAME_STEMPEL_TABLE, "nosuchtable.table");
        try {
            new Stempelator();
            fail("Should fail on non-existing table.");
        } catch (IOException e) {
            // ok, expected.
        }
    }

    /**
     * 
     */
    public void testStemming() throws IOException {
        final Stempelator s = new Stempelator();

        ArrayAssert.assertEquals(new String[] { "żywotopisarstwo" }, s.stem("żywotopisarstwie"));
        ArrayAssert.assertEquals(new String[] { "abradować" }, s.stem("abradowałoby"));

        // We can't test the result because it depends
        // on the size of Stempel's dictionary. So we just
        // test if something _not_ present in Lametyzator correctly
        // falls back to Stempel.
        assertTrue(s.stem("martygalski").length > 0);
    }
}
