package morfologik.tools;

import java.io.IOException;

import com.carrotsearch.hppc.ByteArrayList;

import morfologik.fsa.FSA5;

/**
 * A class that converts tabular data to fsa morphological format. Three formats
 * are supported:
 * <ul>
 * <li><b>standard</b>, see {@link #standardEncode}</li>
 * <li><b>prefix</b>, see {@link #prefixEncode}</li>
 * <li><b>infix</b>, see {@link #infixEncode}</li>
 * </ul>
 */
public final class MorphEncoder {
	private final byte annotationSeparator;

	private final MorphEncoder2.IEncoder infixEncoder = new MorphEncoder2.TrimInfixAndSuffixEncoder();
	private final MorphEncoder2.IEncoder suffixEncoder = new MorphEncoder2.TrimSuffixEncoder();
	private final MorphEncoder2.IEncoder prefixEncoder = new MorphEncoder2.TrimPrefixAndSuffixEncoder();

	private final ByteArrayList src = new ByteArrayList();
	private final ByteArrayList dst = new ByteArrayList();
	private final ByteArrayList tmp = new ByteArrayList();

	public MorphEncoder() {
		this(FSA5.DEFAULT_ANNOTATION);
	}

	public MorphEncoder(byte annotationSeparator) {
		this.annotationSeparator = annotationSeparator;
	}

	/**
	 * This method converts the wordForm, wordLemma and tag to the form:
	 * 
	 * <pre>
	 * wordForm + Kending + tags
	 * </pre>
	 * 
	 * where '+' is a separator, K is a character that specifies how many
	 * characters should be deleted from the end of the inflected form to
	 * produce the lexeme by concatenating the stripped string with the ending.
	 */
	public byte[] standardEncode(byte[] wordForm, byte[] wordLemma, byte[] wordTag) throws IOException {
	    return encodeWith(suffixEncoder, wordForm, wordLemma, wordTag);
	}

    /**
	 * This method converts wordform, wordLemma and the tag to the form:
	 * <p>
	 * 
	 * <pre>
	 * inflected_form + LKending + tags
	 * </pre>
	 * <p>
	 * where '+' is a separator, L is the number of characters to be deleted
	 * from the beginning of the word ("A" means none, "B" means one, "C" - 2,
	 * etc.), K is a character that specifies how many characters should be
	 * deleted from the end of the inflected form to produce the lexeme by
	 * concatenating the stripped string with the ending ("A" means none,
	 * "B' - 1, "C" - 2, and so on).
	 * 
	 * @param wordForm
	 *            - inflected word form
	 * @param wordLemma
	 *            - canonical form
	 * @param wordTag
	 *            - tag
	 * @return the encoded string
	 */
	public byte[] prefixEncode(byte[] wordForm, byte[] wordLemma, byte[] wordTag) {
        return encodeWith(prefixEncoder, wordForm, wordLemma, wordTag);
	}

	/**
	 * This method converts wordform, wordLemma and the tag to the form:
	 * <pre>
	 * inflected_form + MLKending + tags
	 * </pre>
	 * <p>
	 * where '+' is a separator, M is the position of characters to be deleted
	 * towards the beginning of the inflected form ("A" means from the
	 * beginning, "B" from the second character, "C" - from the third one, and
	 * so on), L is the number of characters to be deleted from the position
	 * specified by M ("A" means none, "B" means one, "C" - 2, etc.), K is a
	 * character that specifies how many characters should be deleted from the
	 * end of the inflected form to produce the lexeme by concatenating the
	 * stripped string with the ending ("A" means none, "B' - 1, "C" - 2, and so
	 * on).
	 * 
	 * @param wordForm
	 *            - inflected word form
	 * @param wordLemma
	 *            - canonical form
	 * @param wordTag
	 *            - tag
	 * @return the encoded string
	 */
	public byte[] infixEncode(byte[] wordForm, byte[] wordLemma, byte[] wordTag) {
        return encodeWith(infixEncoder, wordForm, wordLemma, wordTag);
	}

    private byte [] encodeWith(MorphEncoder2.IEncoder encoder, 
        byte [] wordForm, byte [] wordLemma, byte [] wordTag)
    {
        src.clear(); 
        dst.clear(); 
        tmp.clear();
    
        tmp.add(wordForm);
        tmp.add(annotationSeparator);
        
        src.add(wordForm);
        dst.add(wordLemma);
        encoder.encode(src, dst, tmp);

        tmp.add(annotationSeparator);
        if (wordTag != null) {
    	    tmp.add(wordTag);
        }
    
        return tmp.toArray();
    }
}
