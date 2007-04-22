package com.dawidweiss.stemmers;

import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;
import junitx.framework.ArrayAssert;

/**
 * Tests lametyzator.
 * 
 * @author Dawid Weiss
 */
public class LametyzatorTest extends TestCase {

    public LametyzatorTest(String s) {
        super(s);
    }

    public void testDefaultConstructor() {
        try {
            new Lametyzator();
        } catch (IOException e) {
            fail();
        }
    }

    public void tearDown() {
        System.setProperty(Lametyzator.PROPERTY_NAME_LAMETYZATOR_DICT, "");
    }

    public void testDictionaryLocationAsUrl() {
        URL url = this.getClass().getResource("polish.28.01.2005-t.dict");
        assertNotNull(url);
        System.setProperty(Lametyzator.PROPERTY_NAME_LAMETYZATOR_DICT, url.toExternalForm());
        try {
            new Stempelator();
        } catch (IOException e) {
            fail();
        }
    }

    public void testDictionaryLocationAsResourcePath() {
        System.setProperty(Lametyzator.PROPERTY_NAME_LAMETYZATOR_DICT, "polish.28.01.2005-t.dict");
        try {
            new Lametyzator();
        } catch (IOException e) {
            fail();
        }

        System.setProperty(Lametyzator.PROPERTY_NAME_LAMETYZATOR_DICT, "nosuchdict.dict");
        try {
            new Lametyzator();
            fail("Should fail on non-existing dictionary.");
        } catch (IOException e) {
            // ok, expected.
        }
    }

    public void testStemming() throws IOException {
        Lametyzator s = new Lametyzator();

        ArrayAssert.assertEquals(new String[] { "żywotopisarstwo" }, s.stem("żywotopisarstwie"));
        ArrayAssert.assertEquals(new String[] { "abradować" }, s.stem("abradowałoby"));

        // This word is not in lametyzator
        assertTrue(s.stem("martygalski") == null);
    }

    public void testStemmingAndForm() throws IOException {
        Lametyzator s = new Lametyzator();

        ArrayAssert.assertEquals(new String[] { "żywotopisarstwo", "-" }, s.stemAndForm("żywotopisarstwie"));
        ArrayAssert.assertEquals(new String[] { "abradować", "-" }, s.stemAndForm("abradowałoby"));

        // This word is not in lametyzator
        assertTrue(s.stem("martygalski") == null);
    }

    public void testPrefix() throws IOException {
        URL url = this.getClass().getResource("test_prefix.dict");
        Lametyzator s = new Lametyzator(url.openStream(), "iso8859-2", '+', true, false);

        ArrayAssert.assertEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzeczypospolitej"));
        ArrayAssert.assertEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzecząpospolitą"));

        // This word is not in lametyzator
        assertTrue(s.stem("martygalski") == null);
    }

    public void testInfix() throws IOException {
        URL url = this.getClass().getResource("test_infix.dict");
        Lametyzator s = new Lametyzator(url.openStream(), "iso8859-2", '+', true, true);

        ArrayAssert.assertEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzeczypospolitej"));
        ArrayAssert.assertEquals(new String[] { "Rzeczycki", "adj:pl:nom:m" }, s.stemAndForm("Rzeczyccy"));
        ArrayAssert.assertEquals(new String[] { "Rzeczpospolita", "subst:irreg" }, s.stemAndForm("Rzecząpospolitą"));

        // This word is not in lametyzator
        assertTrue(s.stem("martygalski") == null);
    }
    
    public void testSynthesis() throws IOException {
      URL url = this.getClass().getResource("polish_synth.dict");
      Lametyzator s = new Lametyzator(url.openStream(), "iso8859-2", '+');

      ArrayAssert.assertEquals(new String[] { "miała"}, s.stem("mieć|verb:praet:sg:ter:f:?perf"));
      ArrayAssert.assertEquals(new String[] { "a" }, s.stem("a|conj"));

      // This word is not in lametyzator
      assertTrue(s.stem("martygalski") == null);
  }

}
