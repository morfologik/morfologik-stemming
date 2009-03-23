package morfologik.stemming;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import morfologik.util.ResourceUtils;

import org.egothor.stemmer.Trie;
import org.getopt.stempel.Stemmer;


/**
 * A wrapper around <a href="http://www.getopt.org/stempel">Stempel</a> - a heuristic
 * stemmer by Andrzej Bialecki.
 */
public final class Stempel implements morfologik.stemming.IStemmer {
    /**
     * Name of a system property pointing to a stempel dictionary (stemmer 
     * table). The property can be a URL (stringified) or a resource
     * path.
     */
    public final static String PROPERTY_NAME_STEMPEL_TABLE = "stempel.dictionary";

    /** An instance of Stempel. */
    private final org.getopt.stempel.Stemmer stempel;

    /**
     * <p>Instantiate Stempel with default dictionaries. The default dictionary
     * path can be overriden using system property {@link #PROPERTY_NAME_STEMPEL_TABLE}.
     */
    public Stempel() throws IOException {
        // Initialize stempel first.
        final String stempelTable = System.getProperty(PROPERTY_NAME_STEMPEL_TABLE);
        if (stempelTable != null && !"".equals(stempelTable)) {
            final InputStream is = ResourceUtils.openInputStream(stempelTable);
            final Trie trie = loadTrie(is);
            this.stempel = new Stemmer(trie);
        } else {
            this.stempel = new Stemmer();
        }
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
     * @see IStemmer#stem(String)
     */
    public String [] stem(String word) {
        final String stem = stempel.stem(word, false);
        if (stem != null) {
        	return new String[] {stem};
        } else {
	        return null;
        }
    }

    /**
     * @see IStemmer#stemAndForm(String) 
     */
    public String[] stemAndForm(String word) {
        final String stem = stempel.stem(word, false);
        if (stem != null) {
            return new String[] {stem, null};
        } else {
            return null;
        }
    }
}