package morfologik.stemming.polish;

import java.io.IOException;
import java.util.Locale;

import morfologik.stemming.WordData;

import org.junit.Test;

/*
 * 
 */
public class Gh27Test {
	/* */
	@Test
	public void gh27() throws IOException {
		PolishStemmer stemmer = new PolishStemmer();

		String in = "Nie zabrakło oczywiście wpadek. Największym zaskoczeniem okazał się dla nas strój Katarzyny Zielińskiej, której ewidentnie o coś chodziło, ale wciąż nie wiemy o co.";
		for (String t : in.toLowerCase(new Locale("pl")).split("[\\s\\.\\,]+")) {
		  System.out.println("> '" + t + "'");
		  for (WordData wd : stemmer.lookup(t)) {
		    System.out.print(
		        "  - " + 
		        (wd.getStem() == null ? "<null>" : wd.getStem()) + ", " + 
		        wd.getTag());
		  }
		  System.out.println();
		}
	}
}
