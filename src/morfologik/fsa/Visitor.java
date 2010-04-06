package morfologik.fsa;

/**
 * Visitor interface for traversals.
 */
public interface Visitor<T> {
	public void accept(T s);
}