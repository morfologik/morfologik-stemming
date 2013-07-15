package morfologik.stemming;

import java.util.*;

/**
 * A dictionary-based stemmer for the Polish language. This stemmer requires an
 * FSA-compiled dictionary to be present in classpath resources.
 * 
 * <b>Objects of this class are not thread safe.</b>
 * 
 * @see morfologik.stemming.DictionaryLookup
 */
public final class PolishStemmer implements IStemmer, Iterable<WordData> {
	/**
	 * Dictionary lookup delegate.
	 */
	private final List<DictionaryLookup> delegate = new ArrayList<DictionaryLookup>();

	/*
	 * 
	 */
	public PolishStemmer() {
	    delegate.add(new DictionaryLookup(Dictionary.getForLanguage("pl")));
	}

	/**
	 * {@inheritDoc}
	 */
	public List<WordData> lookup(CharSequence word) {
	    if (delegate.size() == 1) {
	        return delegate.get(0).lookup(word);
	    } else {
    	    List<WordData> forms = null;
    	    for (DictionaryLookup lookup : delegate) {
    	        forms = lookup.lookup(word);
    	        if (forms.size() > 0)
    	            break;
    	    }
    	    return forms;
	    }
	}

	/**
	 * Iterates over all dictionary forms stored in this stemmer.
	 */
	public Iterator<WordData> iterator() {
        if (delegate.size() == 1) {
            return delegate.get(0).iterator();
        } else {
            throw new RuntimeException("No iteration over compound stemmer forms: "
                + Arrays.toString(delegate.toArray()));
        }
	}
}
