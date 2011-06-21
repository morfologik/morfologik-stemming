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

	/**
	 * The dictionary resource to load and use for the Polish stemmer.
	 */
	public static enum DICTIONARY {
	    /**
	     * Dictionary of forms from the Morfologik project.
	     * @see "http://morfologik.blogspot.com/"
	     */
        MORFOLOGIK,

	    /**
	     * Dictionary of forms cross-compiled from Morfeusz (MORFEUSZ).
	     * @see "http://sgjp.pl/morfeusz/"
	     */
	    MORFEUSZ,

	    /**
	     * Both dictionaries, combined at runtime. {@link #MORFOLOGIK}
	     * is checked first, then {@link #MORFEUSZ}. <b>If both dictionaries contain an entry
	     * for a surface word, the lemmas are not combined. The first dictionary with the hit 
	     * wins.</b>
	     * 
	     * <p>This may cause problems if morphosyntactic
	     * annotations are to be used because they are not uniform across the two dictionaries.
	     * But for lemmatisation things should work just fine.</p>
	     */
	    COMBINED;
	}

	/**
	 * This constructor is initialized with {@link DICTIONARY#MORFOLOGIK} to preserve
	 * backward compatibility. It will fail with a runtime exception if the dictionary 
	 * is not available.
	 */
	public PolishStemmer() {
	    this(DICTIONARY.MORFOLOGIK);
	}

	/**
     * This constructor is initialized with a built-in dictionary or fails with
     * a runtime exception if the dictionary is not available.
     */
    public PolishStemmer(DICTIONARY dictionary) {
        switch (dictionary) {
            case MORFOLOGIK:
                delegate.add(new DictionaryLookup(Dictionary.getForLanguage("pl")));
                break;

            case MORFEUSZ:
                delegate.add(new DictionaryLookup(Dictionary.getForLanguage("pl-sgjp")));
                break;

            case COMBINED:
                delegate.add(new DictionaryLookup(Dictionary.getForLanguage("pl")));
                delegate.add(new DictionaryLookup(Dictionary.getForLanguage("pl-sgjp")));
                break;
            default:
                throw new RuntimeException();
        }
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
