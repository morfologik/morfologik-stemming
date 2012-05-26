package morfologik.stemming;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

public class SpellerTest {

	public void testRunonWords() throws IOException {
		final URL url = this.getClass().getResource("test-infix.dict");
		final IStemmer s = new DictionaryLookup(Dictionary.read(url));
		final Speller spell = new Speller(s);
		assertTrue(spell.replaceRunOnWords("Rzeczypospolita").isEmpty());
		assertTrue(spell.replaceRunOnWords("RzekuniaRzeczypospolita").
				contains("Rzekunia Rzeczypospolita"));
	}
	
	@Test
	public void test() throws IOException {
		testRunonWords();
	}

}
