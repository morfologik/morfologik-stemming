package morfologik.fsa.builder;

/**
 * State visitor for traversals.
 */
public interface Visitor<T> {
	public void accept(T s);
}