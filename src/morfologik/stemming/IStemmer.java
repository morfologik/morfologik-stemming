package morfologik.stemming;

import java.util.List;

/**
 * A generic &quot;stemmer&quot; interface in Morfologik.
 */
public interface IStemmer {
    /**
     * Returns a list of {@link WordData} entries for a given word. The returned
     * list is never <code>null</code>. Depending on the stemmer's
     * implementation the {@link WordData} may carry the stem and additional
     * information (tag) or just the stem.
     * <p>
     * The returned list and any object it contains are not usable after a
     * subsequent call to this method. Any data that should be stored in between
     * must be copied by the caller.
     */
    public List<WordData> lookup(CharSequence word);
}
