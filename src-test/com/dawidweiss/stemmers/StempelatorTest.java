package com.dawidweiss.stemmers;

import java.io.IOException;
import java.net.URL;

import com.dawidweiss.stemmers.Stempelator;

import junit.framework.TestCase;
import junitx.framework.ArrayAssert;

/**
 * Tests stempelator.
 * 
 * @author Dawid Weiss
 */
public class StempelatorTest extends TestCase {

    public StempelatorTest(String s) {
        super(s);
    }
    
    public void testDefaultConstructor() {
        try {
            new Stempelator();
        } catch (IOException e) {
            fail();
        }
    }
    
    public void tearDown() {
        System.setProperty(Stempelator.PROPERTY_NAME_STEMPEL_TABLE, "");
    }
    
    public void testDictionaryLocationAsUrl() {
        URL url = this.getClass().getResource("stemmer_test.out");
        assertNotNull(url);
        System.setProperty(Stempelator.PROPERTY_NAME_STEMPEL_TABLE, url.toExternalForm());
        try {
            new Stempelator();
        } catch (IOException e) {
            fail();
        }
    }

    public void testDictionaryLocationAsResourcePath() {
        System.setProperty(Stempelator.PROPERTY_NAME_STEMPEL_TABLE, 
                "stemmer_test.out");
        try {
            new Stempelator();
        } catch (IOException e) {
            fail();
        }
        
        System.setProperty(Stempelator.PROPERTY_NAME_STEMPEL_TABLE, 
        	"/res/stemmer_nosuchsize.out");
		try {
		    new Stempelator();
		    fail("Should fail on non-existing table.");
		} catch (IOException e) {
		    // ok, expected.
		}
    }
    
    public void testStemming() throws IOException {
        Stempelator s = new Stempelator();
        
        ArrayAssert.assertEquals(
                new String [] {"żywotopisarstwo"},
                s.stem("żywotopisarstwie"));
        
        ArrayAssert.assertEquals(
                new String [] {"abradować"},
                s.stem("abradowałoby"));

        // We can't test the result because it depends
        // on the size of the Stempel's dictionary. SO we just
        // test if something _not_ present in Lametyzator correctly
        // falls back to Stempel.
        assertTrue( s.stem("martygalski").length > 0);
    }
}
