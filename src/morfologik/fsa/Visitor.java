package morfologik.fsa;

/**
 * Visitor interface for traversals.
 */
public interface Visitor<T> {
	public boolean accept(T s);
}