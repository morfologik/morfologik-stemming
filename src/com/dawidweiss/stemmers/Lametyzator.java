package com.dawidweiss.stemmers;

import java.io.IOException;
import java.io.InputStream;

import com.dawidweiss.fsa.FSA;
import com.dawidweiss.fsa.util.FSAStemmer;


/**
 * Lametyzator: a dictionary-driven stemmer for the Polish language.
 *
 * @see <a href="http://www.cs.put.poznan.pl/dweiss/xml/projects/lametyzator/index.xml?lang=pl">Lametyzator Web page</a>
 *  
 * @author Dawid Weiss
 */
public final class Lametyzator implements Stemmer {

    /**
     * Name of a system property pointing to a lametyzator
     * dictionary. 
     * The property can be a URL (stringified) or a resource path.
     */
    public final static String PROPERTY_NAME_LAMETYZATOR_DICT
    	= "lametyzator.dict";

    /**
     * The default value of <code>lametyzator.dict</code> system
     * property.
     */
    private final static String LAMETYZATOR_DEFAULT
		= "/res/polish.02.02.2005.dict";

    /** Encoding used when compiling the default dictionary ({@link #LAMETYZATOR_DEFAULT}) */
    private final static String DEFAULT_ENCODING = "iso8859-2";

    /** Field delimiter used when compiling the default dictionary {@link #LAMETYZATOR_DEFAULT}) */
    private final static char DEFAULT_DELIMITER = '+';

    /** An instance of Lametyzator */
    private final FSAStemmer lametyzator;
    
    
    /**
     * Instantiate Lametyzator with the default dictionary. 
     * 
     * <p>The defaults <b>can be configured</b> by setting the
     * following system property (to an URL, files or resource paths):
     * 
     * <ul>
     *      <li><code>lametyzator.dict</code></li>
     * </ul>
     * 
     * <p><b>Instatiation can be quite time-consuming. Cache
     * instances of the stemmer and reuse them.</b></p>
     * 
     * <p>Stemmer objects are thread-safe.</p> 
     * 
     * @throws IOException Thrown when the default dictionary cannot be found
     *      or some other i/o-related problem exists.
     */
    public Lametyzator() throws IOException {
        this(getDefaultStream(), DEFAULT_ENCODING, DEFAULT_DELIMITER);
    }


    /**
     * Instantiate Lametyzator with a dictionary read from
     * an opened {@link InputStream}, specifying internal
     * dictionary encoding and field delimiter.
     *
     * <p><b>Instatiation can be quite time-consuming. Cache
     * instances of the stemmer and reuse them.</b></p>
     * 
     * <p>Stemmer objects are thread-safe.</p>
     *  
     * @param is Input stream with the FSA dictionary.
     * @param dictionaryEncoding Byte encoding used when compiling the dictionary.
     * @param fieldDelimiter FSA field delimiter used when compiling the dictionary.
     * 
     * @since 1.0.2
     */
    public Lametyzator(InputStream is, String dictionaryEncoding, char fieldDelimiter)
        throws IOException
    {
        this.lametyzator = new FSAStemmer(
                FSA.getInstance(is, dictionaryEncoding), dictionaryEncoding, fieldDelimiter);
    }
    
    
    /**
     * Find a stem for a given word.
     * 
     * @param word A word for which the stem is to be found.
     * @return A stem of this word, or null if no stem could be found.
     * This method returns identical forms for words that are equal
     * to their stems.
     */
    public String [] stem(String word) {
        // try lametyzator first -- it is a dictionary driven
        // stemmer, so it should be 100% accurate.
        String [] result = lametyzator.stem(word);
        if (result != null && result.length > 0) {
            return result;
        }

        return null;
    }
    
    /**
     * Find a stem and the form for a given inflected form.
     * 
     * @param word A word for which the stem is to be found.
     * @return An array of pairs: (stem, form) of the given word, 
     * or null if no stem could be found.
     * 
     * @since 1.0.1
     */
    public String [] stemAndForm(String word) {
        // try lametyzator first -- it is a dictionary driven
        // stemmer, so it should be 100% accurate.
        String [] result = lametyzator.stemAndForm(word);
        if (result != null && result.length > 0) {
            return result;
        }

        return null;
    }

    /**
     * Invoke from command line for a demo. 
     */
    public static void main(String [] args) throws IOException {
        new CommandLineDemo().start(new Lametyzator(), args);
    }
    
    /**
     * Returns an {@link InputStream} to the default dictionary.
     *  
     * @see Lametyzator#Lametyzator() 
     */
    private static InputStream getDefaultStream() throws IOException {
        String dict = System.getProperty(PROPERTY_NAME_LAMETYZATOR_DICT);
        if (dict == null || "".equals(dict)) {
            dict = LAMETYZATOR_DEFAULT;
        }

        return Utils.getInputStream(dict);
    }
}
