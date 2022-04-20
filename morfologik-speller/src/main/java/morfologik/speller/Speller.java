package morfologik.speller;

import static morfologik.fsa.MatchResult.EXACT_MATCH;
import static morfologik.fsa.MatchResult.SEQUENCE_IS_A_PREFIX;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.*;

import morfologik.fsa.FSA;
import morfologik.fsa.ByteSequenceIterator;
import morfologik.fsa.FSATraversal;
import morfologik.fsa.MatchResult;
import morfologik.stemming.BufferUtils;
import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.DictionaryMetadata;
import morfologik.stemming.UnmappableInputException;

/**
 * Finds spelling suggestions. Implements K. Oflazer's algorithm as described
 * in: Oflazer, Kemal. 1996.
 * "Error-Tolerant Finite-State Recognition with Applications to Morphological Analysis and Spelling Correction."
 * <i>Computational Linguistics</i> 22 (1): 73–89.
 * 
 * <p>
 * See Jan Daciuk's <code>s_fsa</code> package.
 */
public class Speller {
  /**
   * Maximum length of the word to be checked.
   */
  public static final int MAX_WORD_LENGTH = 120;
  static final int FREQ_RANGES = 'Z' - 'A' + 1;
  static final int FIRST_RANGE_CODE = 'A'; // less frequent words

  //FIXME: this is an upper limit for replacement searches, we need
  //proper tree traversal instead of generation of all possible candidates
  static final int UPPER_SEARCH_LIMIT = 15;
  private static final int MIN_WORD_LENGTH = 4;
  private static final int MAX_RECURSION_LEVEL = 6;

  private final int editDistance;
  private int effectEditDistance; // effective edit distance

  private final HMatrix hMatrix;

  private char[] candidate; /* current replacement */
  private int candLen;
  private int wordLen; /* length of word being processed */
  private char[] wordProcessed; /* word being processed */

  private Map<Character, List<char[]>> replacementsAnyToOne = new HashMap<>();
  private Map<String, List<char[]>> replacementsAnyToTwo = new HashMap<>();
  private Map<String, List<String>> replacementsTheRest = new HashMap<>();

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
  private final CharsetDecoder decoder;

  /** An FSA used for lookups. */
  private final FSATraversal matcher;

  /** FSA's root node. */
  private final int rootNode;

  /**
   * The FSA we are using.
   */
  private final FSA fsa;

  /** An iterator for walking along the final states of {@link #fsa}. */
  private final ByteSequenceIterator finalStatesIterator;

  public Speller(final Dictionary dictionary) {
    this(dictionary, 1);
  }

  public Speller(final Dictionary dictionary, final int editDistance) {
    this.editDistance = editDistance;
    this.hMatrix = new HMatrix(editDistance, MAX_WORD_LENGTH);

    this.dictionaryMetadata = dictionary.metadata;
    this.rootNode = dictionary.fsa.getRootNode();
    this.fsa = dictionary.fsa;
    this.matcher = new FSATraversal(fsa);
    this.finalStatesIterator = new ByteSequenceIterator(fsa, rootNode);

    if (rootNode == 0) {
      throw new IllegalArgumentException("Dictionary must have at least the root node.");
    }

    if (dictionaryMetadata == null) {
      throw new IllegalArgumentException("Dictionary metadata must not be null.");
    }

    encoder = dictionaryMetadata.getEncoder();
    decoder = dictionaryMetadata.getDecoder();

    // Multibyte separator will result in an exception here.
    dictionaryMetadata.getSeparatorAsChar();

    this.createReplacementsMaps();
  }

  private void createReplacementsMaps() {
    for (Map.Entry<String, List<String>> entry : dictionaryMetadata.getReplacementPairs().entrySet()) {
      for (String s : entry.getValue()) {
        // replacements any to one
        // the new key is the target of the replacement pair
        if (s.length() == 1) {
          if (!replacementsAnyToOne.containsKey(s.charAt(0))) {
            List<char[]> charList = new ArrayList<>();
            charList.add(entry.getKey().toCharArray());
            replacementsAnyToOne.put(s.charAt(0), charList);
          } else {
            replacementsAnyToOne.get(s.charAt(0)).add(entry.getKey().toCharArray());
          }
        }
        // replacements any to two
        // the new key is the target of the replacement pair
        else if (s.length() == 2) {
          if (!replacementsAnyToTwo.containsKey(s)) {
            List<char[]> charList = new ArrayList<>();
            charList.add(entry.getKey().toCharArray());
            replacementsAnyToTwo.put(s, charList);
          } else {
            replacementsAnyToTwo.get(s).add(entry.getKey().toCharArray());
          }
        } else {
          if (!replacementsTheRest.containsKey(entry.getKey())) {
            List<String> charList = new ArrayList<>();
            charList.add(s);
            replacementsTheRest.put(entry.getKey(), charList);
          } else {
            replacementsTheRest.get(entry.getKey()).add(s);
          }
        }
      }
    }
  }

  private ByteBuffer charSequenceToBytes(final CharSequence word) throws UnmappableInputException {
    // Encode word characters into bytes in the same encoding as the FSA's.
    charBuffer = BufferUtils.clearAndEnsureCapacity(charBuffer, word.length());
    for (int i = 0; i < word.length(); i++) {
      final char chr = word.charAt(i);
      charBuffer.put(chr);
    }
    charBuffer.flip();

    return BufferUtils.charsToBytes(encoder, charBuffer, byteBuffer);
  }

  /**
   * Checks whether the word is misspelled, by performing a series of checks
   * according to properties of the dictionary.
   *
   * If the flag <code>fsa.dict.speller.ignore-punctuation</code> is set, then
   * all non-alphabetic characters are considered to be correctly spelled.
   *
   * If the flag <code>fsa.dict.speller.ignore-numbers</code> is set, then all
   * words containing decimal digits are considered to be correctly spelled.
   *
   * If the flag <code>fsa.dict.speller.ignore-camel-case</code> is set, then
   * all CamelCase words are considered to be correctly spelled.
   *
   * If the flag <code>fsa.dict.speller.ignore-all-uppercase</code> is set, then
   * all alphabetic words composed of only uppercase characters are considered
   * to be correctly spelled.
   *
   * Otherwise, the word is checked in the dictionary. If the test fails, and
   * the dictionary does not perform any case conversions (as set by
   * <code>fsa.dict.speller.convert-case</code> flag), then the method returns
   * false. In case of case conversions, it is checked whether a non-mixed case
   * word is found in its lowercase version in the dictionary, and for
   * all-uppercase words, whether the word is found in the dictionary with the
   * initial uppercase letter.
   *
   * @param word
   *          - the word to be checked
   * @return true if the word is misspelled
   **/
  public boolean isMisspelled(final String word) {
    // dictionaries usually do not contain punctuation
    String wordToCheck = word;
    if (!dictionaryMetadata.getInputConversionPairs().isEmpty()) {
      wordToCheck = DictionaryLookup.applyReplacements(word, dictionaryMetadata.getInputConversionPairs());
    }
    boolean isAlphabetic = wordToCheck.length() != 1 || isAlphabetic(wordToCheck.charAt(0));
    return wordToCheck.length() > 0
        && (!dictionaryMetadata.isIgnoringPunctuation() || isAlphabetic)
        && (!dictionaryMetadata.isIgnoringNumbers() || containsNoDigit(wordToCheck))
        && !(dictionaryMetadata.isIgnoringCamelCase() && isCamelCase(wordToCheck))
        && !(dictionaryMetadata.isIgnoringAllUppercase() && isAlphabetic && isAllUppercase(wordToCheck))
        && !isInDictionary(wordToCheck)
        && (!dictionaryMetadata.isConvertingCase() || 
            !(!isMixedCase(wordToCheck) && 
                (isInDictionary(wordToCheck.toLowerCase(dictionaryMetadata.getLocale())) 
                    || isAllUppercase(wordToCheck) && isInDictionary(initialUppercase(wordToCheck)))));
  }

  private CharSequence initialUppercase(final String wordToCheck) {
    return wordToCheck.substring(0, 1) + wordToCheck.substring(1).toLowerCase(dictionaryMetadata.getLocale());
  }

  /**
   * Test whether the word is found in the dictionary.
   * 
   * @param word
   *          the word to be tested
   * @return True if it is found.
   */
  public boolean isInDictionary(final CharSequence word) {
    try {
      byteBuffer = charSequenceToBytes(word);
    } catch (UnmappableInputException e) {
      return false;
    }

    // Try to find a partial match in the dictionary.
    final MatchResult match = matcher.match(matchResult, byteBuffer.array(), 0, byteBuffer.remaining(), rootNode);

    // Make sure the word doesn't contain a separator if there is an exact match
    if (containsSeparators && match.kind == EXACT_MATCH) {
      containsSeparators = false;
      for (int i=0; i<word.length(); i++) {
        if (word.charAt(i) == dictionaryMetadata.getSeparator()) {
          containsSeparators = true;
          break;
        }
      }
    }
    
    if (match.kind == EXACT_MATCH && !containsSeparators) {
      return true;
    }

    return containsSeparators && match.kind == SEQUENCE_IS_A_PREFIX && byteBuffer.remaining() > 0
        && fsa.getArc(match.node, dictionaryMetadata.getSeparator()) != 0;
  }

  /**
   * Get the frequency value for a word form. It is taken from the first entry
   * with this word form.
   * 
   * @param word
   *          the word to be tested
   * @return frequency value in range: 0..FREQ_RANGE-1 (0: less frequent).
   */

  public int getFrequency(final CharSequence word) {
    if (!dictionaryMetadata.isFrequencyIncluded()) {
      return 0;
    }

    final byte separator = dictionaryMetadata.getSeparator();
    try {
      byteBuffer = charSequenceToBytes(word);
    } catch (UnmappableInputException e) {
      return 0;
    }

    final MatchResult match = matcher.match(matchResult, byteBuffer.array(), 0, byteBuffer.remaining(), rootNode);
    if (match.kind == SEQUENCE_IS_A_PREFIX) {
      final int arc = fsa.getArc(match.node, separator);
      if (arc != 0 && !fsa.isArcFinal(arc)) {
        finalStatesIterator.restartFrom(fsa.getEndNode(arc));
        if (finalStatesIterator.hasNext()) {
          final ByteBuffer bb = finalStatesIterator.next();
          final byte[] ba = bb.array();
          final int bbSize = bb.remaining();
          //the last byte contains the frequency after a separator
          return ba[bbSize - 1] - FIRST_RANGE_CODE;
        }
      }
    }
    return 0;
  }

  /**
   * Propose suggestions for misspelled run-on words. This algorithm is inspired
   * by spell.cc in s_fsa package by Jan Daciuk.
   * 
   * @param original
   *          The original misspelled word.
   * @return The list of suggested pairs, as CandidateData with space-concatenated strings.
   */
  public List<CandidateData> replaceRunOnWordCandidates(final String original) {
    final List<CandidateData> candidates = new ArrayList<>();
    String wordToCheck = original;
    if (!dictionaryMetadata.getInputConversionPairs().isEmpty()) {
      wordToCheck = DictionaryLookup.applyReplacements(original, dictionaryMetadata.getInputConversionPairs());
    }
    if (!isInDictionary(wordToCheck) && dictionaryMetadata.isSupportingRunOnWords()) {
      Locale locale = dictionaryMetadata.getLocale();
      for (int i = 1; i < wordToCheck.length(); i++) {
        // chop from left to right
        final String prefix = wordToCheck.substring(0, i);
        final String suffix = wordToCheck.substring(i);
        if (isInDictionary(suffix)) {
          if (isInDictionary(prefix)) {
            addReplacement(candidates, prefix + " " + suffix);
          } else if (Character.isUpperCase(prefix.charAt(0)) && isInDictionary(prefix.toLowerCase(locale))) {
            // a word that's uppercase just because used at sentence start
            addReplacement(candidates, prefix + " " + suffix);
          }
        }
      }
    }
    return candidates;
  }

  /**
   * Propose suggestions for misspelled run-on words. This algorithm is inspired
   * by spell.cc in s_fsa package by Jan Daciuk.
   *
   * @param original
   *          The original misspelled word.
   * @return The list of suggested pairs, as space-concatenated strings.
   */
  public List<String> replaceRunOnWords(final String original) {
    final List<CandidateData> candidateData = replaceRunOnWordCandidates(original);
    final List<String> candidates = new ArrayList<>();
    for (CandidateData candidate : candidateData) {
      candidates.add(candidate.word);
    }
    return candidates;
  }

  private void addReplacement(List<CandidateData> candidates, String replacement) {
    if (dictionaryMetadata.getOutputConversionPairs().isEmpty()) {
      candidates.add(new CandidateData(replacement, 1));
    } else {
      candidates.add(new CandidateData(DictionaryLookup.applyReplacements(replacement,
          dictionaryMetadata.getOutputConversionPairs()), 1));
    }
  }

  /**
   * Find similar words even if the original word is a correct word that exists in the dictionary
   * 
   * @param word The original word.
   * @return A list of suggested candidate replacements.
   */
  public ArrayList<CandidateData> findSimilarWordCandidates(String word) {
    return findReplacementCandidates(word, true);
  }
  
  public ArrayList<String> findSimilarWords(String word) {
    final List<CandidateData> result = findSimilarWordCandidates(word);
    final ArrayList<String> resultSuggestions = new ArrayList<>(result.size());
    for (CandidateData cd : result) {
      resultSuggestions.add(cd.getWord());
    }
    return resultSuggestions;
  }
  
  
  /**
   * Find suggestions by using K. Oflazer's algorithm. See Jan Daciuk's s_fsa
   * package, spell.cc for further explanation.
   * 
   * @param word The original misspelled word.
   * @return A list of suggested replacements.
   */
  public ArrayList<String> findReplacements(String word) {
    final List<CandidateData> result = findReplacementCandidates(word);

    final ArrayList<String> resultSuggestions = new ArrayList<>(result.size());
    for (CandidateData cd : result) {
      resultSuggestions.add(cd.getWord());
    }
    return resultSuggestions;
  }

  
  /**
   * Find and return suggestions by using K. Oflazer's algorithm. See Jan Daciuk's s_fsa
   * package, spell.cc for further explanation. This method is identical to
   * {@link #findReplacements}, but returns candidate terms with their edit distance scores.
   *
   * @param word The original misspelled word.
   * @return A list of suggested candidate replacements.
   */
  public ArrayList<CandidateData> findReplacementCandidates(String word) {
    return findReplacementCandidates(word, false);
  }
  
  private ArrayList<CandidateData> findReplacementCandidates(String word, boolean evenIfWordInDictionary) {
    if (!dictionaryMetadata.getInputConversionPairs().isEmpty()) {
      word = DictionaryLookup.applyReplacements(word, dictionaryMetadata.getInputConversionPairs());
    }

    // candidate strings, including same additional data such as edit distance from the original word.
    List<CandidateData> candidates = new ArrayList<>();

    if (word.length() > 0 && word.length() < MAX_WORD_LENGTH && (!isInDictionary(word) || evenIfWordInDictionary)) {
      List<String> wordsToCheck = new ArrayList<>();
      if (replacementsTheRest != null && word.length() > 1) {
        for (final String wordChecked : getAllReplacements(word, 0, 0)) {
          if (isInDictionary(wordChecked)) {
            candidates.add(new CandidateData(wordChecked, 0));
          } else {
            String lowerWord = wordChecked.toLowerCase(dictionaryMetadata.getLocale());
            String upperWord = wordChecked.toUpperCase(dictionaryMetadata.getLocale());
            if (isInDictionary(lowerWord)) {
              //add the word as it is in the dictionary, not mixed-case versions of it
              candidates.add(new CandidateData(lowerWord, 0));
            }
            if (isInDictionary(upperWord)) {
              candidates.add(new CandidateData(upperWord, 0));
            }
            if (lowerWord.length() > 1) {
              String firstUpperWord = Character.toUpperCase(lowerWord.charAt(0)) + lowerWord.substring(1);
              if (isInDictionary(firstUpperWord)) {
                candidates.add(new CandidateData(firstUpperWord, 0));
              }
            }
          }
          wordsToCheck.add(wordChecked);
        }
      } else {
        wordsToCheck.add(word);
      }

      // Even if a candidate was found with the replacement pairs (which are usual errors),
      // there might be more good candidates (see issue #94):
      int i = 1;
      for (final String wordChecked : wordsToCheck) {
        i++;
        if (i > UPPER_SEARCH_LIMIT) { // for performance reasons, do not search too deeply
          break;
        }
        wordProcessed = wordChecked.toCharArray();
        wordLen = wordProcessed.length;
        if (wordLen < MIN_WORD_LENGTH && i > 2) { // three-letter replacements make little sense anyway
          break;
        }
        candidate = new char[MAX_WORD_LENGTH];
        candLen = candidate.length;
        effectEditDistance = wordLen <= editDistance ? wordLen - 1 : editDistance;
        charBuffer = BufferUtils.clearAndEnsureCapacity(charBuffer, MAX_WORD_LENGTH);
        byteBuffer = BufferUtils.clearAndEnsureCapacity(byteBuffer, MAX_WORD_LENGTH);
        final byte[] prevBytes = new byte[0];
        findRepl(candidates, 0, fsa.getRootNode(), prevBytes, 0, 0);
      }
    }

    Collections.sort(candidates);

    // Apply replacements, prune duplicates while preserving the candidate order.
    final Set<String> words = new HashSet<>();
    final ArrayList<CandidateData> result = new ArrayList<>(candidates.size());
    for (final CandidateData cd : candidates) {
      String replaced = DictionaryLookup.applyReplacements(cd.getWord(), dictionaryMetadata.getOutputConversionPairs());
      // Add only the first occurrence of a given word.
      if (words.add(replaced) && !replaced.equals(word)) {
        result.add(new CandidateData(replaced, cd.origDistance));
      }
    }

    return result;
  }

  private void findRepl(List<CandidateData> candidates, final int depth, final int node, final byte[] prevBytes, final int wordIndex, final int candIndex) {
    int dist = 0;
    for (int arc = fsa.getFirstArc(node); arc != 0; arc = fsa.getNextArc(arc)) {
      byteBuffer = BufferUtils.clearAndEnsureCapacity(byteBuffer, prevBytes.length + 1);
      byteBuffer.put(prevBytes);
      byteBuffer.put(fsa.getArcLabel(arc));
      final int bufPos = byteBuffer.position();
      byteBuffer.flip();
      decoder.reset();
      // FIXME: this isn't correct -- no checks for overflows, no decoder flush. I don't think this should be in here
      // too, the decoder should run once on accumulated temporary byte buffer (current path) only when there's
      // a potential that this buffer can become a replacement candidate (isEndOfCandidate). Because we assume candidates
      // are valid input strings (this is verified when building the dictionary), it's save a lot of conversions.
      final CoderResult c = decoder.decode(byteBuffer, charBuffer, true);
      if (c.isMalformed()) { // assume that only valid
        // encodings are there
        final byte[] prev = new byte[bufPos];
        byteBuffer.position(0);
        byteBuffer.get(prev);
        if (!fsa.isArcTerminal(arc)) {
          findRepl(candidates, depth, fsa.getEndNode(arc), prev, wordIndex, candIndex); // note: depth is not incremented
        }
        byteBuffer.clear();
      } else if (!c.isError()) { // unmappable characters are silently discarded
        charBuffer.flip();
        candidate[candIndex] = charBuffer.get();
        charBuffer.clear();
        byteBuffer.clear();

        int lengthReplacement;
        // replacement "any to two"
        if ((lengthReplacement = matchAnyToTwo(wordIndex, candIndex)) > 0) {
          // the replacement takes place at the end of the candidate
          if (isEndOfCandidate(arc, wordIndex) && (dist = hMatrix.get(depth - 1, depth - 1)) <= effectEditDistance) {
            if (Math.abs(wordLen - 1 - (wordIndex + lengthReplacement - 2)) > 0) {
              // there are extra letters in the word after the replacement
              dist = dist + Math.abs(wordLen - 1 - (wordIndex + lengthReplacement - 2));
            }
            if (dist <= effectEditDistance) {
              candidates.add(new CandidateData(String.valueOf(candidate, 0, candIndex + 1), dist));
            }
          }
          if (isArcNotTerminal(arc, candIndex)) {
            int x = hMatrix.get(depth, depth);
            hMatrix.set(depth, depth, hMatrix.get(depth - 1, depth - 1));
            findRepl(candidates, Math.max(0, depth), fsa.getEndNode(arc), new byte[0], wordIndex + lengthReplacement - 1,
                candIndex + 1);
            hMatrix.set(depth, depth, x);
          }
        }
        //replacement "any to one"
        if ((lengthReplacement = matchAnyToOne(wordIndex, candIndex)) > 0) {
          // the replacement takes place at the end of the candidate
          if (isEndOfCandidate(arc, wordIndex) && (dist = hMatrix.get(depth, depth)) <= effectEditDistance) {
            if (Math.abs(wordLen - 1 - (wordIndex + lengthReplacement - 1)) > 0) {
              // there are extra letters in the word after the replacement
              dist = dist + Math.abs(wordLen - 1 - (wordIndex + lengthReplacement - 1));
            }
            if (dist <= effectEditDistance) {
              candidates.add(new CandidateData(String.valueOf(candidate, 0, candIndex + 1), dist));
            }
          }
          if (isArcNotTerminal(arc, candIndex)) {
            findRepl(candidates, depth, fsa.getEndNode(arc), new byte[0], wordIndex + lengthReplacement, candIndex + 1);
          }
        }
        //general
        if (cuted(depth, wordIndex, candIndex) <= effectEditDistance) {
          if ((isEndOfCandidate(arc, wordIndex))
              && (dist = ed(wordLen - 1 - (wordIndex - depth), depth, wordLen - 1, candIndex)) <= effectEditDistance) {
            candidates.add(new CandidateData(String.valueOf(candidate, 0, candIndex + 1), dist));
          }
          if (isArcNotTerminal(arc, candIndex)) {
            findRepl(candidates, depth + 1, fsa.getEndNode(arc), new byte[0], wordIndex + 1, candIndex + 1);
          }
        }
      }
    }
  }

  private boolean isArcNotTerminal(final int arc, final int candIndex) {
    return !fsa.isArcTerminal(arc)
        && !(containsSeparators && candidate[candIndex] == dictionaryMetadata.getSeparatorAsChar());
  }

  private boolean isEndOfCandidate(final int arc, final int wordIndex) {
    return (fsa.isArcFinal(arc) || isBeforeSeparator(arc))
    //candidate has proper length
        && (Math.abs(wordLen - 1 - (wordIndex)) <= effectEditDistance);
  }

  private boolean isBeforeSeparator(final int arc) {
    if (containsSeparators) {
      final int arc1 = fsa.getArc(fsa.getEndNode(arc), dictionaryMetadata.getSeparator());
      return arc1 != 0 && !fsa.isArcTerminal(arc1);
    }
    return false;
  }

  /**
   * Calculates edit distance.
   * 
   * @param i length of first word (here: misspelled) - 1;
   * @param j length of second word (here: candidate) - 1.
   * @param wordIndex (TODO: javadoc?)
   * @param candIndex (TODO: javadoc?)
   * @return Edit distance between the two words. Remarks: See Oflazer.
   */
  public int ed(final int i, final int j, final int wordIndex, final int candIndex) {
    int result;
    int a, b, c;

    if (areEqual(wordProcessed[wordIndex], candidate[candIndex])) {
      // last characters are the same
      result = hMatrix.get(i, j);
    } else if (wordIndex > 0 && candIndex > 0 && wordProcessed[wordIndex] == candidate[candIndex - 1]
        && wordProcessed[wordIndex - 1] == candidate[candIndex]) {
      // last two characters are transposed
      a = hMatrix.get(i - 1, j - 1); // transposition, e.g. ababab, ababba
      b = hMatrix.get(i + 1, j); // deletion, e.g. abab, aba
      c = hMatrix.get(i, j + 1); // insertion e.g. aba, abab
      result = 1 + min(a, b, c);
    } else {
      // otherwise
      a = hMatrix.get(i, j); // replacement, e.g. ababa, ababb
      b = hMatrix.get(i + 1, j); // deletion, e.g. ab, a
      c = hMatrix.get(i, j + 1); // insertion e.g. a, ab
      result = 1 + min(a, b, c);
    }

    hMatrix.set(i + 1, j + 1, result);
    return result;
  }

  // by Jaume Ortola
  private boolean areEqual(final char x, final char y) {
    if (x == y) {
      return true;
    }
    if (dictionaryMetadata.getEquivalentChars() != null) {
      List<Character> chars = dictionaryMetadata.getEquivalentChars().get(x);
      if (chars != null && chars.contains(y)) {
        return true;
      }
    }
    if (dictionaryMetadata.isIgnoringDiacritics()) {
      String xn = Normalizer.normalize(Character.toString(x), Form.NFD);
      String yn = Normalizer.normalize(Character.toString(y), Form.NFD);
      if (xn.charAt(0) == yn.charAt(0)) { // avoid case conversion, if possible
        return true;
      }
      if (dictionaryMetadata.isConvertingCase()) {
        //again case conversion only when needed -- we
        // do not need String.lowercase because we only check
        // single characters, so a cheaper method is enough
        if (Character.isLetter(xn.charAt(0))) {
          boolean testNeeded = Character.isLowerCase(xn.charAt(0)) != Character.isLowerCase(yn.charAt(0));
          if (testNeeded) {
            return Character.toLowerCase(xn.charAt(0)) == Character.toLowerCase(yn.charAt(0));
          }
        }
      }
      return xn.charAt(0) == yn.charAt(0);
    }
    return false;
  }

  /**
   * Calculates cut-off edit distance.
   * 
   * @param depth current length of candidates.
   * @param wordIndex (TODO: javadoc?)
   * @param candIndex (TODO: javadoc?)
   * @return Cut-off edit distance. Remarks: See Oflazer.
   */
  public int cuted(final int depth, final int wordIndex, final int candIndex) {
    final int l = Math.max(0, depth - effectEditDistance); // min chars from word to consider - 1
    final int u = Math.min(wordLen - 1 - (wordIndex - depth), depth + effectEditDistance); // max chars from word to
    // consider - 1
    int minEd = effectEditDistance + 1; // what is to be computed
    int wi = wordIndex + l - depth;
    int d;

    for (int i = l; i <= u; i++, wi++) {
      if ((d = ed(i, depth, wi, candIndex)) < minEd) {
        minEd = d;
      }
    }
    return minEd;
  }

  // Match the last letter of the candidate against two or more letters of the word.
  private int matchAnyToOne(final int wordIndex, final int candIndex) {
    if (replacementsAnyToOne.containsKey(candidate[candIndex])) {
      for (final char[] rep : replacementsAnyToOne.get(candidate[candIndex])) {
        int i = 0;
        while (i < rep.length && (wordIndex + i) < wordLen && rep[i] == wordProcessed[wordIndex + i]) {
          i++;
        }
        if (i == rep.length) {
          return i;
        }
      }
    }
    return 0;
  }

  private int matchAnyToTwo(final int wordIndex, final int candIndex) {
    if (candIndex > 0 && candIndex < candidate.length && wordIndex > 0) {
      char[] twoChar = { candidate[candIndex - 1], candidate[candIndex] };
      String sTwoChar = new String(twoChar);
      if (replacementsAnyToTwo.containsKey(sTwoChar)) {
        for (final char[] rep : replacementsAnyToTwo.get(sTwoChar)) {
          if (rep.length == 2 && wordIndex < wordLen && candidate[candIndex - 1] == wordProcessed[wordIndex - 1]
              && candidate[candIndex] == wordProcessed[wordIndex]) {
            return 0; //unnecessary replacements
          }
          int i = 0;
          while (i < rep.length && (wordIndex - 1 + i) < wordLen && rep[i] == wordProcessed[wordIndex - 1 + i]) {
            i++;
          }
          if (i == rep.length) {
            return i;
          }
        }
      }
    }
    return 0;
  }

  private static int min(final int a, final int b, final int c) {
    return Math.min(a, Math.min(b, c));
  }

  /**
   * Copy-paste of Character.isAlphabetic() (needed as we require only 1.6)
   * 
   * @param codePoint
   *          The input character.
   * @return True if the character is a Unicode alphabetic character.
   */
  static boolean isAlphabetic(final int codePoint) {
    return ((1 << Character.UPPERCASE_LETTER | 1 << Character.LOWERCASE_LETTER | 1 << Character.TITLECASE_LETTER
        | 1 << Character.MODIFIER_LETTER | 1 << Character.OTHER_LETTER | 1 << Character.LETTER_NUMBER) >> Character
        .getType(codePoint) & 1) != 0;
  }

  /**
   * Checks whether a string contains a digit. Used for ignoring words with
   * numbers
   * 
   * @param s
   *          Word to be checked.
   * @return True if there is a digit inside the word.
   */
  static boolean containsNoDigit(final String s) {
    for (int k = 0; k < s.length(); k++) {
      if (Character.isDigit(s.charAt(k))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if <code>str</code> is made up of all-uppercase characters
   * (ignoring characters for which no upper-/lowercase distinction exists).
   */
  boolean isAllUppercase(final String str) {
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (Character.isLetter(c) && Character.isLowerCase(c)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if <code>str</code> is made up of all-lowercase characters
   * (ignoring characters for which no upper-/lowercase distinction exists).
   */
  boolean isNotAllLowercase(final String str) {
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (Character.isLetter(c) && !Character.isLowerCase(c)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param str
   *          input string
   */
  boolean isNotCapitalizedWord(final String str) {
    if (isNotEmpty(str) && Character.isUpperCase(str.charAt(0))) {
      for (int i = 1; i < str.length(); i++) {
        char c = str.charAt(i);
        if (Character.isLetter(c) && !Character.isLowerCase(c)) {
          return true;
        }
      }
      return false;
    }
    return true;
  }

  /**
   * Helper method to replace calls to "".equals().
   * 
   * @param str
   *          String to check
   * @return true if string is empty OR null
   */
  static boolean isNotEmpty(final String str) {
    return str != null && str.length() != 0;
  }

  /**
   * @param str
   *          input str
   * @return Returns true if str is MixedCase.
   */
  boolean isMixedCase(final String str) {
    return !isAllUppercase(str) && isNotCapitalizedWord(str) && isNotAllLowercase(str);
  }

  /**
   * @param str The string to check.
   * @return Returns true if str is CamelCase. Note that German compounds with a dash
   *         (like "Waschmaschinen-Test") are also considered camel case by this method.
   */
  public boolean isCamelCase(final String str) {
    return isNotEmpty(str) && 
           !isAllUppercase(str) && 
           isNotCapitalizedWord(str) && 
           Character.isUpperCase(str.charAt(0)) && 
           (!(str.length() > 1) || Character.isLowerCase(str.charAt(1))) 
           && isNotAllLowercase(str);
  }

  /**
   * Used to determine whether the dictionary supports case conversions.
   * 
   * @return boolean value that answers this question in a deep and meaningful
   *         way.
   * @since 1.9
   */
  public boolean convertsCase() {
    return dictionaryMetadata.isConvertingCase();
  }

  /**
   * @param str
   *          The string to find the replacements for.
   * @param fromIndex
   *          The index from which replacements are found.
   * @param level
   *          The recursion level. The search stops if level is &gt; MAX_RECURSION_LEVEL.
   * @return A list of all possible replacements of a {#link str} given string
   */
  public List<String> getAllReplacements(final String str, final int fromIndex, final int level) {
    List<String> replaced = new ArrayList<>();
    if (level > MAX_RECURSION_LEVEL) { // Stop searching at some point
      replaced.add(str);
      return replaced;
    }
    StringBuilder sb = new StringBuilder();
    sb.append(str);
    int index = MAX_WORD_LENGTH;
    String key = "";
    int keyLength = 0;
    boolean found = false;
    // find first possible replacement after fromIndex position
    for (final String auxKey : replacementsTheRest.keySet()) {
      int auxIndex = sb.indexOf(auxKey, fromIndex);
      if (auxIndex > -1 && (auxIndex < index || (auxIndex == index && !(auxKey.length() < keyLength)))) { //select the longest possible key
        index = auxIndex;
        key = auxKey;
        keyLength = auxKey.length();
      }
    }
    if (index < MAX_WORD_LENGTH) {
      for (final String rep : replacementsTheRest.get(key)) {
        // start a branch without replacement (only once per key)
        if (!found) {
          replaced.addAll(getAllReplacements(str, index + key.length(), level + 1));
          found = true;
        }
        // avoid unnecessary replacements (ex. don't replace L by L·L when L·L already present)
        int ind = sb.indexOf(rep, fromIndex - rep.length() + 1);
        if (rep.length() > key.length() && ind > -1 && (ind == index || ind == index - rep.length() + 1)) {
          continue;
        }
        // start a branch with replacement
        sb.replace(index, index + key.length(), rep);
        replaced.addAll(getAllReplacements(sb.toString(), index + rep.length(), level + 1));
        sb.setLength(0);
        sb.append(str);
      }
    }
    if (!found) {
      replaced.add(sb.toString());
    }
    return replaced;
  }

  /**
   * Sets up the word and candidate. Used only to test the edit distance in
   * JUnit tests.
   * 
   * @param word
   *          the first word
   * @param candidate
   *          the second word used for edit distance calculation
   */
  void setWordAndCandidate(final String word, final String candidate) {
    wordProcessed = word.toCharArray();
    wordLen = wordProcessed.length;
    this.candidate = candidate.toCharArray();
    candLen = this.candidate.length;
    effectEditDistance = wordLen <= editDistance ? wordLen - 1 : editDistance;
  }

  public final int getWordLen() {
    return wordLen;
  }

  public final int getCandLen() {
    return candLen;
  }

  public final int getEffectiveED() {
    return effectEditDistance;
  }

  /**
   * Used to sort candidates according to edit distance, and possibly according
   * to their frequency in the future.
   */
  public final class CandidateData implements Comparable<CandidateData> {
    private final String word;
    private final int origDistance;
    private final int distance;

    CandidateData(final String word, final int distance) {
      this.word = word;
      this.origDistance = distance;
      this.distance = distance * FREQ_RANGES + FREQ_RANGES - getFrequency(word) - 1;
    }

    public final String getWord() {
      return word;
    }

    public final int getDistance() {
      return distance;
    }

    @Override
    public int compareTo(final CandidateData cd) {
      // Assume no overflow.
      return Integer.compare(this.distance, cd.getDistance());
    }

    @Override
    public String toString() {
      return word + '/' + distance;
    }
  }
}
