package morfologik.stemmers;

import java.io.IOException;


/**
 * A hybrid stemmer for the Polish language (a combination of {@link Lametyzator},
 * a dictionary-based stemmer and <a href="http://www.getopt.org/stempel">Stempel</a>,
 * a heuristic stemmer).
 * 
 * @author Dawid Weiss
 */
public final class Stempelator extends CascadedStemmer {
    /**
     * <p>Instantiate Stempel and {@link Lametyzator} with default
     * dictionaries.
     * 
     * <p><b>Instantiation can be quite time-consuming. Cache
     * instances of the stemmer and reuse them.</b> IStemmer
     * objects are thread-safe. 
     */
    public Stempelator() throws IOException {
        super(new IStemmer [] {
                new Lametyzator(),
                new Stempel()
        });
    }
}
