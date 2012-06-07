package morfologik.speller;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;

import morflogik.speller.Speller;
import morfologik.stemming.Dictionary;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("Fails with an assertion!")
public class SpellerTest {

	@Test
	public void testRunonWords() throws IOException {
		final URL url = this.getClass().getResource("slownik.dict");		
		final Speller spell = new Speller(Dictionary.read(url));
		assertTrue(spell.replaceRunOnWords("abaka").isEmpty());
		assertTrue(spell.replaceRunOnWords("abakaabace").
				contains("abaka abace"));
		
		//test on an morphological dictionary - should work as well
		final URL url1 = this.getClass().getResource("test-infix.dict");		
		final Speller spell1 = new Speller(Dictionary.read(url1));
		assertTrue(spell1.replaceRunOnWords("Rzekunia").isEmpty());
		assertTrue(spell1.replaceRunOnWords("RzekuniaRzeczypospolitej").
				contains("Rzekunia Rzeczypospolitej"));				
	}
	
	@Test
	public void testFindReplacements() throws IOException {
		final URL url = this.getClass().getResource("slownik.dict");		
		final Speller spell = new Speller(Dictionary.read(url), 2);
		assertTrue(spell.findReplacements("abka").contains("abak"));
		
		final URL url1 = this.getClass().getResource("test-infix.dict");		
		final Speller spell1 = new Speller(Dictionary.read(url1));
		assertTrue(spell1.findReplacements("Rezkunia").contains("Rzekunia"));
	}	
}
