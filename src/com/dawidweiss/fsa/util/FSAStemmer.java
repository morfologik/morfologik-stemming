
package com.dawidweiss.fsa.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;

import com.dawidweiss.fsa.FSA;
import com.dawidweiss.fsa.FSAMatch;
import com.dawidweiss.fsa.FSATraversalHelper;


/**
 * This class implements a stemmer over a Finite State Automaton dictionary.
 *
 * Please note that FSA's in Jan Daciuk's implementation use <em>bytes</em> not unicode characters.
 * Therefore objects of this class always have to be constructed with an encoding used to convert
 * Java strings to byte arrays.
 *
 * The dictionary for this class should be created using Jan Daciuk's FSA package (available for
 * most Unix platforms).
 * 
 * @author Dawid Weiss
 */
public final class FSAStemmer
{
    /** A separator character (converted to byte for the FSA). */
    private byte  separator;

    /** A Finite State Automaton used for look ups. */
    private FSATraversalHelper  matcher;

    /** FSA's root node. */
    private FSA.Node           root;

    /** Encoding used for converting characters to bytes */
    private String  encoding;

    /** An empty array used for 'no-matches' return. */
    private final static String [] NO_STEM = new String[0];

    /** True if the dictionary uses prefixes **/
    private boolean fsaPrefixes = false;
    
    /** True if the dictionary uses infixes **/
    private boolean fsaInfixes = false;
    
    /**
     *  Creates a new object of this class using the given FSA for word lookups
     *  and encoding for converting characters to bytes.
     *
     *  @throws UnsupportedEncodingException if the given encoding
     *          has not been found in the system.
     *  @throws IllegalArgumentException if FSA's root node cannot be acquired
     *          (dictionary is empty).
     */
    public FSAStemmer( FSA dictionary, String encoding, char separator )
        throws UnsupportedEncodingException, IllegalArgumentException
    {
        // we don't really need the dictionary itself. A matcher will do.
        matcher = dictionary.getTraversalHelper();
        root    = dictionary.getStartNode();

        if (root==null)
            throw new IllegalArgumentException("Dictionary must have at least the root node.");

        // convert the separator character to a byte. Also, throw an
        // exception if the encoding is not found.
        this.encoding = encoding;
        this.separator = new String( new char [] { separator } ).getBytes(encoding)[0];
    }

    /**
     *  Creates a new object of this class using the given FSA for word lookups
     *  and encoding for converting characters to bytes, and additionally defines
     *  whether the dictionary uses prefixes or infixes compression.
     *
     *  @throws UnsupportedEncodingException if the given encoding
     *          has not been found in the system.
     *  @throws IllegalArgumentException if FSA's root node cannot be acquired
     *          (dictionary is empty).
     */
    
    public FSAStemmer( FSA dictionary, String encoding, char separator ,
    		boolean usesPrefixes, boolean usesInfixes)
    throws UnsupportedEncodingException, IllegalArgumentException
    {
    	this(dictionary, encoding, separator);
    	fsaPrefixes = usesPrefixes;
    	fsaInfixes = usesInfixes;    	
    }
    
    /**
     * Returns an array of pairs: (base form, tag), or an empty array
     * if the word is not found in the dictionary.
     * 
     * The tag is a simple string and depends on what was saved in the automaton.
     * (it may be nonsensical or <code>null</code>).
     *
     * @since 1.0.1
     */
    public String [] stemAndForm(final String word) {
    	return lookup(word, true);
    }

    /**
     * Returns an array of potential base forms of the word, or an empty array
     * if the word is not found in the dictionary.
     */
    public String [] stem( String word )
    {
    	return lookup(word, false);
    }

    /**
     * Searches the automaton for lemmas of the inflected form of a <code>word</code>.
     * The result is an array of lemmas or pairs of (lemma, tag), if
     * <code>returnForms</code> parameter is <code>true</code>. 
     */
    private String [] lookup(final String word, final boolean returnForms) {
        try
        {
            // try to find a partial match in the dictionary.
            final FSAMatch match = matcher.matchSequence(word.getBytes(encoding), root);

            if (match.getMatchResult() == FSAMatch.PREMATURE_WORD_END_FOUND)
            {
                // the entire sequence fit into the dictionary. Now a separator should be the next
                // character.
                final FSA.Arc arc = match.getMismatchNode().getArcLabelledWith(separator);

                // The situation when the arc points to a final node should NEVER happen. After all,
                // we want the word to have SOME base form.
                if (arc != null && !arc.isFinal())
                {
                    // there is such word in the dictionary. Return its base forms.
                    final ArrayList forms = new ArrayList(1);
                    final Iterator i = matcher.getAllSubsequences(arc.getDestinationNode());
                    while (i.hasNext())
                    {
                        final byte [] baseCompressed = (byte []) i.next();

                        // look for the delimiter of the 'grammar' tag in 
                        // the original Jan Daciuk's FSA format.
                        int j;
                        for (j=0; j < baseCompressed.length; j++) {
                            if (baseCompressed[j] == separator) 
                            	break;
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

                    return (String []) forms.toArray(new String [ forms.size() ]);
                }
            }
            else
            {
                // this case is somewhat confusing: we should have hit the separator first...
                // I don't really know how to deal with it at the time being.
            }

            return NO_STEM;
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("Unexpected Exception: " + e.toString());
        }
    }

    /**
     * Decode the base form of an inflected word.
     */
    private String decompress( String encodedBase, String inflected )
    {
        if (!fsaPrefixes && !fsaInfixes) {
        if (encodedBase.length() > 0 && Character.isUpperCase( encodedBase.charAt(0) ))
        {
            int stripAtEnd = (int) (encodedBase.charAt(0) - 'A');
            return inflected.substring(0, inflected.length() - stripAtEnd ) + encodedBase.substring(1);
        }
        else
        {
            // shouldn't happen, but if so, simply return the encodedBase
            return encodedBase;
        }
        } else if (fsaPrefixes && !fsaInfixes) {
        	if (encodedBase.length() > 1 && Character.isUpperCase( encodedBase.charAt(0) ))
            {
                int stripAtBeginning = (int) (encodedBase.charAt(0) - 'A');                
        		int stripAtEnd = (int) (encodedBase.charAt(1) - 'A');
                return inflected.substring(stripAtBeginning, inflected.length() - stripAtEnd ) + encodedBase.substring(2);
            }
            else
            {
                // shouldn't happen, but if so, simply return the encodedBase
                return encodedBase;
            }
        } else if (fsaInfixes) { //note: prefixes are silently assumed here
        	if (encodedBase.length() > 2 && Character.isUpperCase( encodedBase.charAt(0) ))
            {        		
        		int stripPosition = (int) (encodedBase.charAt(0) - 'A');
        		int stripAtBeginning = (int) (encodedBase.charAt(1) - 'A');                
        		int stripAtEnd = (int) (encodedBase.charAt(2) - 'A');      		
        		return inflected.substring(0, stripPosition) + inflected.substring(stripPosition + stripAtBeginning, inflected.length() - stripAtEnd) +  encodedBase.substring(3);        		
            }
            else
            {
                // shouldn't happen, but if so, simply return the encodedBase
                return encodedBase;
            }
        
        } else
         {
            // shouldn't happen, but if so, simply return the encodedBase
            return encodedBase;
        }
    }
}
