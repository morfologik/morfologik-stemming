package morfologik.fsa.dictionary;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import morfologik.fsa.core.FSA;
import morfologik.fsa.core.FSAMatch;
import morfologik.fsa.core.FSATraversalHelper;
import morfologik.stemmers.IStemmer;

/**
 * <p>This class implements a dictionary lookup over an FSA dictionary.
 * 
 * <p>Please note that FSA's in Jan Daciuk's implementation use <em>bytes</em>
 * not unicode characters. Therefore objects of this class always have to be
 * constructed with an encoding used to convert Java strings to byte arrays and
 * vice versa. The dictionary for this class should be created using Jan Daciuk's 
 * FSA package.
 * 
 * @see <a href="http://www.eti.pg.gda.pl/~jandac/fsa.html">FSA package Web site</a> 
 * @author Dawid Weiss
 */
public final class DictionaryStemmer implements IStemmer {
  /** An empty array used for 'no-matches' return. */
  private final static String[] NO_STEM = null;

  /** A Finite State Automaton used for look ups. */
  private final FSATraversalHelper matcher;

  /** FSA's root node. */
  private final FSA.Node root;  

  /** 
   * Features of the compiled dictionary.
   * 
   * @see DictionaryFeatures
   */
  private final DictionaryFeatures dictionaryFeatures;


  /**
   * <p>Creates a new object of this class using the given FSA for word lookups
   * and encoding for converting characters to bytes.
   * 
   * @throws UnsupportedEncodingException if the given encoding is not 
   *         found in the system.
   * @throws IllegalArgumentException if FSA's root node cannot be acquired
   *         (dictionary is empty).
   */
  public DictionaryStemmer(Dictionary dictionary)
  throws UnsupportedEncodingException, IllegalArgumentException
  {
    this.dictionaryFeatures = dictionary.features;
    this.root = dictionary.fsa.getStartNode();
    this.matcher = dictionary.fsa.getTraversalHelper();    

    if (root == null) {
      throw new IllegalArgumentException("Dictionary must have at least the root node.");
    }

    if (dictionaryFeatures == null) {
      throw new IllegalArgumentException("Dictionary features must not be null.");
    }
  }

  /**
   * @see IStemmer#stem(String)
   */
  public String[] stem(String word) {
    return lookup(word, false);
  }

  /**
   * @see IStemmer#stemAndForm(String)
   */
  public String[] stemAndForm(final String word) {
    return lookup(word, true);
  }

  /**
   * Searches the automaton for lemmas of the inflected form of a
   * <code>word</code>. The result is an array of lemmas or pairs of
   * (lemma, tag), if <code>returnForms</code> parameter is
   * <code>true</code>.
   */
  private String[] lookup(final String word, final boolean returnForms) {
    final String encoding = dictionaryFeatures.encoding;
    final byte separator = dictionaryFeatures.separator;

    try {
      // try to find a partial match in the dictionary.
      final FSAMatch match = matcher.matchSequence(word.getBytes(encoding), root);

      if (match.getMatchResult() == FSAMatch.PREMATURE_WORD_END_FOUND) {
        // the entire sequence fit into the dictionary. Now a separator
        // should be the next
        // character.
        final FSA.Arc arc = match.getMismatchNode().getArcLabelledWith(separator);

        // The situation when the arc points to a final node should
        // NEVER happen. After all, we want the word to have SOME base form.
        if (arc != null && !arc.isFinal()) {
          // there is such word in the dictionary. Return its base forms.
          final ArrayList forms = new ArrayList(2);
          final Iterator i = matcher.getAllSubsequences(arc.getDestinationNode());                    
          while (i.hasNext()) {
            final byte[] baseCompressed = (byte[]) i.next();

            // look for the delimiter of the 'grammar' tag in
            // the original Jan Daciuk's FSA format.
            int j;
            for (j = 0; j < baseCompressed.length; j++) {
              if (baseCompressed[j] == separator) break;
            }

            // now, expand the prefix/ suffix 'compression'
            // and store the base form.
            byte[] byteBuf = new byte[j];
            System.arraycopy(baseCompressed, 0, byteBuf, 0, j);
            forms.add(decompress(byteBuf, word));

            // if needed, store the tag as well.
            if (returnForms) {
              j = j + 1;
              forms.add(new String(baseCompressed, j, baseCompressed.length - j, encoding));
            }
          }

          if (forms.size() == 0) {
            return NO_STEM;
          }

          return (String[]) forms.toArray(new String[forms.size()]);
        }
      } else {
        // this case is somewhat confusing: we should have hit the
        // separator first...
        // I don't really know how to deal with it at the time being.
      }

      return NO_STEM;
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unexpected Exception: " + e.toString());
    }
  }

  /**
   * Decode the base form of an inflected word.
   */
  private String decompress(byte[] encodedBase, String inflected) {
    final int encodedBaseLength = encodedBase.length;
    final boolean fsaPrefixes = this.dictionaryFeatures.usesPrefixes;
    final boolean fsaInfixes = this.dictionaryFeatures.usesInfixes;
    String returnString = "";
    boolean getBase = false;
    try {
      if (encodedBaseLength > 0) {
        final int code0 = (encodedBase[0] - 'A');      
        int infLen = 0;      
        infLen = inflected.getBytes(this.dictionaryFeatures.encoding).length;
        ByteBuffer bb = ByteBuffer.allocate(infLen + encodedBaseLength);
        if (!fsaPrefixes && !fsaInfixes) {
          if ((code0 >= 0)) {
            if (code0 <= infLen) {              
              bb.put(inflected.getBytes(this.dictionaryFeatures.encoding), 0, infLen - code0);
              bb.put(encodedBase, 1, encodedBaseLength - 1);
              returnString = new String(bb.array(), 0, bb.position(), this.dictionaryFeatures.encoding);
            }                                  
          } else {
            getBase = true;
          }
        } else if (fsaPrefixes && !fsaInfixes) {
          if (encodedBaseLength > 1 && code0 >= 0) {            
            final int stripAtEnd = encodedBase[1] - 'A';
            if (stripAtEnd <= infLen && code0 <= infLen) {
              bb.put(inflected.getBytes(this.dictionaryFeatures.encoding), code0, infLen - (stripAtEnd + code0));
              bb.put(encodedBase, 2, encodedBaseLength - 2);
              returnString = new String(bb.array(), 0, bb.position(), this.dictionaryFeatures.encoding);
            } 
          } else {
            getBase = true;
          }
        } else if (fsaInfixes) { // note: prefixes are silently assumed here
          if (encodedBaseLength > 2 && code0 >= 0) {            
            final int stripAtBeginning = encodedBase[1] - 'A';
            final int stripAtEnd = encodedBase[2] - 'A';
            if (code0 <= infLen && code0 + stripAtBeginning <= infLen && stripAtEnd <= infLen) {
              bb.put(inflected.getBytes(this.dictionaryFeatures.encoding), 0, code0);
              bb.put(inflected.getBytes(this.dictionaryFeatures.encoding), code0 + stripAtBeginning, infLen - (stripAtEnd + code0 + stripAtBeginning));
              bb.put(encodedBase, 3, encodedBaseLength - 3);               
              returnString = new String(bb.array(), 0, bb.position(), this.dictionaryFeatures.encoding);
            } 
          } else {
            getBase = true;
          }
        } else {
          getBase = true;
        }

      } else {
        getBase = true;
      } 

      if (getBase) {
        // shouldn't happen, but if so, simply return the
        // encodedBase
        returnString = new String(encodedBase, 0, encodedBase.length, this.dictionaryFeatures.encoding);
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unexpected Exception: " + e.toString());
    }

    return returnString;
  }
}
