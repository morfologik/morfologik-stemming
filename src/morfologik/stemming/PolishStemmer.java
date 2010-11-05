package morfologik.stemming;

import java.util.Iterator;
import java.util.List;

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
	private final DictionaryLookup delegate;

	/**
	 * This constructor is initialized with a built-in dictionary or fails with
	 * a runtime exception if the dictionary is not available.
	 */
	public PolishStemmer() {
		final String languageCode = "pl";

		this.delegate = new DictionaryLookup(Dictionary.getForLanguage(languageCode));
	}

	/**
	 * {@inheritDoc}
	 */
	public List<WordData> lookup(CharSequence word) {
		return delegate.lookup(word);
	}

	/**
	 * Iterates over all dictionary forms stored in this stemmer.
	 */
	public Iterator<WordData> iterator() {
		return delegate.iterator();
	}
}
