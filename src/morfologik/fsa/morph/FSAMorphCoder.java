package morfologik.fsa.morph;

/**
 * A class that converts tabular data to fsa morphological
 * format. Three formats are supported:
 * <ul>
 * <li>standard</li>
 * <li>prefix</li>
 * <li>infix</li>
 * </ul>
 */
public class FSAMorphCoder {

	static final char SEPARATOR = '+';
	static final int MAX_PREFIX_LEN = 3;
	static final int MAX_INFIX_LEN = 3;	
	
	private FSAMorphCoder() {
	// only static stuff
	};
	
	public static final int commonPrefix(final String s1, final String s2) {
		int maxLen = Math.min(s1.length(), s2.length());
		for (int i = 0; i < maxLen; i++)
			if (s1.charAt(i) != s2.charAt(i))
				return i;
		return maxLen;
	}	

	/** 
	 * This method converts the WordForm, wordLemma and tag to the form:
	 * <pre>wordForm+Kending+tags</pre>
	 * where '+' is a separator, K is a character that specifies how 
	 * many characters should be deleted from the end of the inflected 
	 * form to produce the lexeme by concatenating the stripped string 
	 * with the ending.
	 * 
	 */
	public static final String standardEncode(final String wordForm, 
			final String wordLemma, final String wordTag) {		
		int l1 = wordForm.length();
		int prefix = commonPrefix(wordForm, wordLemma);
		StringBuilder sb = new StringBuilder();
		sb.append(wordForm);
		sb.append(SEPARATOR);
		if (prefix != 0) {								
			sb.append((char)((l1 - prefix + 65)&0xff));
			sb.append(wordLemma.substring(prefix));
		} else {			  		 
			sb.append((char)((l1 + 65)&0xff));
			sb.append(wordLemma);
		}
		sb.append(SEPARATOR);
		sb.append(wordTag);
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
	public static final String prefixEncode(final String wordForm,
			final String wordLemma, final String wordTag) {
		int l1 = wordForm.length();
		int prefix = commonPrefix(wordForm, wordLemma);
		StringBuilder sb = new StringBuilder();
		sb.append(wordForm);
		sb.append(SEPARATOR);
		if (prefix != 0) {
			sb.append('A');
			sb.append((char) ((l1 - prefix + 65) & 0xff));
			sb.append(wordLemma.substring(prefix));
		} else {
			int prefixFound = 0;						
			int prefix1 = 0;
			final int max = Math.min(wordForm.length(), MAX_PREFIX_LEN);
			for (int i = 1; i <= max; i++) {
				prefix1 = commonPrefix(wordForm.substring(i), wordLemma); 
				if (prefix1 > 2) {
					prefixFound = i;
					break;
				}
			}						
			if (prefixFound != 0) {
				sb.append((char) ((prefixFound + 65) & 0xff));				
				sb.append((char) ((l1 - prefixFound - prefix1 + 65) & 0xff));
				sb.append(wordLemma.substring(prefix1));
			} else {
				sb.append('A');
				sb.append((char) ((l1 + 65) & 0xff));
				sb.append(wordLemma);
			}
		}
		sb.append(SEPARATOR);
		sb.append(wordTag);
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
	public static final String infixEncode(final String wordForm,
			final String wordLemma, final String wordTag) {
		int l1 = wordForm.length();		
		int prefixFound = 0;						
		int prefix1 = 0;
		int prefix = commonPrefix(wordForm, wordLemma);
		final int max = Math.min(wordForm.length(), MAX_INFIX_LEN);
		StringBuilder sb = new StringBuilder();
		sb.append(wordForm);
		sb.append(SEPARATOR);
		if (prefix != 0) { //prefix found but we have to check the infix
			
			for (int i = 1; i <= max; i++) {
				prefix1 = commonPrefix(wordForm.substring(i), wordLemma); 
				if (prefix1 > 2) {
					prefixFound = i;
					break;
				}
			}
			int prefix2 = 0;
			int infixFound = 0;
			int max2 = Math.min(wordForm.length() - prefix, MAX_INFIX_LEN);
			for (int i = 1; i <= max2; i++) {
				prefix2 = commonPrefix(wordForm.substring(prefix + i), 
						wordLemma.substring(prefix)); 
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
					sb.append(wordLemma.substring(prefix1));
				} else {
					//infixFound == 0 && prefixFound == 0
					sb.append("AA");
					sb.append((char) ((l1 - prefix + 65) & 0xff));
					sb.append(wordLemma.substring(prefix));
				}
			} else if (infixFound > 0 && prefix2 > 0) {
				//we have an infix, , and if there seems to be a prefix,
			    // the infix is longer
				sb.append((char) ((prefix + 65) &0xff));
				sb.append((char) ((infixFound + 65) &0xff));
				sb.append((char) ((l1 - prefix - prefix2 - infixFound + 65) &0xff));
				sb.append(wordLemma.substring(prefix + prefix2));
			} else {
				// we have an infix, and if there seems to be a prefix,
			    // the infix is longer
			    // but the common prefix of two words is longer
				sb.append("AA");
				sb.append((char) ((l1 - prefix + 65) & 0xff));
				sb.append(wordLemma.substring(prefix));
			}
			
		} else {
			//we may have a prefix
			for (int i = 1; i <= max; i++) {
				prefix1 = commonPrefix(wordForm.substring(i), wordLemma); 
				if (prefix1 > 2) {
					prefixFound = i;
					break;
				}
			}						
			if (prefixFound != 0) {
				sb.append('A');
				sb.append((char) ((prefixFound + 65) & 0xff));				
				sb.append((char) ((l1 - prefixFound - prefix1 + 65) & 0xff));
				sb.append(wordLemma.substring(prefix1));
			} else {
				sb.append("AA");
				sb.append((char) ((l1 + 65) & 0xff));
				sb.append(wordLemma);
			}
		}
		sb.append(SEPARATOR);
		sb.append(wordTag);
		return sb.toString();
	}
}
