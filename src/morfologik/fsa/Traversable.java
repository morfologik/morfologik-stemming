package morfologik.fsa;

/**
 * Graph traversal acceptors.
 */
public interface Traversable<T> {
	/**
	 * Visit all sub-nodes in post-order (children first, then the node).
	 */
	public void postOrder(Visitor<? super T> v);

	/**
	 * Visit all sub-states in pre-order (the node first, then children).
	 */
	public void preOrder(Visitor<? super T> v);
}