package morfologik.fsa.dictionary;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;

import morfologik.fsa.core.FSA;
import morfologik.fsa.core.FSAMatch;
import morfologik.fsa.core.FSATraversalHelper;
import morfologik.stemmers.IStemmer;

/**
 * <p>This class implements a dictionary lookup over an FSA dictionary.
 * 
 * <p>Please note that FSA's in Jan Daciuk's implementation use <em>bytes</em>
 * not unicode characters. Therefore objects of this class always have to be
 * constructed with an encoding used to convert Java strings to byte arrays and
 * vice versa. The dictionary for this class should be created using Jan Daciuk's 
 * FSA package.
 * 
 * @see <a href="http://www.eti.pg.gda.pl/~jandac/fsa.html">FSA package Web site</a> 
 * @author Dawid Weiss
 */
public final class DictionaryStemmer implements IStemmer {
    /** An empty array used for 'no-matches' return. */
    private final static String[] NO_STEM = null;

    /** A Finite State Automaton used for look ups. */
    private final FSATraversalHelper matcher;

    /** FSA's root node. */
    private final FSA.Node root;

    /** 
     * Features of the compiled dictionary.
     * 
     * @see DictionaryFeatures
     */
    private final DictionaryFeatures dictionaryFeatures;

    /**
     * <p>Creates a new object of this class using the given FSA for word lookups
     * and encoding for converting characters to bytes.
     * 
     * @throws UnsupportedEncodingException if the given encoding is not 
     *         found in the system.
     * @throws IllegalArgumentException if FSA's root node cannot be acquired
     *         (dictionary is empty).
     */
    public DictionaryStemmer(Dictionary dictionary)
        throws UnsupportedEncodingException, IllegalArgumentException
    {
        this.dictionaryFeatures = dictionary.features;
        this.root = dictionary.fsa.getStartNode();
        this.matcher = dictionary.fsa.getTraversalHelper();

        if (root == null) {
            throw new IllegalArgumentException("Dictionary must have at least the root node.");
        }

        if (dictionaryFeatures == null) {
            throw new IllegalArgumentException("Dictionary features must not be null.");
        }
    }

    /**
     * @see IStemmer#stem(String)
     */
    public String[] stem(String word) {
        return lookup(word, false);
    }

    /**
     * @see IStemmer#stemAndForm(String)
     */
    public String[] stemAndForm(final String word) {
        return lookup(word, true);
    }

    /**
     * Searches the automaton for lemmas of the inflected form of a
     * <code>word</code>. The result is an array of lemmas or pairs of
     * (lemma, tag), if <code>returnForms</code> parameter is
     * <code>true</code>.
     */
    private String[] lookup(final String word, final boolean returnForms) {
        final String encoding = dictionaryFeatures.encoding;
        final byte separator = dictionaryFeatures.separator;

        try {
            // try to find a partial match in the dictionary.
            final FSAMatch match = matcher.matchSequence(word.getBytes(encoding), root);

            if (match.getMatchResult() == FSAMatch.PREMATURE_WORD_END_FOUND) {
                // the entire sequence fit into the dictionary. Now a separator
                // should be the next
                // character.
                final FSA.Arc arc = match.getMismatchNode().getArcLabelledWith(separator);

                // The situation when the arc points to a final node should
                // NEVER happen. After all, we want the word to have SOME base form.
                if (arc != null && !arc.isFinal()) {
                    // there is such word in the dictionary. Return its base forms.
                    final ArrayList forms = new ArrayList(2);
                    final Iterator i = matcher.getAllSubsequences(arc.getDestinationNode());
                    while (i.hasNext()) {
                        final byte[] baseCompressed = (byte[]) i.next();

                        // look for the delimiter of the 'grammar' tag in
                        // the original Jan Daciuk's FSA format.
                        int j;
                        for (j = 0; j < baseCompressed.length; j++) {
                            if (baseCompressed[j] == separator) break;
                        }

                        // now, expand the prefix/ suffix 'compression'
                        // and store the base form.
                        forms.add(decompress(new String(baseCompressed, 0, j, encoding), word));

                        // if needed, store the tag as well.
                        if (returnForms) {
                            j = j + 1;
                            forms.add(new String(baseCompressed, j, baseCompressed.length - j, encoding));
                        }
                    }

                    if (forms.size() == 0) {
                        return NO_STEM;
                    }
                    
                    return (String[]) forms.toArray(new String[forms.size()]);
                }
            } else {
                // this case is somewhat confusing: we should have hit the
                // separator first...
                // I don't really know how to deal with it at the time being.
            }

            return NO_STEM;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected Exception: " + e.toString());
        }
    }

    /**
     * Decode the base form of an inflected word.
     */
    private String decompress(String encodedBase, String inflected) {
        final int encodedBaseLength = encodedBase.length();
        final boolean fsaPrefixes = this.dictionaryFeatures.usesPrefixes;
        final boolean fsaInfixes = this.dictionaryFeatures.usesInfixes;

        if (encodedBaseLength > 0) {
            final int code0 = (encodedBase.charAt(0) - 'A');
            final int infLen = inflected.length();

            if (!fsaPrefixes && !fsaInfixes) {
                if ((encodedBase.charAt(0) - 'A' >= 0)) {
                    final int stripAtEnd = code0;
                    if (stripAtEnd <= infLen) {
                        return inflected.substring(0, infLen - stripAtEnd) + encodedBase.substring(1);
                    } else {
                        // shouldn't happen, but if so, simply return the
                        // encodedBase
                        return "";
                    }
                } else {
                    // shouldn't happen, but if so, simply return the
                    // encodedBase
                    return encodedBase;
                }
            } else if (fsaPrefixes && !fsaInfixes) {
                if (encodedBaseLength > 1 && code0 >= 0) {
                    final int stripAtBeginning = code0;
                    final int stripAtEnd = encodedBase.charAt(1) - 'A';
                    if (stripAtEnd <= infLen && stripAtBeginning <= infLen) {
                        return inflected.substring(stripAtBeginning, infLen - stripAtEnd) + encodedBase.substring(2);
                    } else {
                        // word simply not found
                        return "";
                    }
                } else {
                    // shouldn't happen, but if so, simply return the
                    // encodedBase
                    return encodedBase;
                }
            } else if (fsaInfixes) { // note: prefixes are silently assumed here
                if (encodedBaseLength > 2 && code0 >= 0) {
                    final int stripPosition = code0;
                    final int stripAtBeginning = encodedBase.charAt(1) - 'A';
                    final int stripAtEnd = encodedBase.charAt(2) - 'A';
                    if (stripPosition <= infLen && stripPosition + stripAtBeginning <= infLen && stripAtEnd <= infLen) {
                        return inflected.substring(0, stripPosition)
                                + inflected.substring(stripPosition + stripAtBeginning, infLen - stripAtEnd)
                                + encodedBase.substring(3);
                    } else {
                        return "";
                    }
                } else {
                    // shouldn't happen, but if so, simply return the encodedBase
                    return encodedBase;
                }
            } else {
                // shouldn't happen, but if so, simply return the encodedBase
                return encodedBase;
            }
        } else {
            // shouldn't happen, but if so, simply return the encodedBase
            return encodedBase;
        }
    }
}
