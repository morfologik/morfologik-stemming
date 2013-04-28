package morfologik.speller;

import static morfologik.fsa.MatchResult.EXACT_MATCH;
import static morfologik.fsa.MatchResult.SEQUENCE_IS_A_PREFIX;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnsupportedCharsetException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import morfologik.fsa.FSA;
import morfologik.fsa.FSATraversal;
import morfologik.fsa.MatchResult;
import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryMetadata;
import morfologik.util.BufferUtils;

/**
 * Find suggestions by using K. Oflazer's algorithm. See Jan Daciuk's <code>s_fsa</code>
 * package.
 */
public class Speller {

	private final int editDistance;
	private int e_d; // effective edit distance

	private final HMatrix H;

	public static int MAX_WORD_LENGTH = 120;

	private char[] candidate; /* current replacement */
	private int candLen;
	private int wordLen; /* length of word being processed */
	private char[] word_ff; /* word being processed */
	
	/**
	 * List of candidate strings, including same additional data such as 
	 * edit distance from the original word.
	 */
	private final List<CandidateData> candidates = new ArrayList<CandidateData>();	
	
	private boolean containsSeparators = true;

	/**
	 * Internal reusable buffer for encoding words into byte arrays using
	 * {@link #encoder}.
	 */
	private ByteBuffer byteBuffer = ByteBuffer.allocate(MAX_WORD_LENGTH);
	
	/**
	 * Internal reusable buffer for encoding words into byte arrays using
	 * {@link #encoder}.
	 */
	private CharBuffer charBuffer = CharBuffer.allocate(MAX_WORD_LENGTH);

	/**
	 * Reusable match result.
	 */
	private final MatchResult matchResult = new MatchResult();

	/**
	 * Features of the compiled dictionary.
	 * 
	 * @see DictionaryMetadata
	 */
	private final DictionaryMetadata dictionaryMetadata;
		
	/**
	 * Charset encoder for the FSA.
	 */
	private final CharsetEncoder encoder;

	/**
	 * Charset decoder for the FSA.
	 */
	protected final CharsetDecoder decoder;

	/** An FSA used for lookups. */
	private final FSATraversal matcher;

	/** FSA's root node. */
	private final int rootNode;

	/**
	 * The FSA we are using.
	 */
	protected final FSA fsa;

	public Speller(final Dictionary dictionary) {
		this(dictionary, 1);
	}

	public Speller(final Dictionary dictionary, final int editDistance) {
		this(dictionary, editDistance, true);
	}

	public Speller(final Dictionary dictionary, final int editDistance, final boolean convertCase) {		

	    this.editDistance = editDistance;
        H = new HMatrix(editDistance, MAX_WORD_LENGTH);
	    
		this.dictionaryMetadata = dictionary.metadata;
		this.rootNode = dictionary.fsa.getRootNode();
		this.fsa = dictionary.fsa;
		this.matcher = new FSATraversal(fsa);
		
		if (rootNode == 0) {
			throw new IllegalArgumentException(
					"Dictionary must have at least the root node.");
		}

		if (dictionaryMetadata == null) {
			throw new IllegalArgumentException(
					"Dictionary metadata must not be null.");
		}
		
		
		try {
			final Charset charset = Charset.forName(dictionaryMetadata.encoding);
			encoder = charset.newEncoder();
			decoder = charset.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);
		} catch (final UnsupportedCharsetException e) {
			throw new RuntimeException(
					"FSA's encoding charset is not supported: "
							+ dictionaryMetadata.encoding);
		}

		try {
			final CharBuffer decoded = decoder.decode(ByteBuffer
					.wrap(new byte[] { dictionaryMetadata.separator }));
			if (decoded.remaining() != 1) {
				throw new RuntimeException(
						"FSA's separator byte takes more than one character after conversion "
								+ " of byte 0x"
								+ Integer
								.toHexString(dictionaryMetadata.separator)
								+ " using encoding "
								+ dictionaryMetadata.encoding);
			}			
		} catch (final CharacterCodingException e) {
			throw new RuntimeException(
					"FSA's separator character cannot be decoded from byte value 0x"
							+ Integer.toHexString(dictionaryMetadata.separator)
							+ " using encoding " + dictionaryMetadata.encoding,
							e);
		}								
				
	}

	/**
	 * Encode a character sequence into a byte buffer, optionally expanding
	 * buffer.
	 */
	private ByteBuffer charsToBytes(final CharBuffer chars, ByteBuffer bytes) {
		bytes.clear();
		final int maxCapacity = (int) (chars.remaining() * encoder
				.maxBytesPerChar());
		if (bytes.capacity() <= maxCapacity) {
			bytes = ByteBuffer.allocate(maxCapacity);
		}
		chars.mark();
		encoder.reset();
		if (encoder.encode(chars, bytes, true).
		        isError()) { // in the case of encoding errors, clear the buffer
		    bytes.clear();
		}
		bytes.flip();
		chars.reset();
		return bytes;
	}

	
	private ByteBuffer CharSequenceToBytes(final CharSequence word) {
	 // Encode word characters into bytes in the same encoding as the FSA's.
        charBuffer.clear();
        charBuffer = BufferUtils.ensureCapacity(charBuffer, word.length());
        for (int i = 0; i < word.length(); i++) {
            final char chr = word.charAt(i);
            charBuffer.put(chr);
        }
        charBuffer.flip();
        byteBuffer = charsToBytes(charBuffer, byteBuffer);
        return byteBuffer;
	}
	
	public boolean isMisspelled(String word) {
	      boolean isAlphabetic = true;
	      if (word.length() == 1) { // dictionaries usually do not contain punctuation
	        isAlphabetic = isAlphabetic(word.charAt(0));
	      }
	return word.length() > 0
	      && (!dictionaryMetadata.ignorePunctuation || isAlphabetic)
	      && (!dictionaryMetadata.ignoreNumbers || !containsDigit(word))	              
          && !isInDictionary(word)	              
          && (!dictionaryMetadata.convertCase || 
          !(!isMixedCase(word) 
            && isInDictionary(word.toLowerCase(dictionaryMetadata.dictionaryLocale)))); 
	      
	}

	
	/**
	 * Test whether the word is found in the dictionary. 
	 * @param word - the word to be tested
	 * @return - true if it is found.
	 */
	public boolean isInDictionary(final CharSequence word) {
	    
	    byteBuffer = CharSequenceToBytes(word);
	    	    	   
	    
		// Try to find a partial match in the dictionary.
		final MatchResult match = matcher.match(matchResult,
				byteBuffer.array(), 0, byteBuffer.remaining(), rootNode);

		if (match.kind == EXACT_MATCH) {
		    containsSeparators = false;
		    return true;
		}
		
		final byte separator = dictionaryMetadata.separator;
		
		return (containsSeparators && match.kind == SEQUENCE_IS_A_PREFIX
		        && fsa.getArc(match.node, separator) != 0);
	}

	/**
	 * Propose suggestions for misspelled runon words. This algorithm is inspired by
	 * spell.cc in s_fsa package by Jan Daciuk.
	 * 
	 * @param original
	 *            The original misspelled word.
	 * @return The list of suggested pairs, as space-concatenated strings.
	 */
	public List<String> replaceRunOnWords(final String original) {
		final List<String> candidates = new ArrayList<String>();
		if (!isInDictionary(original) && dictionaryMetadata.runOnWords) {
			final CharSequence ch = original;
			for (int i = 2; i < ch.length(); i++) {
				// chop from left to right
				final CharSequence firstCh = ch.subSequence(0, i);
				if (isInDictionary(firstCh)
						&& isInDictionary(ch.subSequence(i, ch.length()))) {
					candidates.add(firstCh + " "
							+ ch.subSequence(i, ch.length()));
				}
			}
		}
		return candidates;
	}

	/**
	 * Find suggestions by using K. Oflazer's algorithm. See Jan Daciuk's s_fsa
	 * package, spell.cc for further explanation.
	 * 
	 * @param word
	 *            The original misspelled word.
	 * @return A list of suggested replacements.
	 * @throws CharacterCodingException
	 */
	public List<String> findReplacements(final String word)
			throws CharacterCodingException {
		candidates.clear();
		if (!isInDictionary(word) && word.length() < MAX_WORD_LENGTH) {
		    List<String> wordsToCheck = new ArrayList<String>();
		    wordsToCheck.add(word);
		    if (dictionaryMetadata.replacementPairs != null) {
	            for (final String key : dictionaryMetadata.replacementPairs.keySet()) {
	                if (word.contains(key)) {
	                    for (final String rep : dictionaryMetadata.replacementPairs.get(key)) {
	                        wordsToCheck.addAll(getAllReplacements(word, key, rep));
	                    }
	                }
	            }
		    }		    
		    for (final String wordChecked : wordsToCheck) {
			word_ff = wordChecked.toCharArray();
			wordLen = word_ff.length;
			candidate = new char[MAX_WORD_LENGTH];
			candLen = candidate.length;
			e_d = (wordLen <= editDistance ? (wordLen - 1) : editDistance);			
			charBuffer = BufferUtils.ensureCapacity(charBuffer, MAX_WORD_LENGTH);
			byteBuffer = BufferUtils.ensureCapacity(byteBuffer, MAX_WORD_LENGTH);
			charBuffer.clear();
			byteBuffer.clear();
			final byte[] prevBytes = new byte[0];
			findRepl(0, fsa.getRootNode(), prevBytes);
		    }
		}
		
		Collections.sort(candidates);
		final List<String> candStringList = new ArrayList<String>(candidates.size());
		for (final CandidateData cd : candidates) {
		    candStringList.add(cd.getWord());
		}
		return candStringList;
	}

	private void findRepl(final int depth, final int node, final byte[] prevBytes)
	        throws CharacterCodingException {
	    int dist = 0; 	    		
	    for (int arc = fsa.getFirstArc(node); arc != 0; arc = fsa.getNextArc(arc)) {	        
	        byteBuffer = BufferUtils.ensureCapacity(byteBuffer, prevBytes.length + 1);
	        byteBuffer.clear();
	        byteBuffer.put(prevBytes);	        
	        byteBuffer.put(fsa.getArcLabel(arc));
	        final int bufPos = byteBuffer.position();
	        byteBuffer.flip();	        
	        decoder.reset();
	        final CoderResult c = decoder.decode(byteBuffer, charBuffer, true);
	        if (c.isMalformed()) { // assume that only valid 
	                               // encodings are there	            
	            final byte[] prev = new byte[bufPos];
	            byteBuffer.position(0);	            
	            byteBuffer.get(prev);	            	               
	            if (!fsa.isArcTerminal(arc)) {	                    
	                findRepl(depth, fsa.getEndNode(arc), prev); // note: depth is not incremented    
	            }
	            byteBuffer.clear();
	        } else if (!c.isError()) { //unmappable characters are silently discarded
	            charBuffer.flip();
	            candidate[depth] = charBuffer.get();
	            charBuffer.clear();                                 
	            byteBuffer.clear();
	                	             
	        if (cuted(depth) <= e_d) {
	            if (Math.abs(wordLen - 1 - depth) <= e_d  
	                    && (dist = ed(wordLen - 1, depth)) <= e_d
	                    && (fsa.isArcFinal(arc) || isBeforeSeparator(arc))) {
	                addCandidate(depth, dist);	                    
	            }
	            if (!fsa.isArcTerminal(arc) && !(containsSeparators && 
	                    candidate[depth] == (char) dictionaryMetadata.separator)) {
	                findRepl(depth + 1, fsa.getEndNode(arc), new byte[0]);
	            }
	        }
	        }
	    }
	    return;
	}

    private boolean isBeforeSeparator(final int arc) {
        if (containsSeparators) {
            final int arc1 = fsa.getArc(fsa.getEndNode(arc),
                    dictionaryMetadata.separator);
            return (arc1 != 0 && !fsa.isArcTerminal(arc1));
        }
        return false;
    }
    
    private void addCandidate(final int depth, final int dist) 
            throws CharacterCodingException {
        final StringBuilder sb = new StringBuilder(depth);
        sb.append(candidate, 0, depth + 1);                    
        candidates.add(new CandidateData(sb.toString(), dist));
    }

	/**
	 * Calculates edit distance.
	 * 
	 * @param i
	 *            -(i) length of first word (here: misspelled)-1;
	 * @param j
	 *            -(i) length of second word (here: candidate)-1.
	 * @return Edit distance between the two words. Remarks: See Oflazer.
	 */
	public int ed(final int i, final int j) {
		int result;
		int a, b, c; // not really necessary

		if (areEqual(word_ff[i], candidate[j])) {
			// last characters are the same
			result = H.get(i, j);
		} else if (i > 0 && j > 0 && word_ff[i] == candidate[j - 1]
				&& word_ff[i - 1] == candidate[j]) {
			// last two characters are transposed
			a = H.get(i - 1, j - 1); // transposition, e.g. ababab, ababba
			b = H.get(i + 1, j); // deletion, e.g. abab, aba
			c = H.get(i, j + 1); // insertion e.g. aba, abab
			result = 1 + min(a, b, c);
		} else {
			// otherwise
			a = H.get(i, j); // replacement, e.g. ababa, ababb
			b = H.get(i + 1, j); // deletion, e.g. ab, a
			c = H.get(i, j + 1); // insertion e.g. a, ab
			result = 1 + min(a, b, c);
		}

		H.set(i + 1, j + 1, result);
		return result;
	}
	   
	 // by Jaume Ortola
	  private boolean areEqual(char x, char y) {
	    if (x == y) {
	      return true;
	    }
	    if (dictionaryMetadata.equivalentChars != null) {
	        if (dictionaryMetadata.equivalentChars.containsKey(x) &&
	            dictionaryMetadata.equivalentChars.get(x).contains(y))
	            return true;
	    }
	    if (dictionaryMetadata.ignoreDiacritics) {
	        String xn = Normalizer.normalize(Character.toString(x), Form.NFD);
	        String yn = Normalizer.normalize(Character.toString(y), Form.NFD);
	        if (dictionaryMetadata.convertCase) {
	            xn = xn.toLowerCase(dictionaryMetadata.dictionaryLocale);
	            yn = yn.toLowerCase(dictionaryMetadata.dictionaryLocale);	        
	        } 
	        return xn.charAt(0) == yn.charAt(0); 
	    }
	    return false;
	  }
	
	
	/**
	 * Calculates cut-off edit distance.
	 * 
	 * @param depth
	 *            - (int) current length of candidates.
	 * @return Cut-off edit distance. Remarks: See Oflazer.
	 */
	public int cuted(final int depth) {
		final int l = Math.max(0, depth - e_d); // min chars from word to consider - 1
		final int u = Math.min(wordLen - 1, depth + e_d); // max chars from word to
		                                            // consider - 1
		int min_ed = e_d + 1; // what is to be computed
		int d;

		for (int i = l; i <= u; i++) {
			if ((d = ed(i, depth)) < min_ed) {
                min_ed = d;
            }
		}
		return min_ed;
	}

	private int min(final int a, final int b, final int c) {
		return Math.min(a, Math.min(b, c));
	}

	/**
	   * Mimicks Java 1.7 Character.isAlphabetic() (needed as we require only 1.6)
	   *  
	   * @param codePoint The input character.
	   * @return True if the character is a Unicode alphabetic character.
	   */
	static boolean isAlphabetic(int codePoint) {
	      return (((((1 << Character.UPPERCASE_LETTER) |
	          (1 << Character.LOWERCASE_LETTER) |
	          (1 << Character.TITLECASE_LETTER) |
	          (1 << Character.MODIFIER_LETTER) |
	          (1 << Character.OTHER_LETTER) |
	          (1 << Character.LETTER_NUMBER)) >> Character.getType(codePoint)) & 1) != 0);
	 }
	
	/**
	 * Checks whether a string contains a digit. Used for ignoring words with
	 * numbers
	 * @param s Word to be checked.
	 * @return True if there is a digit inside the word.
	 */
	
	static boolean containsDigit(final String s) {
	    for (int k = 0; k < s.length(); k++) {
	      if (Character.isDigit(s.charAt(k))) {
	        return true;
	      }
	    }
	    return false;
	  }
	
	  /**
	   * Returns true if <code>str</code> is made up of all-uppercase characters
	   * (ignoring characters for which no upper-/lowercase distinction exists).
	   */
	  boolean isAllUppercase(final String str) {
	    return str.equals(str.toUpperCase(dictionaryMetadata.dictionaryLocale));
	  }

	  /**
	   * @param str - input string
	   */
	  boolean isCapitalizedWord(final String str) {
	    if (isEmpty(str)) {
	      return false;
	    }
	    final char firstChar = str.charAt(0);
	    if (Character.isUpperCase(firstChar)) {
	      return str.substring(1).equals(str.substring(1).toLowerCase(dictionaryMetadata.dictionaryLocale));
	    }
	    return false;
	  }
	  
	  /**
	   * Helper method to replace calls to "".equals().
	   * 
	   * @param str
	   *          String to check
	   * @return true if string is empty OR null
	   */
	  static boolean isEmpty(final String str) {
	    return str == null || str.length() == 0;
	  }

	  
	/**
	   * @param str - input str
	   * Returns true if str is MixedCase.
	   */
	  boolean isMixedCase(final String str) {
	    return !isAllUppercase(str)
	    && !isCapitalizedWord(str)
	    && !str.equals(str.toLowerCase(dictionaryMetadata.dictionaryLocale));
	  }
	
	/**
	 * Returns a list of all possible replacements of a given string
	 */
	List<String> getAllReplacements(final String str, final String src, final String rep) {
	    List<String> replaced = new ArrayList<String>();
	    StringBuilder sb = new StringBuilder();
	    sb.append(str);
	    int index = 0;
	    while (index != -1) {
	        index = sb.indexOf(src, index);
	        if (index != -1) {
	            //TODO: we replace the strings one by one
	            // e.g., "abcdabxyzab", key = ab, rep = eg => "egcdabxyzab", "egcdegxyzeg"
	            // but we also need to have "abcdegxyzeg", "abcdegxyzab"...
	            sb.replace(index, index + src.length(), rep);
	            replaced.add(sb.toString());
	        }	        
	    }
	    return replaced;
	}
	  
	/**
	 * Sets up the word and candidate.
	 * Used only to test the edit distance in JUnit tests.
	 * @param word - the first word
	 * @param candidate - the second word used for edit distance calculation 
	 */
	public void setWordAndCandidate(final String word, final String candidate) {
        word_ff = word.toCharArray();        
        wordLen = word_ff.length;        
        this.candidate = candidate.toCharArray();        
        candLen = this.candidate.length;
        e_d = (wordLen <= editDistance ? (wordLen - 1) : editDistance);	    
	}
	
	public final int getWordLen() {
	    return wordLen;
	}
	
	public final int getCandLen() {
	    return candLen;
	}
	
	public final int getEffectiveED(){
	    return e_d;
	}

	/**
	 * Used to sort candidates according to edit distance,
	 * and possibly according to their frequency in the future.
	 *
	 */
	private class CandidateData implements Comparable<CandidateData> {
	    private final String word;
	    private final int distance;
	    
	    CandidateData(final String word, final int distance) {
	        this.word = word;
	        this.distance = distance;
	    }
	    
	    final String getWord() {
	        return word;
	    }
	    
	    final int getDistance() {
	        return distance;
	    }

        @Override
        public int compareTo(final CandidateData cd) {            
            return ((cd.getDistance() > this.distance ? -1 :
                (cd.getDistance() == this.distance ? 0 : 1)));
        }
	}
}
