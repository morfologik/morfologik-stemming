package morfologik.fsa.morph;

import java.io.UnsupportedEncodingException;
import java.nio.charset.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;


/**
 * A class that converts tabular data to fsa morphological
 * format. Three formats are supported:
 * <ul>
 * <li>standard</li>
 * <li>prefix</li>
 * <li>infix</li>
 * </ul>
 * 
 * Note: if you are using UTF-8 encoding for the dictionary file, read the
 * text as ISO-8859-1 (or any other complete 8-bit code page), and then pass the 
 * resulting byte arrays to the methods. If you are worried about the coverage
 * of ISO-8859-1, note that it is not the same as ISO 8859-1 (that has only 191
 * characters) but an extended version with all 256 characters. 
 *  
 * You can use the .*UTF8 variants of all encoding methods if you are using
 * encoding directly for Java strings.
 * 
 */
public class FSAMorphCoder {

	static final char SEPARATOR = '+';
	static final int MAX_PREFIX_LEN = 3;
	static final int MAX_INFIX_LEN = 3;	
	
	private FSAMorphCoder() {
	// only static stuff
	};
	
	public static final int commonPrefix(final byte[] s1, final byte[] s2) {
		int maxLen = Math.min(s1.length, s2.length);
		for (int i = 0; i < maxLen; i++)
			if (s1[i] != s2[i])
				return i;
		return maxLen;
	}
	
	public static final byte[] substring(final byte[] bytes, final int start) {
		byte[] newArray = new byte[bytes.length - start];
		System.arraycopy(bytes, start, newArray, 0, bytes.length - start);
		return newArray;
	}

	/** 
	 * This method converts the wordForm, wordLemma and tag to the form:
	 * <pre>wordForm+Kending+tags</pre>
	 * where '+' is a separator, K is a character that specifies how 
	 * many characters should be deleted from the end of the inflected 
	 * form to produce the lexeme by concatenating the stripped string 
	 * with the ending.
	 * 
	 */
	public static final String standardEncode(final byte[] wordForm, 
			final byte[] wordLemma, final byte[] wordTag, final String encoding) {		
		int l1 = wordForm.length;
		int prefix = commonPrefix(wordForm, wordLemma);
		StringBuilder sb = new StringBuilder();
		sb.append(asString(wordForm, encoding));
		sb.append(SEPARATOR);
		if (prefix != 0) {								
			sb.append((char)((l1 - prefix + 65)&0xff));
			sb.append(asString(substring(wordLemma, prefix), encoding));
		} else {			  		 
			sb.append((char)((l1 + 65)&0xff));
			sb.append(asString(wordLemma, encoding));
		}
		sb.append(SEPARATOR);
		sb.append(asString(wordTag, encoding));
		return sb.toString();
	}
	
	/**
	 * This method converts wordform, wordLemma and the tag 
	 * to the form: <p>
 	 * <pre>inflected_form+LKending+tags</pre>
 	 * <p>
 	 * where '+' is a separator,
     * L is the number of characters to be deleted from the beginning of the word
     * ("A" means none, "B" means one, "C" - 2, etc.),
     * K is a character that specifies how many characters
     * should be deleted from the end of the inflected form to produce the lexeme
     * by concatenating the stripped string with the ending ("A" means none,
     * "B' - 1, "C" - 2, and so on).
	 * @param wordForm - inflected word form
	 * @param wordLemma - canonical form
	 * @param wordTag - tag
	 * @return the encoded string
	 */
	public static final String prefixEncode(final byte[] wordForm,
			final byte[] wordLemma, final byte[] wordTag, final String encoding) {
		int l1 = wordForm.length;
		int prefix = commonPrefix(wordForm, wordLemma);
		StringBuilder sb = new StringBuilder();
		sb.append(asString(wordForm, encoding));
		sb.append(SEPARATOR);
		if (prefix != 0) {
			sb.append('A');
			sb.append((char) ((l1 - prefix + 65) & 0xff));
			sb.append(asString(substring(wordLemma, prefix), encoding));
		} else {
			int prefixFound = 0;						
			int prefix1 = 0;
			final int max = Math.min(wordForm.length, MAX_PREFIX_LEN);
			for (int i = 1; i <= max; i++) {
				prefix1 = commonPrefix(substring(wordForm, i), wordLemma); 
				if (prefix1 > 2) {
					prefixFound = i;
					break;
				}
			}						
			if (prefixFound != 0) {
				sb.append((char) ((prefixFound + 65) & 0xff));				
				sb.append((char) ((l1 - prefixFound - prefix1 + 65) & 0xff));
				sb.append(asString(substring(wordLemma, prefix1), encoding));
			} else {
				sb.append('A');
				sb.append((char) ((l1 + 65) & 0xff));
				sb.append(asString(wordLemma, encoding));
			}
		}
		sb.append(SEPARATOR);
		sb.append(asString(wordTag, encoding));
		return sb.toString();
	}
	
	/**
	 *  This method converts wordform, wordLemma and the tag 
	 * to the form: <p>
 	 * <pre>inflected_form+MLKending+tags</pre>
 	 * <p>
 	 *  where '+' is a separator, M is the position of characters to be deleted
     *   towards the beginning of the inflected form ("A" means from the beginning,
     *   "B" from the second character, "C" - from the third one, and so on),
     *   L is the number of characters to be deleted from the position specified by M
     *   ("A" means none, "B" means one, "C" - 2, etc.),
     *   K is a character that specifies how many characters
     *   should be deleted from the end of the inflected form to produce the lexeme
     *   by concatenating the stripped string with the ending ("A" means none,
     *   "B' - 1, "C" - 2, and so on).
     *   
	 * @param wordForm - inflected word form
	 * @param wordLemma - canonical form
	 * @param wordTag - tag
	 * @return the encoded string
	 */
	public static final String infixEncode(final byte[] wordForm,
			final byte[] wordLemma, final byte[] wordTag, final String encoding) {
		int l1 = wordForm.length;		
		int prefixFound = 0;						
		int prefix1 = 0;
		int prefix = commonPrefix(wordForm, wordLemma);
		final int max = Math.min(l1, MAX_INFIX_LEN);
		StringBuilder sb = new StringBuilder();
		sb.append(asString(wordForm, encoding));
		sb.append(SEPARATOR);
		if (prefix != 0) { //prefix found but we have to check the infix
			
			for (int i = 1; i <= max; i++) {
				prefix1 = commonPrefix(substring(wordForm, i), wordLemma); 
				if (prefix1 > 2) {
					prefixFound = i;
					break;
				}
			}
			int prefix2 = 0;
			int infixFound = 0;
			int max2 = Math.min(l1 - prefix, MAX_INFIX_LEN);
			for (int i = 1; i <= max2; i++) {
				prefix2 = commonPrefix(substring(wordForm, prefix + i), 
						substring(wordLemma, prefix)); 
				if (prefix2 > 2) {
					infixFound = i;
					break;
				}
			}
			
			if (prefixFound > (infixFound)) {
				if (prefixFound > 0 && (prefix1 > prefix)) {
					sb.append('A');
					sb.append((char) ((prefixFound + 65) & 0xff));				
					sb.append((char) ((l1 - prefixFound - prefix1 + 65) & 0xff));
					sb.append(asString(substring(wordLemma, prefix1), encoding));
				} else {
					//infixFound == 0 && prefixFound == 0
					sb.append("AA");
					sb.append((char) ((l1 - prefix + 65) & 0xff));
					sb.append(asString(substring(wordLemma, prefix), encoding));
				}
			} else if (infixFound > 0 && prefix2 > 0) {
				//we have an infix, , and if there seems to be a prefix,
			    // the infix is longer
				sb.append((char) ((prefix + 65) &0xff));
				sb.append((char) ((infixFound + 65) &0xff));
				sb.append((char) ((l1 - prefix - prefix2 - infixFound + 65) &0xff));
				sb.append(asString(substring(wordLemma, prefix + prefix2), encoding));
			} else {
				// we have an infix, and if there seems to be a prefix,
			    // the infix is longer
			    // but the common prefix of two words is longer
				sb.append("AA");
				sb.append((char) ((l1 - prefix + 65) & 0xff));
				sb.append(asString(substring(wordLemma, prefix), encoding));
			}
			
		} else {
			//we may have a prefix
			for (int i = 1; i <= max; i++) {
				prefix1 = commonPrefix(substring(wordForm, i), wordLemma); 
				if (prefix1 > 2) {
					prefixFound = i;
					break;
				}
			}						
			if (prefixFound != 0) {
				sb.append('A');
				sb.append((char) ((prefixFound + 65) & 0xff));				
				sb.append((char) ((l1 - prefixFound - prefix1 + 65) & 0xff));
				sb.append(asString(substring(wordLemma, prefix1), encoding));
			} else {
				sb.append("AA");
				sb.append((char) ((l1 + 65) & 0xff));
				sb.append(asString(wordLemma, encoding));
			}
		}
		sb.append(SEPARATOR);
		sb.append(asString(wordTag, encoding));
		return sb.toString();
	}
		
	/**
	 * Converts a byte array to a given encoding.
	 * @param str
	 * 			Byte-array to be converted.
	 * @return
	 * 			Java String. If decoding is unsuccessful, the string is empty.
	 */
	public static String asString(final byte[] str, final String encoding) {		
		CharsetDecoder decoder = Charset.forName(encoding).newDecoder();
		try { 
			ByteBuffer bbuf = ByteBuffer.wrap(str); 
			CharBuffer cbuf = decoder.decode(bbuf); 			
			return cbuf.toString();
		} catch (CharacterCodingException e) 
		{ 

		}
		return "";
	}
	

	/** 
	 * A UTF-8 variant of {@link #standardEncode(wordForm, wordLemma, tag)}
	 * This method converts the wordForm, wordLemma and tag to the form:
	 * <pre>wordForm+Kending+tags</pre>
	 * where '+' is a separator, K is a character that specifies how 
	 * many characters should be deleted from the end of the inflected 
	 * form to produce the lexeme by concatenating the stripped string 
	 * with the ending.
	 * @throws UnsupportedEncodingException 
	 * 
	 */
	public static final String standardEncodeUTF8(final String wordForm, 
			final String wordLemma, final String wordTag) throws UnsupportedEncodingException {		
		return standardEncode(wordForm.getBytes("UTF-8"), 
				wordLemma.getBytes("UTF-8"),
				wordTag.getBytes("UTF-8"), "UTF-8");				
	}
	
	/**
	 * A UTF-8 variant of {@link #prefixEncode(wordForm, wordLemma, tag)}
	 * This method converts wordform, wordLemma and the tag 
	 * to the form: <p>
 	 * <pre>inflected_form+LKending+tags</pre>
 	 * <p>
 	 * where '+' is a separator,
     * L is the number of characters to be deleted from the beginning of the word
     * ("A" means none, "B" means one, "C" - 2, etc.),
     * K is a character that specifies how many characters
     * should be deleted from the end of the inflected form to produce the lexeme
     * by concatenating the stripped string with the ending ("A" means none,
     * "B' - 1, "C" - 2, and so on).
	 * @param wordForm - inflected word form
	 * @param wordLemma - canonical form
	 * @param wordTag - tag
	 * @return the encoded string
	 * @throws UnsupportedEncodingException 
	 */
	public static final String prefixEncodeUTF8(final String wordForm,
			final String wordLemma, final String wordTag) throws UnsupportedEncodingException {
		return prefixEncode(wordForm.getBytes("UTF-8"), 
				wordLemma.getBytes("UTF-8"),
				wordTag.getBytes("UTF-8"), "UTF-8");
	}

	/**
	 *  This method converts wordform, wordLemma and the tag 
	 * to the form: <p>
 	 * <pre>inflected_form+MLKending+tags</pre>
 	 * <p>
 	 *  where '+' is a separator, M is the position of characters to be deleted
     *   towards the beginning of the inflected form ("A" means from the beginning,
     *   "B" from the second character, "C" - from the third one, and so on),
     *   L is the number of characters to be deleted from the position specified by M
     *   ("A" means none, "B" means one, "C" - 2, etc.),
     *   K is a character that specifies how many characters
     *   should be deleted from the end of the inflected form to produce the lexeme
     *   by concatenating the stripped string with the ending ("A" means none,
     *   "B' - 1, "C" - 2, and so on).
     *   
	 * @param wordForm - inflected word form
	 * @param wordLemma - canonical form
	 * @param wordTag - tag
	 * @return the encoded string
	 * @throws UnsupportedEncodingException 
	 */
	public static final String infixEncodeUTF8(final String wordForm,
			final String wordLemma, final String wordTag) throws UnsupportedEncodingException {
		return infixEncode(wordForm.getBytes("UTF-8"), 
				wordLemma.getBytes("UTF-8"),
				wordTag.getBytes("UTF-8"), "UTF-8");
	}
}
