// this will go to another package but 
// creating a new pom.xml for maven is beyond me...
package morfologik.stemming;

import java.util.List;
import java.util.ArrayList;


public class Speller {

	final IStemmer spellDictionary; 
	
	public Speller(final IStemmer dictionary) {
		spellDictionary = dictionary;
	}
	
	
	/**
	 * Propose suggestions for misspelled runon words. This algorithm comes
	 * from spell.cc in s_fsa package by Jan Daciuk.
	 * @param original The original misspelled word.
	 * @return The list of suggested pairs, as space-concatenated strings.
	 */	
	public List<String> replaceRunOnWords(final String original) {
		final List<String> candidates = new ArrayList<String>();
		if (spellDictionary.lookup(original).isEmpty()) {
			
			CharSequence ch = original;
			
			for (int i = 2; i < ch.length(); i++) {
				//chop from left to right
				CharSequence firstCh = ch.subSequence(0, i);
				if (!spellDictionary.lookup(firstCh).isEmpty()
						&& !spellDictionary.lookup(ch.subSequence(i, ch.length())).isEmpty()) {
					//FIXME: languages with no space might want to get
					//simply a pair of dictionary words, but we need to define
					//a new class for this
					candidates.add(firstCh + " " + ch.subSequence(i, ch.length()));
				}
			}
			
			return candidates;
		}
		return candidates;
	}
}
