package morfologik.fsa;

import java.util.Arrays;
import java.util.IdentityHashMap;


/**
 * DFSA state with <code>byte</code> labels on transitions.
 */
public final class State implements Traversable<State> {

	/** An empty set of labels. */
	private final static byte[] NO_LABELS = new byte[0];

	/** An empty set of states. */
	private final static State[] NO_STATES = new State[0];

	/** An empty list of final flags. */
	private final static boolean[] NO_FINALS = new boolean[0];

	/**
	 * Labels of outgoing transitions. Indexed identically to {@link #states}.
	 * Labels must be sorted lexicographically.
	 */
	byte[] labels = NO_LABELS;

	/**
	 * States reachable from outgoing transitions. Indexed identically to
	 * {@link #labels}.
	 */
	State[] states = NO_STATES;

	/**
	 * Transitions leaving this node that mark the end of an input sequence.
	 * Storing final state flag as part of the transition makes automata
	 * slightly more compact. Indexed identically to {@link #labels}
	 * and {@link #states}.
	 */
	boolean[] final_transitions = NO_FINALS;

	/**
	 * Returns the target state of a transition leaving this state and labeled
	 * with <code>label</code>. If no such transition exists, returns
	 * <code>null</code>.
	 */
	public State getState(byte label) {
		final int index = Arrays.binarySearch(labels, label);
		if (index >= 0) {
			return states[index];
		} else
			return null;
	}

	/**
	 * Returns an array of outgoing transition labels. The array is sorted in 
	 * lexicographic order and indexes correspond to states returned from 
	 * {@link #getStates()}.
	 */
	public byte [] getTransitionLabels() {
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
	 * the same labels,</li>
	 * <li>transitions have identical final flags,</li>
	 * <li>corresponding outgoing transitions lead to the same states (to states
	 * with an identical right-language).
	 * </ul>
	 */
	@Override
	public boolean equals(Object obj) {
		final State other = (State) obj;
		return 
			Arrays.equals(this.labels, other.labels)
			&& Arrays.equals(this.final_transitions, other.final_transitions)
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
	 * Compute the hash code of the <i>current</i> status of this state.
	 */
	@Override
	public int hashCode() {
		int hash = 0;

		for (boolean b : this.final_transitions)
			hash = hash * 17 + (b ? 1 : 0);

		for (byte c : this.labels)
			hash = hash * 31 + (c & 0xFF);

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
    State newState(byte label, boolean finalTransition) {
    	assert Arrays.binarySearch(labels, label) < 0 : 
    		"State already has transition labeled: " + label;

    	final int newLength = labels.length + 1;
    	labels = morfologik.util.Arrays.copyOf(labels, newLength);
    	states = morfologik.util.Arrays.copyOf(states, newLength);
    	final_transitions = morfologik.util.Arrays.copyOf(final_transitions, newLength);

    	final int index = labels.length - 1; 
    	labels[index] = label;
    	final_transitions[index] = finalTransition;
    	return states[index] = new State();
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
    State lastChild(byte label) {
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
			target.preOrder(v, visited);
		}
	}
}
