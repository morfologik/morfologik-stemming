package morfologik.tools;

import java.io.UnsupportedEncodingException;

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

	private static final int MAX_PREFIX_LEN = 3;
	private static final int MAX_INFIX_LEN = 3;

	private static final String UTF8 = "UTF-8";	

	public MorphEncoder() {
		this(FSA5.DEFAULT_ANNOTATION);
	}

	public MorphEncoder(byte annotationSeparator) {
		this.annotationSeparator = annotationSeparator;
	}
	
	public static int commonPrefix(final byte[] s1, final byte[] s2) {
		final int maxLen = Math.min(s1.length, s2.length);
		for (int i = 0; i < maxLen; i++) {
	        if (s1[i] != s2[i]) {
	            return i;
            }
        }
		return maxLen;
	}

	private static byte[] subsequence(final byte[] bytes, final int start) {
		final byte[] newArray = new byte[bytes.length - start];
		System.arraycopy(bytes, start, newArray, 0, bytes.length - start);
		return newArray;
	}

	private static int copyTo(byte[] dst, final int pos, final byte[] src) {
		System.arraycopy(src, 0, dst, pos, src.length);
		return src.length;
	}
	
	private static int copyTo(byte[] dst, final int pos, final byte src) {
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
	public byte[] standardEncode(final byte[] wordForm,
	        final byte[] wordLemma, final byte[] wordTag) {
		final int l1 = wordForm.length;
		final int prefix = commonPrefix(wordForm, wordLemma);
		final int len = wordLemma.length - prefix;
		int pos = 0;		
		// 3 = 2 separators and K character
		int arrayLen = l1 + len + 3;		
		if (wordTag != null) { //wordTag may be empty for stemming
			arrayLen += wordTag.length;
		}		
		final byte[] bytes = new byte[arrayLen]; 
		pos += copyTo(bytes, pos, wordForm);
		pos += copyTo(bytes, pos, annotationSeparator);
		if (prefix == 0) {
			pos += copyTo(bytes, pos, (byte) ((l1 + 65) & 0xff));
			pos += copyTo(bytes, pos, wordLemma);
		} else {
			pos += copyTo(bytes, pos, (byte) ((l1 - prefix + 65) & 0xff));
			pos += copyTo(bytes, pos, subsequence(wordLemma, prefix));
		}
		pos += copyTo(bytes, pos, annotationSeparator);
		if (wordTag != null) {
			pos += copyTo(bytes, pos, wordTag);
		}
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
	public byte[] prefixEncode(final byte[] wordForm,
	        final byte[] wordLemma, final byte[] wordTag) {
		final int l1 = wordForm.length;
		final int prefix = commonPrefix(wordForm, wordLemma);
		
		// 4 = 2 separators + LK characters
		int arrayLen = l1 + wordLemma.length + 4;
		if (wordTag != null) {
			arrayLen += wordTag.length;
		}
		final byte[] bytes = new byte[arrayLen]; 
		int pos = 0;
		pos += copyTo(bytes, pos, wordForm);
		pos += copyTo(bytes, pos, annotationSeparator);
		if (prefix == 0) {
			int prefixFound = 0;
			int prefix1 = 0;
			final int max = Math.min(wordForm.length, MAX_PREFIX_LEN);
			for (int i = 1; i <= max; i++) {
				prefix1 = commonPrefix(subsequence(wordForm, i), wordLemma);
				if (prefix1 > 2) {
					prefixFound = i;
					break;
				}
			}
			if (prefixFound == 0) {
				pos += copyTo(bytes, pos, (byte) 'A');
				pos += copyTo(bytes, pos, (byte) ((l1 + 65) & 0xff));
				pos += copyTo(bytes, pos, wordLemma);
			} else {
				pos += copyTo(bytes, pos, (byte) ((prefixFound + 65) & 0xff));
				pos += copyTo(bytes, pos,
				        (byte) ((l1 - prefixFound - prefix1 + 65) & 0xff));
				pos += copyTo(bytes, pos, subsequence(wordLemma, prefix1));
			}
		} else {
			pos += copyTo(bytes, pos, (byte) 'A');
			pos += copyTo(bytes, pos, (byte) ((l1 - prefix + 65) & 0xff));
			pos += copyTo(bytes, pos, subsequence(wordLemma, prefix));
		}
		pos += copyTo(bytes, pos, annotationSeparator);
		if (wordTag != null) {
			pos += copyTo(bytes, pos, wordTag);
		}
		final byte[] finalArray = new byte[pos];
		System.arraycopy(bytes, 0, finalArray, 0, pos);
		return finalArray;
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
	public byte[] infixEncode(final byte[] wordForm,
	        final byte[] wordLemma, final byte[] wordTag) {
		final int l1 = wordForm.length;
		int prefixFound = 0;
		int prefix1 = 0;
		final int prefix = commonPrefix(wordForm, wordLemma);
		final int max = Math.min(l1, MAX_INFIX_LEN);

		// 5 = 2 separators + MLK characters		
		int arrayLen = l1 + wordLemma.length + 5;
		if (wordTag != null) {
			arrayLen += wordTag.length;
		}
		final byte[] bytes = new byte[arrayLen];
		int pos = 0;
		pos += copyTo(bytes, pos, wordForm);
		pos += copyTo(bytes, pos, annotationSeparator);
		if (prefix == 0) {
			// we may have a prefix
			for (int i = 1; i <= max; i++) {
				prefix1 = commonPrefix(subsequence(wordForm, i), wordLemma);
				if (prefix1 > 2) {
					prefixFound = i;
					break;
				}
			}
			if (prefixFound == 0) {
				pos += copyTo(bytes, pos, (byte) 'A');
				pos += copyTo(bytes, pos, (byte) 'A');
				pos += copyTo(bytes, pos, (byte) ((l1 + 65) & 0xff));
				pos += copyTo(bytes, pos, wordLemma);
			} else {
				pos += copyTo(bytes, pos, (byte) 'A');
				pos += copyTo(bytes, pos, (byte) ((prefixFound + 65) & 0xff));
				pos += copyTo(bytes, pos,
				        (byte) ((l1 - prefixFound - prefix1 + 65) & 0xff));
				pos += copyTo(bytes, pos, subsequence(wordLemma, prefix1));
			}
		} else { // prefix found but we have to check the infix

			for (int i = 1; i <= max; i++) {
				prefix1 = commonPrefix(subsequence(wordForm, i), wordLemma);
				if (prefix1 > 2) {
					prefixFound = i;
					break;
				}
			}
			int prefix2 = 0;
			int infixFound = 0;
			final int max2 = Math.min(l1 - prefix, MAX_INFIX_LEN);
			for (int i = 1; i <= max2; i++) {
				prefix2 = commonPrefix(subsequence(wordForm, prefix + i),
				        subsequence(wordLemma, prefix));
				if (prefix2 > 2) {
					infixFound = i;
					break;
				}
			}

			if (prefixFound > infixFound) {
				if (prefixFound > 0 && (prefix1 > prefix)) {
					pos += copyTo(bytes, pos, (byte) 'A');
					pos += copyTo(bytes, pos,
					        (byte) ((prefixFound + 65) & 0xff));
					pos += copyTo(bytes, pos, (byte) ((l1 - prefixFound
					        - prefix1 + 65) & 0xff));
					pos += copyTo(bytes, pos, subsequence(wordLemma, prefix1));
				} else {
					// infixFound == 0 && prefixFound == 0
					pos += copyTo(bytes, pos, (byte) 'A');
					pos += copyTo(bytes, pos, (byte) 'A');
					pos += copyTo(bytes, pos,
					        (byte) ((l1 - prefix + 65) & 0xff));
					pos += copyTo(bytes, pos, subsequence(wordLemma, prefix));
				}
			} else if (infixFound > 0 && prefix2 > 0) {
				// we have an infix, , and if there seems to be a prefix,
				// the infix is longer
				pos += copyTo(bytes, pos, (byte) ((prefix + 65) & 0xff));
				pos += copyTo(bytes, pos, (byte) ((infixFound + 65) & 0xff));
				pos += copyTo(bytes, pos, (byte) ((l1 - prefix - prefix2
				        - infixFound + 65) & 0xff));
				pos += copyTo(bytes, pos,
				        subsequence(wordLemma, prefix + prefix2));
			} else {
				// we have an infix, and if there seems to be a prefix,
				// the infix is longer
				// but the common prefix of two words is longer
				pos += copyTo(bytes, pos, (byte) 'A');
				pos += copyTo(bytes, pos, (byte) 'A');
				pos += copyTo(bytes, pos, (byte) ((l1 - prefix + 65) & 0xff));
				pos += copyTo(bytes, pos, subsequence(wordLemma, prefix));
			}

		}
		pos += copyTo(bytes, pos, annotationSeparator);
		if (wordTag != null) {
			pos += copyTo(bytes, pos, wordTag);
		}
		final byte[] finalArray = new byte[pos];
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
	protected static String asString(final byte[] str, final String encoding) {
		try {
	        return new String(str, encoding);
        } catch (UnsupportedEncodingException e) {
	        return "";
        }
	}

	/**
	 * A UTF-8 variant of {@link #standardEncode(byte[], byte[], byte[])} This
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
	 */
	public String standardEncodeUTF8(final String wordForm,
	        final String wordLemma, final String wordTag)
	        throws UnsupportedEncodingException {
		return asString(standardEncode(wordForm.getBytes(UTF8), wordLemma
		        .getBytes(UTF8), wordTag.getBytes(UTF8)), UTF8);
	}

	/**
	 * A UTF-8 variant of {@link #prefixEncode(byte[], byte[], byte[])} This
	 * method converts wordform, wordLemma and the tag to the form:
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
	public String prefixEncodeUTF8(final String wordForm,
	        final String wordLemma, final String wordTag)
	        throws UnsupportedEncodingException {
		return asString(prefixEncode(wordForm.getBytes(UTF8), wordLemma
		        .getBytes(UTF8), wordTag.getBytes(UTF8)), UTF8);
	}

	/**
	 * A UTF-8 variant of {@link #infixEncode(byte[], byte[], byte[])}.
	 * 
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
	 * @throws UnsupportedEncodingException
	 */
	public String infixEncodeUTF8(final String wordForm,
	        final String wordLemma, final String wordTag)
	        throws UnsupportedEncodingException {
		return asString(infixEncode(wordForm.getBytes(UTF8), wordLemma
		        .getBytes(UTF8), wordTag.getBytes(UTF8)), UTF8);
	}
}
