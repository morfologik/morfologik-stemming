package morfologik.stemming;

import static morfologik.fsa.MatchResult.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.*;

import morfologik.fsa.*;
import morfologik.util.BufferUtils;

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
 * @see <a href="http://www.eti.pg.gda.pl/~jandac/fsa.html">FSA package Web
 *      site</a>
 */
public final class DictionaryLookup implements IStemmer, Iterable<WordData> {
	/** An FSA used for lookups. */
	private final FSATraversal matcher;

	/** An iterator for walking along the final states of {@link #fsa}. */
	private final FSAFinalStatesIterator finalStatesIterator;

	/** FSA's root node. */
	private final int rootNode;

	/** Expand buffers and arrays by this constant. */
	private final static int EXPAND_SIZE = 10;

	/** Private internal array of reusable word data objects. */
	private WordData[] forms = new WordData[0];

	/** A "view" over an array implementing */
	private ArrayViewList<WordData> formsList = new ArrayViewList<WordData>(
	        forms, 0, forms.length);

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
	private final CharsetDecoder decoder;

	/**
	 * The FSA we are using.
	 */
	private final FSA fsa;

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
	 * The {@link Dictionary} this lookup is using.
	 */
	private final Dictionary dictionary;

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
		this.dictionary = dictionary;
		this.dictionaryMetadata = dictionary.metadata;
		this.rootNode = dictionary.fsa.getRootNode();
		this.fsa = dictionary.fsa;
		this.matcher = new FSATraversal(fsa);
		this.finalStatesIterator = new FSAFinalStatesIterator(fsa, fsa.getRootNode());

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
			decoder = charset.newDecoder().onMalformedInput(
			        CodingErrorAction.REPORT).onUnmappableCharacter(
			        CodingErrorAction.REPORT);
		} catch (UnsupportedCharsetException e) {
			throw new RuntimeException(
			        "FSA's encoding charset is not supported: "
			                + dictionaryMetadata.encoding);
		}
	}

	/**
	 * Searches the automaton for a symbol sequence equal to <code>word</code>,
	 * followed by a separator. The result is a stem (decompressed accordingly
	 * to the dictionary's specification) and an optional tag data.
	 */
	public List<WordData> lookup(CharSequence word) {
		final byte separator = dictionaryMetadata.separator;

		// Encode word characters into bytes in the same encoding as the FSA's.
		charBuffer.clear();
		charBuffer = BufferUtils.ensureCapacity(charBuffer, word.length());
		for (int i = 0; i < word.length(); i++)
			charBuffer.put(word.charAt(i));
		charBuffer.flip();
		byteBuffer = charsToBytes(charBuffer, byteBuffer);

		// Try to find a partial match in the dictionary.
		final MatchResult match = matcher.match(matchResult, byteBuffer
		        .array(), 0, byteBuffer.remaining(), rootNode);

		if (match.kind == SEQUENCE_IS_A_PREFIX) {
			/*
			 * The entire sequence exists in the dictionary. A separator should
			 * be the next symbol.
			 */
			final int arc = fsa.getArc(match.node, separator);

			/*
			 * The situation when the arc points to a final node should NEVER
			 * happen. After all, we want the word to have SOME base form.
			 */
			if (arc != 0 && !fsa.isArcFinal(arc)) {
				// There is such a word in the dictionary. Return its base forms.
				int formsCount = 0;

				finalStatesIterator.restartFrom(fsa.getEndNode(arc));
				while (finalStatesIterator.hasNext()) {
					final ByteBuffer bb = finalStatesIterator.next();
					final byte[] ba = bb.array();
					final int bbSize = bb.remaining();

					if (formsCount >= forms.length) {
						forms = Arrays.copyOf(forms, forms.length + EXPAND_SIZE);
						for (int k = 0; k < forms.length; k++) {
							if (forms[k] == null)
								forms[k] = new WordData(decoder);
						}
					}

					/*
					 * Now, expand the prefix/ suffix 'compression' and store
					 * the base form.
					 */
					final WordData wordData = forms[formsCount++];
					wordData.reset();

					wordData.wordBuffer = byteBuffer;
					wordData.wordCharSequence = word;

					/*
					 * Find the separator byte's position splitting word form
					 * and tag.
					 */
					int sepPos;
					for (sepPos = 0; sepPos < bbSize; sepPos++) {
						if (ba[sepPos] == separator)
							break;
					}

					/*
					 * Decode the stem into stem buffer.
					 */
					wordData.stemBuffer.clear();
					wordData.stemBuffer = decodeStem(wordData.stemBuffer, ba,
					        sepPos, byteBuffer, dictionaryMetadata);
					wordData.stemBuffer.flip();

					// Skip separator character.
					sepPos++;

					/*
					 * Decode the tag data.
					 */
					wordData.tagBuffer = BufferUtils.ensureCapacity(
					        wordData.tagBuffer, bbSize - sepPos);
					wordData.tagBuffer.clear();
					wordData.tagBuffer.put(ba, sepPos, bbSize - sepPos);
					wordData.tagBuffer.flip();
				}

				formsList.wrap(forms, 0, formsCount);
				return formsList;
			}
		} else {
			/*
			 * this case is somewhat confusing: we should have hit the separator
			 * first... I don't really know how to deal with it at the time
			 * being.
			 */
		}

		return Collections.emptyList();
	}

	/**
	 * Decode the base form of an inflected word and save its decoded form into
	 * a byte buffer.
	 * 
	 * @param bb
	 *            The byte buffer to save the result to. A new buffer may be
	 *            allocated if the capacity of <code>bb</code> is not large
	 *            enough to store the result. The buffer is not flipped upon
	 *            return.
	 * 
	 * @param inflectedBuffer
	 *            Inflected form's bytes (decoded properly).
	 * 
	 * @param bytes
	 *            Bytes of the encoded base form, starting at 0 index.
	 * 
	 * @param len
	 *            Length of the encode base form.
	 * 
	 * @return Returns either <code>bb</code> or a new buffer whose capacity is
	 *         large enough to store the output of the decoded data.
	 */
	public static ByteBuffer decodeStem(ByteBuffer bb, byte[] bytes, int len,
	        ByteBuffer inflectedBuffer, DictionaryMetadata metadata) {
		bb.clear();

		// Empty length? Weird, but return an empty buffer.
		if (len == 0) {
			return bb;
		}

		// Determine inflected string's length in bytes, in the same encoding.
		final byte[] infBytes = inflectedBuffer.array();
		final int infLen = inflectedBuffer.remaining();
		final int code0 = bytes[0] - 'A';

		final boolean fsaPrefixes = metadata.usesPrefixes;
		final boolean fsaInfixes = metadata.usesInfixes;

		// Increase buffer size, if needed.
		if (bb.capacity() < infLen + len) {
			bb = ByteBuffer.allocate(infLen + len);
		}

		if (code0 >= 0) {
			if (!fsaPrefixes && !fsaInfixes) {
				if (code0 <= infLen) {
					bb.put(infBytes, 0, infLen - code0);
					bb.put(bytes, 1, len - 1);
					return bb;
				}
			} else if (fsaPrefixes && !fsaInfixes) {
				if (len > 1) {
					final int stripAtEnd = bytes[1] - 'A' + code0;
					if (stripAtEnd <= infLen) {
						bb.put(infBytes, code0, infLen - stripAtEnd);
						bb.put(bytes, 2, len - 2);
						return bb;
					}
				}
			} else if (fsaInfixes) {
				// Note: Prefixes are silently assumed here.
				if (len > 2) {
					final int stripAtBeginning = bytes[1] - 'A' + code0;
					final int stripAtEnd = bytes[2] - 'A' + stripAtBeginning;
					if (stripAtEnd <= infLen) {
						bb.put(infBytes, 0, code0);
						bb.put(infBytes, stripAtBeginning, infLen - stripAtEnd);
						bb.put(bytes, 3, len - 3);
						return bb;
					}
				}
			}
		}

		/*
		 * This is a fallback in case some junk is detected above. Return the
		 * base form only if this is the case.
		 */
		bb.clear();
		bb.put(bytes, 0, len);
		return bb;
	}

	/**
	 * Encode a character sequence into a byte buffer, optionally expanding
	 * buffer.
	 */
	private ByteBuffer charsToBytes(CharBuffer chars, ByteBuffer bytes) {
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

	/**
	 * Return an iterator over all {@link WordData} entries available in the
	 * embedded {@link Dictionary}.
	 */
	public Iterator<WordData> iterator() {
		return new DictionaryIterator(dictionary, decoder, true);
	}

	/**
	 * @return Return the {@link Dictionary} used by this object.
	 */
	public Dictionary getDictionary() {
		return dictionary;
	}
}
