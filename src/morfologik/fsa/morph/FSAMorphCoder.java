package morfologik.fsa.morph;

import java.io.UnsupportedEncodingException;
import java.nio.charset.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * A class that converts tabular data to fsa morphological format. Three formats
 * are supported:
 * <ul>
 * <li>standard</li>
 * <li>prefix</li>
 * <li>infix</li>
 * </ul>
 * 
 *
 * 
 */
public class FSAMorphCoder {

	static final byte SEPARATOR = (byte)'+';
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

	static final int copyTo(byte[] dst, final int pos, byte[] src) {
		System.arraycopy(src, 0, dst, pos, src.length);
		return src.length;
	}
	
	static final int copyTo(byte[] dst, final int pos, byte src) {
		byte[] single = new byte[1];
		single[0] = src;
		System.arraycopy(single, 0, dst, pos, 1);
		return 1;
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
	 * 
	 */
	public static final byte[] standardEncode(final byte[] wordForm,
	        final byte[] wordLemma, final byte[] wordTag) {
		int l1 = wordForm.length;
		int prefix = commonPrefix(wordForm, wordLemma);
		int len = wordLemma.length;
		int pos = 0;
		if (prefix != 0) {
			len = len - prefix;
		}
		byte[] bytes = new byte[wordForm.length + len +  wordTag.length
		        + 3]; // 2 separators and K character
		pos += copyTo(bytes, pos, wordForm);		 
		pos += copyTo(bytes, pos, SEPARATOR);
		if (prefix != 0) {			
			pos += copyTo(bytes, pos, (byte) ((l1 - prefix + 65) & 0xff));
			pos += copyTo(bytes, pos, substring(wordLemma, prefix));
		} else {
			pos += copyTo(bytes, pos, (byte) ((l1 + 65) & 0xff));
			pos += copyTo(bytes, pos, wordLemma);
		}
		pos += copyTo(bytes, pos, SEPARATOR);
		pos += copyTo(bytes, pos, wordTag);			
		return bytes;
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
	public static final byte[] prefixEncode(final byte[] wordForm,
	        final byte[] wordLemma, final byte[] wordTag) {
		int l1 = wordForm.length;
		int prefix = commonPrefix(wordForm, wordLemma);
		byte[] bytes = new byte[wordForm.length + wordLemma.length +  wordTag.length
			        + 4]; // 2 separators + LK characters
		int pos = 0;
		pos += copyTo(bytes, pos, wordForm);
		pos += copyTo(bytes, pos, SEPARATOR);
		if (prefix != 0) {
			pos += copyTo(bytes, pos, (byte) 'A');
			pos += copyTo(bytes, pos, (byte) ((l1 - prefix + 65) & 0xff));
			pos += copyTo(bytes, pos, substring(wordLemma, prefix));
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
				pos += copyTo(bytes, pos, (byte) ((prefixFound + 65) & 0xff));
				pos += copyTo(bytes, pos, (byte) ((l1 - prefixFound - prefix1 + 65) & 0xff));
				pos += copyTo(bytes, pos, substring(wordLemma, prefix1));
			} else {
				pos += copyTo(bytes, pos, (byte)'A');
				pos += copyTo(bytes, pos, (byte) ((l1 + 65) & 0xff));
				pos += copyTo(bytes, pos, wordLemma);
			}
		}
		pos += copyTo(bytes, pos, SEPARATOR);
		pos += copyTo(bytes, pos, wordTag);
		byte[] finalArray = new byte[pos];
		System.arraycopy(bytes, 0, finalArray, 0, pos);		
		return finalArray;
	}

	/**
	 * This method converts wordform, wordLemma and the tag to the form:
	 * <p>
	 * 
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
	public static final byte[] infixEncode(final byte[] wordForm,
	        final byte[] wordLemma, final byte[] wordTag) {
		int l1 = wordForm.length;
		int prefixFound = 0;
		int prefix1 = 0;
		int prefix = commonPrefix(wordForm, wordLemma);
		final int max = Math.min(l1, MAX_INFIX_LEN);
		byte[] bytes = new byte[wordForm.length + wordLemma.length +  wordTag.length
		    			        + 5]; // 2 separators + MLK characters
		int pos = 0;
		pos += copyTo(bytes, pos, wordForm);		
		pos += copyTo(bytes, pos, SEPARATOR);
		if (prefix != 0) { // prefix found but we have to check the infix

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
					pos += copyTo(bytes, pos, (byte)'A');
					pos += copyTo(bytes, pos, (byte) ((prefixFound + 65) & 0xff));
					pos += copyTo(bytes, pos, (byte) ((l1 - prefixFound - prefix1 + 65) & 0xff));
					pos += copyTo(bytes, pos, substring(wordLemma, prefix1));
				} else {
					// infixFound == 0 && prefixFound == 0
					pos += copyTo(bytes, pos, (byte)'A');
					pos += copyTo(bytes, pos, (byte)'A');
					pos += copyTo(bytes, pos, (byte) ((l1 - prefix + 65) & 0xff));
					pos += copyTo(bytes, pos, substring(wordLemma, prefix));
				}
			} else if (infixFound > 0 && prefix2 > 0) {
				// we have an infix, , and if there seems to be a prefix,
				// the infix is longer
				pos += copyTo(bytes, pos, (byte) ((prefix + 65) & 0xff));
				pos += copyTo(bytes, pos, (byte) ((infixFound + 65) & 0xff));
				pos += copyTo(bytes, pos, (byte) ((l1 - prefix - prefix2 - infixFound + 65) & 0xff));
				pos += copyTo(bytes, pos, substring(wordLemma, prefix + prefix2));
			} else {
				// we have an infix, and if there seems to be a prefix,
				// the infix is longer
				// but the common prefix of two words is longer
				pos += copyTo(bytes, pos, (byte)'A');
				pos += copyTo(bytes, pos, (byte)'A');
				pos += copyTo(bytes, pos, (byte) ((l1 - prefix + 65) & 0xff));
				pos += copyTo(bytes, pos, substring(wordLemma, prefix));
			}

		} else {
			// we may have a prefix
			for (int i = 1; i <= max; i++) {
				prefix1 = commonPrefix(substring(wordForm, i), wordLemma);
				if (prefix1 > 2) {
					prefixFound = i;
					break;
				}
			}
			if (prefixFound != 0) {
				pos += copyTo(bytes, pos, (byte)'A');
				pos += copyTo(bytes, pos, (byte)((prefixFound + 65) & 0xff));
				pos += copyTo(bytes, pos, (byte)((l1 - prefixFound - prefix1 + 65) & 0xff));
				pos += copyTo(bytes, pos, substring(wordLemma, prefix1));
			} else {
				pos += copyTo(bytes, pos, (byte)'A');
				pos += copyTo(bytes, pos, (byte)'A');
				pos += copyTo(bytes, pos, (byte)((l1 + 65) & 0xff));
				pos += copyTo(bytes, pos, wordLemma);
			}
		}
		pos += copyTo(bytes, pos, SEPARATOR);
		pos += copyTo(bytes, pos, wordTag);
		byte[] finalArray = new byte[pos];
		System.arraycopy(bytes, 0, finalArray, 0, pos);		
		return finalArray;
	}

	/**
	 * Converts a byte array to a given encoding.
	 * 
	 * @param str
	 *            Byte-array to be converted.
	 * @return Java String. If decoding is unsuccessful, the string is empty.
	 */
	public static String asString(final byte[] str, final String encoding) {
		CharsetDecoder decoder = Charset.forName(encoding).newDecoder();
		try {
			ByteBuffer bbuf = ByteBuffer.wrap(str);
			CharBuffer cbuf = decoder.decode(bbuf);
			return cbuf.toString();
		} catch (CharacterCodingException e) {

		}
		return "";
	}

	/**
	 * A UTF-8 variant of {@link #standardEncode(wordForm, wordLemma, tag)} This
	 * method converts the wordForm, wordLemma and tag to the form:
	 * 
	 * <pre>
	 * wordForm + Kending + tags
	 * </pre>
	 * 
	 * where '+' is a separator, K is a character that specifies how many
	 * characters should be deleted from the end of the inflected form to
	 * produce the lexeme by concatenating the stripped string with the ending.
	 * 
	 * @throws UnsupportedEncodingException
	 * 
	 */
	public static final String standardEncodeUTF8(final String wordForm,
	        final String wordLemma, final String wordTag)
	        throws UnsupportedEncodingException {
		return asString(standardEncode(wordForm.getBytes("UTF-8"), wordLemma
		        .getBytes("UTF-8"), wordTag.getBytes("UTF-8")), "UTF-8");
	}

	/**
	 * A UTF-8 variant of {@link #prefixEncode(wordForm, wordLemma, tag)} This
	 * method converts wordform, wordLemma and the tag to the form:
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
	 * @throws UnsupportedEncodingException
	 */
	public static final String prefixEncodeUTF8(final String wordForm,
	        final String wordLemma, final String wordTag)
	        throws UnsupportedEncodingException {
		return asString(prefixEncode(wordForm.getBytes("UTF-8"), wordLemma
		        .getBytes("UTF-8"), wordTag.getBytes("UTF-8")), "UTF-8");
	}

	/**
	 * This method converts wordform, wordLemma and the tag to the form:
	 * <p>
	 * 
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
	 * @throws UnsupportedEncodingException
	 */
	public static final String infixEncodeUTF8(final String wordForm,
	        final String wordLemma, final String wordTag)
	        throws UnsupportedEncodingException {
		return asString(infixEncode(wordForm.getBytes("UTF-8"), wordLemma
		        .getBytes("UTF-8"), wordTag.getBytes("UTF-8")), "UTF-8");
	}
}
