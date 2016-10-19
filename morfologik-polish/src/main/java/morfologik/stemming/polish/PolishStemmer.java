package morfologik.stemming.polish;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.List;

import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.IStemmer;
import morfologik.stemming.WordData;

/**
 * A dictionary-based stemmer for the Polish language. Instances of this class
 * are not thread safe.
 * 
 * @see morfologik.stemming.DictionaryLookup
 */
public final class PolishStemmer implements IStemmer, Iterable<WordData> {
  /**
   * The underlying dictionary, loaded once (lazily).
   */
  private static Dictionary dictionary;

  /** Dictionary lookup delegate. */
  private final DictionaryLookup lookup;

  public PolishStemmer() {
    synchronized (getClass()) {
      if (dictionary == null) {
        try {
          dictionary = AccessController.doPrivileged(new PrivilegedExceptionAction<Dictionary>() {
            @Override
            public Dictionary run() throws Exception {
              URL dictResource = getClass().getResource("polish.dict");
              if (dictResource == null) {
                throw new IOException("Polish dictionary resource not found.");
              }
              return Dictionary.read(dictResource);
            }
          });
        } catch (PrivilegedActionException e) {
          throw new RuntimeException("Could not read dictionary data.", e.getException());
        }
      }
    }

    lookup = new DictionaryLookup(dictionary);
  }

  /**
   * @return Return the underlying {@link Dictionary} driving the stemmer.
   */
  public Dictionary getDictionary() {
    return dictionary;
  }

  /**
   * {@inheritDoc}
   */
  public List<WordData> lookup(CharSequence word) {
    return lookup.lookup(word);
  }

  /**
   * Iterates over all dictionary forms stored in this stemmer.
   */
  public Iterator<WordData> iterator() {
    return lookup.iterator();
  }
}
