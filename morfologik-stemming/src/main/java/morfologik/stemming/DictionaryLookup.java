package morfologik.stemming;

import static morfologik.fsa.MatchResult.SEQUENCE_IS_A_PREFIX;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import morfologik.fsa.ByteSequenceIterator;
import morfologik.fsa.FSA;
import morfologik.fsa.FSATraversal;
import morfologik.fsa.MatchResult;

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
  private final ByteSequenceIterator finalStatesIterator;

  /** FSA's root node. */
  private final int rootNode;

  /** Expand buffers and arrays by this constant. */
  private final static int EXPAND_SIZE = 10;

  /** Private internal array of reusable word data objects. */
  private WordData[] forms = new WordData[0];

  /** A "view" over an array implementing */
  private final ArrayViewList<WordData> formsList = new ArrayViewList<WordData>(
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
   * @see #getSeparatorChar()
   */
  private final char separatorChar;

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

  private final ISequenceEncoder formEncoder;

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
    this.formEncoder = Encoders.forType(dictionary.metadata.getEncoderType());
    this.rootNode = dictionary.fsa.getRootNode();
    this.fsa = dictionary.fsa;
    this.matcher = new FSATraversal(fsa);
    this.finalStatesIterator = new ByteSequenceIterator(fsa, fsa.getRootNode());

    if (rootNode == 0) {
      throw new IllegalArgumentException(
          "Dictionary must have at least the root node.");
    }

    if (dictionaryMetadata == null) {
      throw new IllegalArgumentException(
          "Dictionary metadata must not be null.");
    }

    decoder = dictionary.metadata.getDecoder();
    encoder = dictionary.metadata.getEncoder();
    separatorChar = dictionary.metadata.getSeparatorAsChar();
  }

  /**
   * Searches the automaton for a symbol sequence equal to <code>word</code>,
   * followed by a separator. The result is a stem (decompressed accordingly
   * to the dictionary's specification) and an optional tag data.
   */
  @Override
  public List<WordData> lookup(CharSequence word) {
    final byte separator = dictionaryMetadata.getSeparator();

    if (!dictionaryMetadata.getInputConversionPairs().isEmpty()) {
      word = applyReplacements(word, dictionaryMetadata.getInputConversionPairs());
    }

    // Reset the output list to zero length.
    formsList.wrap(forms, 0, 0);

    // Encode word characters into bytes in the same encoding as the FSA's.
    charBuffer.clear();
    charBuffer = BufferUtils.ensureCapacity(charBuffer, word.length());
    for (int i = 0; i < word.length(); i++) {
      char chr = word.charAt(i);
      if (chr == separatorChar)
        return formsList;
      charBuffer.put(chr);
    }
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
          if (dictionaryMetadata.getOutputConversionPairs().isEmpty()) {
            wordData.update(byteBuffer, word);
          } else {
            wordData.update(byteBuffer, applyReplacements(word, dictionaryMetadata.getOutputConversionPairs()));
          }

          /*
           * Find the separator byte's position splitting the inflection instructions
           * from the tag.
           */
          int sepPos;
          for (sepPos = 0; sepPos < bbSize; sepPos++) {
            if (ba[sepPos] == separator) {
              break;
            }
          }

          /*
           * Decode the stem into stem buffer.
           */
          wordData.stemBuffer = formEncoder.decode(wordData.stemBuffer,
                                                   byteBuffer,
                                                   ByteBuffer.wrap(ba, 0, sepPos));

          // Skip separator character.
          sepPos++;

          /*
           * Decode the tag data.
           */
          final int tagSize = bbSize - sepPos;
          if (tagSize > 0) {
            wordData.tagBuffer = BufferUtils.ensureCapacity(wordData.tagBuffer, tagSize);
            wordData.tagBuffer.clear();
            wordData.tagBuffer.put(ba, sepPos, tagSize);
            wordData.tagBuffer.flip();
          }
        }

        formsList.wrap(forms, 0, formsCount);
      }
    } else {
      /*
       * this case is somewhat confusing: we should have hit the separator
       * first... I don't really know how to deal with it at the time
       * being.
       */
    }
    return formsList;
  }

  /**
   * Apply replacements partial string replacements from
   * a given map.
   * 
   * Useful if the word needs to be normalized somehow (i.e., ligatures,
   * apostrophes and such).
   */
  public static String applyReplacements(CharSequence word, LinkedHashMap<String, String> replacements) {
    // quite horrible from performance point of view; this should really be a transducer.
    StringBuilder sb = new StringBuilder(word);
    for (final Map.Entry<String, String> e : replacements.entrySet()) {
      String key = e.getKey();
      int index = sb.indexOf(e.getKey());
      while (index != -1) {
        sb.replace(index, index + key.length(), e.getValue());
        index = sb.indexOf(key, index + key.length());
      }
    }
    return sb.toString();
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
    if (encoder.encode(chars, bytes, true).isError()) {
      // remove everything, we don't want to accept malformed input
      bytes.clear();
    }
    bytes.flip();
    chars.reset();

    return bytes;
  }

  /**
   * Return an iterator over all {@link WordData} entries available in the
   * embedded {@link Dictionary}.
   */
  @Override
  public Iterator<WordData> iterator() {
    return new DictionaryIterator(dictionary, decoder, true);
  }

  /**
   * @return Return the {@link Dictionary} used by this object.
   */
  public Dictionary getDictionary() {
    return dictionary;
  }

  /**
   * @return Returns the logical separator character splitting inflected form,
   *         lemma correction token and a tag. Note that this character is a best-effort
   *         conversion from a byte in {@link DictionaryMetadata#separator} and
   *         may not be valid in the target encoding (although this is highly unlikely).
   */
  public char getSeparatorChar() {
    return separatorChar;
  }
}
