package com.dawidweiss.stemmers;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.egothor.stemmer.Trie;
import org.getopt.stempel.Stemmer;


/**
 * A hybrid stemmer for the Polish language (a plain combination
 * of Lametyzator and Stempel):
 * 
 * <ul>
 * 		<li><b>Stempel</b> -- <a href="http://www.getopt.org/stempel">http://www.getopt.org/stempel</a></li>
 * 	    <li><b>Lametyzator</b> -- <a href="http://www.cs.put.poznan.pl/dweiss/xml/projects/lametyzator/index.xml?lang=pl">http://www.cs.put.poznan.pl/dweiss/xml/projects/lametyzator/index.xml?lang=pl</a></li>
 * </ul>
 * 
 * @author Dawid Weiss
 */
public final class Stempelator implements com.dawidweiss.stemmers.Stemmer {
    /**
     * Name of a system property pointing to a stempel dictionary (stemmer 
     * table). The property can be a URL (stringified) or a resource
     * path.
     */
    public final static String PROPERTY_NAME_STEMPEL_TABLE
    	= "stempel.table";

    /** An instance of Stempel. */
    private final org.getopt.stempel.Stemmer stempel;
    
    /** An instance of Lametyzator */
    private final Lametyzator lametyzator;
    
    
    /**
     * Instantiate Stempel and Lametyzator with the default
     * dictionaries. Defaults can be changed by setting the
     * following system properties to (URLs, or resource 
     * paths):
     * 
     * <ul>
     * 		<li><code>stempel.table</code></li>
     *      <li><code>lametyzator.dict</code></li>
     * </ul>
     * 
     * <p>
     * <b>Instatiation can be quite time-consuming. Cache
     * instances of the stemmer and reuse them.</b> Stemmer
     * objects are thread-safe. 
     */
    public Stempelator() throws IOException {
        // Read stempel first.
        String stempelTable = System.getProperty(PROPERTY_NAME_STEMPEL_TABLE);
        if (stempelTable != null && !"".equals(stempelTable)) {
            InputStream is = Utils.getInputStream(stempelTable);
            final Trie trie = loadTrie(is);
            this.stempel = new Stemmer(trie);
        } else {
            this.stempel = new Stemmer();
        }

        // Now get lametyzator instance
        this.lametyzator = new Lametyzator();
    }
    
    /**
     * Loads {@link Trie} data from an input stream.
     */
    private static Trie loadTrie(InputStream tableData) throws IOException {
        final DataInputStream in = new DataInputStream(new BufferedInputStream(tableData));
        final String method = in.readUTF().toUpperCase();
        final Trie stemmer;
        if (method.indexOf('M') < 0) {
            stemmer = new org.egothor.stemmer.Trie(in);
        } else {
            stemmer = new org.egothor.stemmer.MultiTrie2(in);
        }
        in.close();
        return stemmer;
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
        final String [] result = lametyzator.stem(word);
        if (result != null && result.length > 0) {
            return result;
        }

        // ok, fallback to stempel.
        final String stem = stempel.stem(word, false);
        if (stem != null) {
        	return new String[] {stem};
        } else {
	        return null;
        }
    }
    
    /**
     * Invoke from command line for a demo. 
     */
    public static void main(String [] args) throws IOException {
        new CommandLineDemo().start(new Stempelator(), args);
    }
}
