package morfologik.stemming;

/**
 * <p>A generic stemmer interface in Morfologik.
 * 
 * @see Lametyzator
 * @see Stempel
 * @see Stempelator
 */
public interface IStemmer {
    /**
     * Returns an array of potential base forms (stems) of the word, or <code>null</code>
     * if the word is not found in the dictionary.
     */
    public String[] stem(String word);

    /**
     * <p>Returns an array of pairs of the form:
     * <pre>
     * String stem1, String form1, String stem2, String stem2, ...
     * </pre> 
     * or <code>null</code> if the word is not found in the dictionary. 
     *
     * <p>The form tag is a simple string and depends on what was saved in the automaton
     * (it may be nonsensical or even <code>null</code>).
     */
    public String[] stemAndForm(final String word);
}
