package com.dawidweiss.stemmers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import com.dawidweiss.stemmers.Lametyzator;
import com.dawidweiss.stemmers.Stempelator;

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
        System.setProperty(Lametyzator.PROPERTY_NAME_LAMETYZATOR_DICT, 
                "polish.28.01.2005-t.dict");
        try {
            new Lametyzator();
        } catch (IOException e) {
            fail();
        }
        
        System.setProperty(Lametyzator.PROPERTY_NAME_LAMETYZATOR_DICT, 
        	"nosuchdict.dict");
		try {
		    new Lametyzator();
		    fail("Should fail on non-existing dictionary.");
		} catch (IOException e) {
		    // ok, expected.
		}
    }
    
    public void testStemming() throws IOException {
        Lametyzator s = new Lametyzator();
        
        ArrayAssert.assertEquals(
                new String [] {"żywotopisarstwo"},
                s.stem("żywotopisarstwie"));
        
        ArrayAssert.assertEquals(
                new String [] {"abradować"},
                s.stem("abradowałoby"));

        // This word is not in lametyzator
        assertTrue( s.stem("martygalski") == null);
    }

    public void testStemmingAndForm() throws IOException {
        Lametyzator s = new Lametyzator();

        ArrayAssert.assertEquals(
                new String [] {"żywotopisarstwo", "-"},
                s.stemAndForm("żywotopisarstwie"));

        ArrayAssert.assertEquals(
                new String [] {"abradować", "-"},
                s.stemAndForm("abradowałoby"));

        // This word is not in lametyzator
        assertTrue(s.stem("martygalski") == null);
    }
    
    public void testPrefix() throws IOException {
            
     File f = new File("test_prefix.dict");
     FileInputStream fis = new FileInputStream(f);
 	   Lametyzator s = new Lametyzator(fis,
               "iso8859-2", '+', true, false);
        
        ArrayAssert.assertEquals(
                new String [] {"Rzeczpospolita", "subst:irreg"},
                s.stemAndForm("Rzeczypospolitej"));

        ArrayAssert.assertEquals(
                new String [] {"Rzeczpospolita", "subst:irreg"},
                s.stemAndForm("Rzecząpospolitą"));

        // This word is not in lametyzator
        assertTrue(s.stem("martygalski") == null);
    }
    
    public void testInfix() throws IOException {
        
        File f = new File("test_infix.dict");
        FileInputStream fis = new FileInputStream(f);
    	   Lametyzator s = new Lametyzator(fis,
                  "iso8859-2", '+', true, true);
           
           ArrayAssert.assertEquals(
                   new String [] {"Rzeczpospolita", "subst:irreg"},
                   s.stemAndForm("Rzeczypospolitej"));
           
           ArrayAssert.assertEquals(
                   new String [] {"Rzeczycki", "adj:pl:nom:m"},
                   s.stemAndForm("Rzeczyccy"));

           ArrayAssert.assertEquals(
                   new String [] {"Rzeczpospolita", "subst:irreg"},
                   s.stemAndForm("Rzecząpospolitą"));

           // This word is not in lametyzator
           assertTrue(s.stem("martygalski") == null);
       }

}
