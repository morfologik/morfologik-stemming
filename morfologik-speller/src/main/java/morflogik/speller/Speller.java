// this will go to another package but 
// creating a new pom.xml for maven is beyond me...
package morflogik.speller;

import static morfologik.fsa.MatchResult.EXACT_MATCH;
import static morfologik.fsa.MatchResult.SEQUENCE_IS_A_PREFIX;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
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

	private final hMatrix H;

	public static int MAX_WORD_LENGTH = 120;

	private byte[] candidate; /* current replacement */
	private int candLen;
	private int wordLen; /* length of word being processed */
	private byte[] word_ff; /* word being processed */
	private final List<String> candidates = new ArrayList<String>();

	protected final char separatorChar;

	/**
	 * Internal reusable buffer for encoding words into byte arrays using
	 * {@link #encoder}.
	 */
	private ByteBuffer byteBuffer = ByteBuffer.allocate(0);

	/**
	 * Internal reusable buffer for encoding words into byte arrays using
	 * {@link #encoder}.
	 */
	private CharBuffer charBuffer = CharBuffer.allocate(0);

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
		H = new hMatrix(editDistance, MAX_WORD_LENGTH);

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
			Charset charset = Charset.forName(dictionaryMetadata.encoding);
			encoder = charset.newEncoder();
			decoder = charset.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT);
		} catch (UnsupportedCharsetException e) {
			throw new RuntimeException(
					"FSA's encoding charset is not supported: "
							+ dictionaryMetadata.encoding);
		}

		try {
			CharBuffer decoded = decoder.decode(ByteBuffer
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
			this.separatorChar = decoded.get();
		} catch (CharacterCodingException e) {
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
	protected ByteBuffer charsToBytes(CharBuffer chars, ByteBuffer bytes) {
		bytes.clear();
		final int maxCapacity = (int) (chars.remaining() * encoder
				.maxBytesPerChar());
		if (bytes.capacity() <= maxCapacity) {
			bytes = ByteBuffer.allocate(maxCapacity);
		}

		chars.mark();
		encoder.reset();
		encoder.encode(chars, bytes, true);
		bytes.flip();
		chars.reset();

		return bytes;
	}

	public boolean isInDictionary(CharSequence word) {

		// Encode word characters into bytes in the same encoding as the FSA's.
		charBuffer.clear();
		charBuffer = BufferUtils.ensureCapacity(charBuffer, word.length());
		for (int i = 0; i < word.length(); i++) {
			char chr = word.charAt(i);
			charBuffer.put(chr);
		}
		charBuffer.flip();
		byteBuffer = charsToBytes(charBuffer, byteBuffer);

		// Try to find a partial match in the dictionary.
		final MatchResult match = matcher.match(matchResult,
				byteBuffer.array(), 0, byteBuffer.remaining(), rootNode);

		return (match.kind == SEQUENCE_IS_A_PREFIX || match.kind == EXACT_MATCH);
	}

	/**
	 * Propose suggestions for misspelled runon words. This algorithm comes from
	 * spell.cc in s_fsa package by Jan Daciuk.
	 * 
	 * @param original
	 *            The original misspelled word.
	 * @return The list of suggested pairs, as space-concatenated strings.
	 */
	public List<String> replaceRunOnWords(final String original) {
		final List<String> candidates = new ArrayList<String>();
		if (!isInDictionary(original)) {
			CharSequence ch = original;
			for (int i = 2; i < ch.length(); i++) {
				// chop from left to right
				CharSequence firstCh = ch.subSequence(0, i);
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
	public List<String> findReplacements(final CharSequence word)
			throws CharacterCodingException {
		candidates.clear();
		if (!isInDictionary(word)) {
			CharBuffer ch = CharBuffer.allocate(word.length());
			ch.put(CharBuffer.wrap(word));
			ch.flip();
			ByteBuffer bb1 = ByteBuffer.allocate(word.length());
			bb1 = charsToBytes(ch, bb1);
			word_ff = bb1.array();
			wordLen = word_ff.length;
			candidate = word_ff.clone();
			candLen = candidate.length;
			e_d = (wordLen <= editDistance ? (wordLen - 1) : editDistance);
			findRepl(0, fsa.getRootNode());
		}
		return candidates;
	}

	private void findRepl(final int depth, final int node)
			throws CharacterCodingException {
		int dist = 0; // not yet used, might be useful for sorting suggestions
		if (depth + 1 >= candLen) {
			candidate = Arrays.copyOf(candidate, MAX_WORD_LENGTH);
		}

		for (int arc = fsa.getFirstArc(node); arc != 0; arc = fsa.getNextArc(arc)) {
			if (!fsa.isArcTerminal(arc)) {
				candidate[depth] = fsa.getArcLabel(arc);
				if (cuted(depth) <= e_d) {
				    // TODO: shouldn't findRepl be invoked _after_ a check
				    // for a potential suggestion? After you return from recursion
				    // the candidate array may be corrupted/ filled with junk data?
					findRepl(depth + 1, fsa.getEndNode(arc));
					if (Math.abs(wordLen - 1 - depth) <= e_d
							&& (dist = ed(wordLen - 1, depth)) <= e_d
							&& (fsa.isArcFinal(arc) || isBeforeSeparator(arc))) {
						ByteBuffer bb1 = ByteBuffer.allocate(MAX_WORD_LENGTH);
						bb1.put(candidate);
						bb1.limit(depth + 1);
						bb1.flip();
						CharBuffer ch = decoder.decode(bb1);
						candidates.add(ch.toString());
					}
				}
			}
		}
		return;
	}

	private boolean isBeforeSeparator(int arc) {
		int arc1 = fsa.getEndNode(arc);
		if (arc1 != 0 && !fsa.isArcTerminal(arc1)) {
			return fsa.getArcLabel(arc1) == dictionaryMetadata.separator;
		}
		return false;
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

		if (word_ff[i] == candidate[j]) {
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

	/**
	 * Calculates cut-off edit distance.
	 * 
	 * @param depth
	 *            - (int) current length of candidates.
	 * @return Cut-off edit distance. Remarks: See Oflazer.
	 */
	public int cuted(final int depth) {
		int l = Math.max(0, depth - e_d); // min chars from word to consider - 1
		int u = Math.min(wordLen - 1, depth + e_d);// max chars from word to
		// consider - 1
		int min_ed = e_d + 1; // what is to be computed
		int d;

		for (int i = l; i <= u; i++) {
			if ((d = ed(i, depth)) < min_ed)
				min_ed = d;
		}
		return min_ed;
	}

	private int min(final int a, final int b, final int c) {
		return Math.min(a, Math.min(b, c));
	}

	/**
	 * Keeps track of already computed values of edit distance.<br/>
	 * Remarks: To save space, the matrix is kept in a vector.
	 */
	private class hMatrix {
		private int[] p; /* the vector */
		private int rowLength; /* row length of matrix */
		int columnHeight; /* column height of matrix */
		int editDistance; /* edit distance */

		/**
		 * Allocates memory and initializes matrix (constructor).
		 * 
		 * @param distance (int) max edit distance allowed for
		 *        candidates;
		 * @param maxLength (int) max length of words.
		 * @return: Nothing. Remarks: See Oflazer. To save space, the matrix is
		 *          stored as a vector. To save time, additional raws and
		 *          columns are added. They are initialized to their distance in
		 *          the matrix, so that no bound checking is necessary during
		 *          access.
		 */
		public hMatrix(final int distance, final int maxLength) {
			rowLength = maxLength + 2;
			columnHeight = 2 * distance + 3;
			editDistance = distance;
			int size = rowLength * columnHeight;
			p = new int[size];
			// Initialize edges of the diagonal band to distance + 1 (i.e.
			// distance too big)
			for (int i = 0; i < rowLength - distance - 1; i++) {
				p[i] = distance + 1; // H(distance + j, j) = distance + 1
				p[size - i - 1] = distance + 1; // H(i, distance + i) = distance
				// + 1
			}
			// Initialize items H(i,j) with at least one index equal to zero to
			// |i - j|
			for (int j = 0; j < 2 * distance + 1; j++) {
				p[j * rowLength] = distance + 1 - j; // H(i=0..distance+1,0)=i
				//FIXME: dla distance == 2 tu mamy wykroczenie poza rozmiar tablicy
				p[Math.min(p.length - 1, (j + distance + 1) * rowLength + j)] = j; // H(0,j=0..distance+1)=j
				//w spell.cc jest tutaj błąd, Jaś już wie...
			}
		}

		/**
		 * Provide an item of hMatrix indexed by indices.
		 * 
		 * @param i
		 *            - (int) row number;
		 * @param j
		 *            - (int) column number.
		 * @return Item H[i][j] <br/>
		 *         Remarks: H matrix is really simulated. What is needed is only
		 *         2 * edit_distance + 1 wide band around the diagonal. In fact
		 *         this diagonal has been pushed up to the upper border of the
		 *         matrix.
		 * 
		 *         The matrix in the vector looks likes this:
		 * 
		 *         <pre>
		 * 	    +---------------------+
		 * 	0   |#####################| j=i-e-1
		 * 	1   |                     | j=i-e
		 * 	    :                     :
		 * 	e+1 |                     | j=i-1
		 * 	    +---------------------+
		 * 	e+2 |                     | j=i
		 * 	    +---------------------+
		 * 	e+3 |                     | j=i+1
		 * 	    :                     :
		 * 	2e+2|                     | j=i+e
		 * 	2e+3|#####################| j=i+e+1
		 * 	    +---------------------+
		 * </pre>
		 */
		public int get(final int i, final int j) {
			return p[(j - i + editDistance + 1) * rowLength + j];
		}

		/**
		 * Set an item in hMatrix.
		 * 
		 * @param i
		 *            - (int) row number;
		 * @param j
		 *            - (int) column number;
		 * @param val
		 *            - (int) value to put there.
		 * @return: Nothing.
		 * 
		 *          No checking for i & j is done. They must be correct.
		 */
		public void set(final int i, final int j, final int val) {
			p[(j - i + editDistance + 1) * rowLength + j] = val;
		}

	}

}
