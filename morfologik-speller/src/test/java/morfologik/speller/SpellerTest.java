package morfologik.speller;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import morfologik.speller.Speller;
import morfologik.stemming.Dictionary;

import org.junit.BeforeClass;
import org.junit.Test;

public class SpellerTest {
    private static Dictionary slownikDictionary;

    @BeforeClass
    public static void setup() throws Exception {
        final URL url = SpellerTest.class.getResource("slownik.dict");     
        slownikDictionary = Dictionary.read(url);
    }

    
    /*
    @Test
    public void testAbka() throws Exception {
        final Speller spell = new Speller(slownikDictionary, 2);
        System.out.println("Replacements:");
        for (String s : spell.findReplacements("abka")) {
            System.out.println(s);
        }
    }
    */
    
	@Test
	public void testRunonWords() throws IOException {
		final Speller spell = new Speller(slownikDictionary);
		assertTrue(spell.replaceRunOnWords("abaka").isEmpty());
		assertTrue(spell.replaceRunOnWords("abakaabace").
				contains("abaka abace"));

		// Test on an morphological dictionary - should work as well
		final URL url1 = getClass().getResource("test-infix.dict");		
		final Speller spell1 = new Speller(Dictionary.read(url1));
		assertTrue(spell1.replaceRunOnWords("Rzekunia").isEmpty());
		assertTrue(spell1.replaceRunOnWords("RzekuniaRzeczypospolitej").
				contains("Rzekunia Rzeczypospolitej"));				
		assertTrue(spell1.replaceRunOnWords("RzekuniaRze").isEmpty());
	}
	
	@Test
	public void testIsInDictionary() throws IOException {
	 // Test on an morphological dictionary, including separators
     final URL url1 = getClass().getResource("test-infix.dict");     
     final Speller spell1 = new Speller(Dictionary.read(url1));
     assertTrue(spell1.isInDictionary("Rzekunia"));
     assertTrue(!spell1.isInDictionary("Rzekunia+"));
     assertTrue(!spell1.isInDictionary("Rzekunia+aaa"));
     //test UTF-8 dictionary
     final URL url = getClass().getResource("test-utf-spell.dict");     
     final Speller spell = new Speller(Dictionary.read(url));
     assertTrue(spell.isInDictionary("jaźń"));
     assertTrue(spell.isInDictionary("zażółć"));
     assertTrue(spell.isInDictionary("żółwiową"));
     assertTrue(spell.isInDictionary("ćwikła"));
     assertTrue(spell.isInDictionary("Żebrowski"));
     assertTrue(spell.isInDictionary("Święto"));
     assertTrue(spell.isInDictionary("Świerczewski"));
     assertTrue(spell.isInDictionary("abc"));
	}
	
	@Test
	public void testFindReplacements() throws IOException {
		final Speller spell = new Speller(slownikDictionary, 1);
		assertTrue(spell.findReplacements("abka").contains("abak"));
	      //check if we get only dictionary words...
		List<String> reps = spell.findReplacements("bak");
		    for (final String word: reps) {
		        assertTrue(spell.isInDictionary(word));
		    }
		assertTrue(spell.findReplacements("abka~~").isEmpty()); // 2 characters more -> edit distance too large
		assertTrue(!spell.findReplacements("Rezkunia").contains("Rzekunia"));
		
		final URL url1 = getClass().getResource("test-infix.dict");		
		final Speller spell1 = new Speller(Dictionary.read(url1));
		assertTrue(spell1.findReplacements("Rezkunia").contains("Rzekunia"));
		//diacritics
		assertTrue(spell1.findReplacements("Rzękunia").contains("Rzekunia"));
		//we should get no candidates for correct words
		assertTrue(spell1.isInDictionary("Rzekunia"));
		assertTrue(spell1.findReplacements("Rzekunia").isEmpty());
		//and no for things that are too different from the dictionary
		assertTrue(spell1.findReplacements("Strefakibica").isEmpty());
		//nothing for nothing
		assertTrue(spell1.findReplacements("").isEmpty());
	    //nothing for weird characters
        assertTrue(spell1.findReplacements("\u0000").isEmpty());
        //nothing for other characters
        assertTrue(spell1.findReplacements("«…»").isEmpty());
        //nothing for separator
        assertTrue(spell1.findReplacements("+").isEmpty());

	}
	
	@Test
	public void testEditDistanceCalculation() throws IOException {
        final Speller spell = new Speller(slownikDictionary, 5);
        //test examples from Oflazer's paper
	    assertTrue(getEditDistance(spell, "recoginze", "recognize") == 1);
	    assertTrue(getEditDistance(spell, "sailn", "failing") == 3);
	    assertTrue(getEditDistance(spell, "abc", "abcd") == 1);	    
	    assertTrue(getEditDistance(spell, "abc", "abcde") == 2);
	    //test words from fsa_spell output
	    assertTrue(getEditDistance(spell, "abka", "abaka") == 1);
	    assertTrue(getEditDistance(spell, "abka", "abakan") == 2);
	    assertTrue(getEditDistance(spell, "abka", "abaką") == 2);
	    assertTrue(getEditDistance(spell, "abka", "abaki") == 2);
	}
	
	@Test
	public void testCutOffEditDistance() throws IOException {
	    final Speller spell2 = new Speller(slownikDictionary, 2); //note: threshold = 2        
        //test cut edit distance - reprter / repo from Oflazer	    
        assertTrue(getCutOffDistance(spell2, "repo", "reprter") == 1);
        assertTrue(getCutOffDistance(spell2, "reporter", "reporter") == 0);
	}
	
	private int getCutOffDistance(final Speller spell, final String word, final String candidate) {
	    spell.setWordAndCandidate(word, candidate);
        final int [] ced = new int[spell.getCandLen() - spell.getWordLen()];
        for (int i = 0; i < spell.getCandLen() - spell.getWordLen(); i++) {
            ced[i] = spell.cuted(spell.getWordLen() + i);
        }
        Arrays.sort(ced);
        //and the min value...
        if (ced.length > 0) {
            return ced[0];
        }
        return 0;
	}
	
	private int getEditDistance(final Speller spell, final String word, final String candidate) {
	    spell.setWordAndCandidate(word, candidate);	    	   	    
	    final int maxDistance = spell.getEffectiveED(); 
	    final int candidateLen = spell.getCandLen();
	    final int wordLen = spell.getWordLen();
	    int ed = 0;
	    for (int i = 0; i < candidateLen; i++) {
	        if (spell.cuted(i) <= maxDistance) {
	            if (Math.abs(wordLen - 1 - i) <= maxDistance) {
	                ed = spell.ed(wordLen - 1, i);
	            }
	        } 
	    }
	    return ed;
	}
}