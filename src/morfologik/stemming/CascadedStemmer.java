package morfologik.stemming;

import java.io.IOException;

/**
 * This stemmer uses a sequence of {@link IStemmer} instances for lookup. The
 * first one to return a non-null result wins.
 */
public final class CascadedStemmer implements IStemmer {
    /** Stemmers used in the cascade. */
    private final IStemmer[] stemmers;

    /**
     * 
     */
    public CascadedStemmer(IStemmer... stemmers) throws IOException {
	this.stemmers = stemmers;
    }

    /**
     * @see IStemmer#stem(String)
     */
    public String[] stem(String word) {
	for (int i = 0; i < stemmers.length; i++) {
	    final String[] result = stemmers[i].stem(word);
	    if (result != null && result.length > 0) {
		return result;
	    }
	}

	return null;
    }

    /**
     * @see IStemmer#stemAndForm(String)
     */
    public String[] stemAndForm(String word) {
	for (int i = 0; i < stemmers.length; i++) {
	    final String[] result = stemmers[i].stemAndForm(word);
	    if (result != null && result.length > 0) {
		return result;
	    }
	}

	return null;
    }
}
