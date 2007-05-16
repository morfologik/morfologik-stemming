package morfologik.stemmers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import morfologik.fsa.dictionary.Dictionary;
import morfologik.fsa.dictionary.DictionaryStemmer;
import morfologik.util.ResourceUtils;

/**
 * <p>A stemmer performing dictionary lookups for stemmed forms
 * and their tags. This stemmer requires an FSA-compiled dictionary and
 * is a simple delegate to {@link DictionaryStemmer}.
 *
 * @see morfologik.fsa.dictionary.DictionaryStemmer
 * @author Dawid Weiss
 */
public class Lametyzator implements IStemmer {
    /**
     * Global system property that overrides the default dictionary
     * resource in {@link Lametyzator#Lametyzator()}.
     */
    public static final String PROPERTY_NAME_LAMETYZATOR_DICTIONARY = "lametyzator.dictionary";

    /**
     * Default dictionary path.
     */
    private static final String DEFAULT_DICTIONARY_PATH = "/res/polish.08.05.2007.dict";

    /**
     * Local instance of {@link DictionaryStemmer}.
     */
    private final DictionaryStemmer fsaStemmer;

    /**
     * This constructor is initialized with a built-in
     * dictionary or fails with a runtime exception if
     * the dictionary is not available.
     */
    public Lametyzator() throws IOException {
        final String dictionaryResource = System.getProperty(PROPERTY_NAME_LAMETYZATOR_DICTIONARY);
        if (dictionaryResource != null && !"".equals(dictionaryResource)) {
            this.fsaStemmer = new DictionaryStemmer(
                    Dictionary.read(
                            ResourceUtils.getResourceURL(dictionaryResource)));
        } else {
            this.fsaStemmer = new DictionaryStemmer(
                    Dictionary.read(
                            ResourceUtils.getResourceURL(
                                    DEFAULT_DICTIONARY_PATH)));
        }
    }

    /**
     * This constructor uses an explicit {@link Dictionary}.
     * 
     * @throws UnsupportedEncodingException If the dictionary is encoded with an encoding
     * unsupported on this virtual machine.
     */
    public Lametyzator(Dictionary dictionary) 
        throws UnsupportedEncodingException
    {
        this.fsaStemmer = new DictionaryStemmer(dictionary);
    }

    /**
     * @see IStemmer#stem(String) 
     */
    public final String[] stem(String word) {
        return fsaStemmer.stem(word);
    }

    /**
     * @see IStemmer#stemAndForm(String) 
     */
    public final String[] stemAndForm(String word) {
        return fsaStemmer.stemAndForm(word);
    }
}
