package morfologik.stemming;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import morfologik.fsa.*;

/**
 * This class implements a dictionary lookup over an FSA dictionary. The
 * dictionary for this class should be prepared from a text file using Jan
 * Daciuk's FSA package (see link below).
 * 
 * <p>
 * <b>Important:</b> finite state automatons in Jan Daciuk's implementation use
 * <em>bytes</em> not unicode characters. Therefore objects of this class always
 * have to be constructed with an encoding used to convert Java strings to byte
 * arrays and the other way around. You <b>can</b> use UTF-8 encoding, as it
 * should not conflict with any control sequences and separator characters.
 * 
 * @see <a href="http://www.eti.pg.gda.pl/~jandac/fsa.html">FSA package Web *
 *      site< /a>
 */
public final class DictionaryLookup implements IStemmer {
    /** An empty array used for 'no-matches' return. */
    private final static String[] NO_STEM = null;

    /** A Finite State Automaton used for look ups. */
    private final FSATraversalHelper matcher;

    /** FSA's root node. */
    private final FSA.Node root;

    /** Private internal reusable byte buffer for assembling strings. */
    private ByteBuffer bb = ByteBuffer.allocate(/* magic default */40);

    /** Private internal reusable array for assembling word forms. */
    private final ArrayList<String> forms = new ArrayList<String>(2);

    /**
     * Features of the compiled dictionary.
     * 
     * @see DictionaryMetadata
     */
    private final DictionaryMetadata dictionaryFeatures;

    /**
     * <p>
     * Creates a new object of this class using the given FSA for word lookups
     * and encoding for converting characters to bytes.
     * 
     * @throws IllegalArgumentException
     *             if FSA's root node cannot be acquired (dictionary is empty).
     */
    public DictionaryLookup(Dictionary dictionary)
	    throws IllegalArgumentException {
	this.dictionaryFeatures = dictionary.metadata;
	this.root = dictionary.fsa.getStartNode();
	this.matcher = dictionary.fsa.getTraversalHelper();

	if (root == null) {
	    throw new IllegalArgumentException(
		    "Dictionary must have at least the root node.");
	}

	if (dictionaryFeatures == null) {
	    throw new IllegalArgumentException(
		    "Dictionary metadata must not be null.");
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
     * <code>word</code>. The result is an array of lemmas or pairs of (lemma,
     * tag), if <code>returnForms</code> parameter is <code>true</code>.
     */
    private String[] lookup(final String word, final boolean returnForms) {
	final String encoding = dictionaryFeatures.encoding;
	final byte separator = dictionaryFeatures.separator;

	try {
	    // try to find a partial match in the dictionary.
	    final FSAMatch match = matcher.matchSequence(word
		    .getBytes(encoding), root);

	    if (match.getMatchType() == FSAMatchType.PREMATURE_WORD_END_FOUND) {
		/*
		 * The entire sequence fit into the dictionary. Now a separator
		 * should be the next character.
		 */
		final FSA.Arc arc = match.getMismatchNode().getArcLabelledWith(
			separator);

		/*
		 * The situation when the arc points to a final node should
		 * NEVER happen. After all, we want the word to have SOME base
		 * form.
		 */
		if (arc != null && !arc.isFinal()) {
		    // There is such word in the dictionary. Return its base forms.
		    forms.clear();
		    final Iterator<ByteBuffer> i = 
			matcher.getAllSubsequences(arc.getDestinationNode());

		    while (i.hasNext()) {
			final ByteBuffer bb = i.next();
			final byte [] ba = bb.array();
			final int bbSize = bb.remaining();

			/*
			 * Find the separator byte splitting word form and tag.
			 */
			int j = 0;
			for (; j < bbSize; j++) {
			    if (ba[j] == separator)
				break;
			}

			// Now, expand the prefix/ suffix 'compression' and
			// store the base form.
			forms.add(decompress(ba, j, word));

			// if needed, store the tag as well.
			if (returnForms) {
			    j = j + 1;
			    forms.add(new String(ba, j, bbSize - j, encoding));
			}
		    }

		    if (forms.size() == 0) {
			return NO_STEM;
		    }

		    return (String[]) forms.toArray(new String[forms.size()]);
		}
	    } else {
		/*
		 * this case is somewhat confusing: we should have hit the
		 * separator first... I don't really know how to deal with it at
		 * the time being.
		 */
	    }

	    return NO_STEM;
	} catch (UnsupportedEncodingException e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Decode the base form of an inflected word.
     */
    private String decompress(byte[] bytes, int len, String inflected) {
	final String encoding = this.dictionaryFeatures.encoding;
	final boolean fsaPrefixes = this.dictionaryFeatures.usesPrefixes;
	final boolean fsaInfixes = this.dictionaryFeatures.usesInfixes;

	try {
	    // Empty length? Weird, but return an empty string.
	    if (len == 0) {
		return "";
	    }

	    // Determine inflected string's length in bytes, in the same
	    // encoding.
	    final byte[] infBytes = inflected.getBytes(encoding);
	    final int infLen = infBytes.length;
	    final int code0 = bytes[0] - 'A';

	    // Increase buffer size, if needed.
	    bb.clear();
	    if (bb.capacity() < infLen + len) {
		bb = ByteBuffer.allocate(infLen + len);
	    }

	    if (code0 >= 0) {
		if (!fsaPrefixes && !fsaInfixes) {
		    if (code0 <= infLen) {
			bb.put(infBytes, 0, infLen - code0);
			bb.put(bytes, 1, len - 1);
			return new String(bb.array(), 0, bb.position(),
				encoding);
		    }
		} else if (fsaPrefixes && !fsaInfixes) {
		    if (len > 1) {
			final int stripAtEnd = bytes[1] - 'A' + code0;
			if (stripAtEnd <= infLen) {
			    bb.put(infBytes, code0, infLen - stripAtEnd);
			    bb.put(bytes, 2, len - 2);
			    return new String(bb.array(), 0, bb.position(),
				    encoding);
			}
		    }
		} else if (fsaInfixes) {
		    // Note: prefixes are silently assumed here
		    if (len > 2) {
			final int stripAtBeginning = bytes[1] - 'A' + code0;
			final int stripAtEnd = bytes[2] - 'A'
				+ stripAtBeginning;
			if (stripAtEnd <= infLen) {
			    bb.put(infBytes, 0, code0);
			    bb.put(infBytes, stripAtBeginning, infLen
				    - stripAtEnd);
			    bb.put(bytes, 3, len - 3);
			    return new String(bb.array(), 0, bb.position(),
				    encoding);
			}
		    }
		}
	    }

	    /*
	     * This is a fallback in case some junk is detected above. Return
	     * the base form only if this is the case.
	     */
	    return new String(bytes, 0, len, encoding);
	} catch (UnsupportedEncodingException e) {
	    throw new RuntimeException(e);
	}
    }
}
