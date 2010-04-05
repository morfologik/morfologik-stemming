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
public class FsaMorphCoder {

	static final char SEPARATOR = '+';
	
	private FsaMorphCoder() {
	// only static stuff
	};
	
	public static final int commonPrefix(final String s1, final String s2, 
			final int n)
	{
		for (int i = 0; i < n; i++)
			if (s1.charAt(i) != s2.charAt(i))
				return i;
		return n;
	}
	
	/** 
	 * This method converts the WordForm, wordLemma and tag to the form:<br/>
	 * wordForm+Kending+tags
	 * </br>
	 * where '+' is a separator, K is a character that specifies how 
	 * many characters should be deleted from the end of the inflected 
	 * form to produce the lexeme by concatenating the stripped string 
	 * with the ending.
	 * 
	 */
	public static final String standardEncode(final String wordForm, 
			final String wordLemma, final String wordTag) {		
		int l1 = wordForm.length();
		int prefix = commonPrefix(wordForm, wordLemma, wordForm.length());
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
	
}
