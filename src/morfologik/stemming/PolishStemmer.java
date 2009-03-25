package morfologik.stemming;

import java.util.List;


/**
 * A dictionary-based stemmer for the Polish language. This stemmer requires an
 * FSA-compiled dictionary to be present in classpath resources.
 * 
 * @see morfologik.stemming.DictionaryLookup
 */
public final class PolishStemmer implements IStemmer {
    /**
     * Dictionary lookup delegate.
     */
    private final DictionaryLookup delegate;

    /**
     * This constructor is initialized with a built-in dictionary or fails with
     * a runtime exception if the dictionary is not available.
     */
    public PolishStemmer() {
	final String languageCode = "pl";
	this.delegate = new DictionaryLookup(
		Dictionary.getForLanguage(languageCode));
    }

    /*
     * 
     */
    public List<WordData> lookup(CharSequence word) {
	return delegate.lookup(word);
    }
}
