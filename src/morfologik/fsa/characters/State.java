package morfologik.fsa.characters;

import java.util.Arrays;
import java.util.IdentityHashMap;

import morfologik.fsa.Traversable;
import morfologik.fsa.Visitor;

/**
 * DFSA state with <code>char</code> labels on transitions.
 */
public final class State implements Traversable<State> {

	/** An empty set of labels. */
	private final static char[] NO_LABELS = new char[0];

	/** An empty set of states. */
	private final static State[] NO_STATES = new State[0];

	/**
	 * Labels of outgoing transitions. Indexed identically to {@link #states}.
	 * Labels must be sorted lexicographically.
	 */
	char[] labels = NO_LABELS;

	/**
	 * States reachable from outgoing transitions. Indexed identically to
	 * {@link #labels}.
	 */
	State[] states = NO_STATES;

	/**
	 * <code>true</code> if this state corresponds to the end of at least one
	 * input sequence.
	 */
	boolean is_final;

	/**
	 * Returns the target state of a transition leaving this state and labeled
	 * with <code>label</code>. If no such transition exists, returns
	 * <code>null</code>.
	 */
	public State getState(char label) {
		final int index = Arrays.binarySearch(labels, label);
		return index >= 0 ? states[index] : null; 
	}

	/**
	 * Returns an array of outgoing transition labels. The array is sorted in 
	 * lexicographic order and indexes correspond to states returned from 
	 * {@link #getStates()}.
	 */
	public char [] getTransitionLabels() {
		return this.labels;
	}

	/**
	 * Returns an array of outgoing transitions from this state. The returned
	 * array must not be changed.
	 */
	public State[] getStates() {
		return this.states;
	}
	
	/**
	 * Two states are equal if:
	 * <ul>
	 * <li>they have an identical number of outgoing transitions, labeled with
	 * the same labels</li>
	 * <li>corresponding outgoing transitions lead to the same states (to states
	 * with an identical right-language).
	 * </ul>
	 */
	@Override
	public boolean equals(Object obj) {
		final State other = (State) obj;
		return is_final == other.is_final
				&& Arrays.equals(this.labels, other.labels)
				&& morfologik.util.Arrays.referenceEquals(this.states, other.states);
	}

	/**
     * Return <code>true</code> if this state has any children (outgoing
     * transitions).
     */
    public boolean hasChildren() {
    	return labels.length > 0;
    }

	/**
	 * Is this state a final state in the automaton?
	 */
	public boolean isFinal() {
		return is_final;
	}

	/**
	 * Compute the hash code of the <i>current</i> status of this state.
	 */
	@Override
	public int hashCode() {
		int hash = is_final ? 1 : 0;

		hash ^= hash * 31 + this.labels.length;
		for (char c : this.labels)
			hash ^= hash * 31 + c;

		/*
		 * Compare the right-language of this state using reference-identity of
		 * outgoing states. This is possible because states are interned (stored
		 * in registry) and traversed in post-order, so any outgoing transitions
		 * are already interned.
		 */
		for (State s : this.states) {
			hash ^= System.identityHashCode(s);
		}

		return hash;
	}

	/**
	 * Visit all sub-states in post-order.
	 */
	public void postOrder(Visitor<? super State> v) {
		postOrder(v, new IdentityHashMap<State, State>());
	}

	/**
	 * Visit all sub-states in pre-order.
	 */
	public void preOrder(Visitor<? super State> v) {
		preOrder(v, new IdentityHashMap<State, State>());
	}

	/**
     * Create a new outgoing transition labeled <code>label</code> and return
     * the newly created target state for this transition.
     */
    State newState(char label) {
    	assert Arrays.binarySearch(labels, label) < 0 : "State already has transition labeled: "
    			+ label;
    
    	labels = morfologik.util.Arrays.copyOf(labels, labels.length + 1);
    	states = morfologik.util.Arrays.copyOf(states, states.length + 1);
    
    	labels[labels.length - 1] = label;
    	return states[states.length - 1] = new State();
    }

	/**
     * Return the most recent transitions's target state.
     */
    State lastChild() {
    	assert hasChildren() : "No outgoing transitions.";
    	return states[states.length - 1];
    }

	/**
     * Return the associated state if the most recent transition
     * is labeled with <code>label</code>.
     */
    State lastChild(char label) {
    	final int index = labels.length - 1;
    	State s = null;
    	if (index >= 0 && labels[index] == label) {
    		s = states[index];
    	}
    	assert s == getState(label);
    	return s;
    }

	/**
     * Replace the last added outgoing transition's target state with the given
     * state.
     */
    void replaceLastChild(State state) {
    	assert hasChildren() : "No outgoing transitions.";
    	states[states.length - 1] = state;
    }

	/**
	 * Internal recursive postorder traversal.
	 */
	private void postOrder(Visitor<? super State> v, IdentityHashMap<State, State> visited) {
		if (visited.containsKey(this))
			return;

		visited.put(this, this);
		for (State target : states) {
			target.postOrder(v, visited);
		}
		v.accept(this);
	}

	/**
	 * Internal recursive preorder traversal.
	 */
	private void preOrder(Visitor<? super State> v, IdentityHashMap<State, State> visited) {
		if (visited.containsKey(this))
			return;

		visited.put(this, this);
		v.accept(this);
		for (State target : states) {
			target.postOrder(v, visited);
		}
	}
}
