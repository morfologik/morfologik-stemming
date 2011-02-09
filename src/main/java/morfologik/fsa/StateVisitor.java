package morfologik.fsa;

/**
 * State visitor.
 * 
 * @see FSA#visitInPostOrder(StateVisitor)
 * @see FSA#visitInPreOrder(StateVisitor)
 */
public interface StateVisitor {
    public boolean accept(int state);
}