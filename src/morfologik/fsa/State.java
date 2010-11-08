package morfologik.fsa;

import java.util.IdentityHashMap;

import morfologik.util.Arrays;


/**
 * DFSA state with <code>byte</code> labels on transitions.
 */
public final class State implements Traversable<State> {
    /**
	 * Labels of outgoing transitions. Indexed identically to {@link #states}.
	 * Labels must be sorted lexicographically.
	 */
    byte[] labels;

	/**
	 * States reachable from outgoing transitions. Indexed identically to
	 * {@link #labels}.
	 */
	State[] states;

	/**
	 * Transitions leaving this node that mark the end of an input sequence.
	 * Storing final state flag as part of the transition makes automata
	 * slightly more compact. Indexed identically to {@link #labels}
	 * and {@link #states}.
	 */
	boolean[] final_transitions;

	/**
	 * Number of arcs.
	 */
	int arcs;
	
    /**
     * Start index in parallel arrays.
     */
    int start;	

	/**
	 * An internal field for storing this state's offset during serialization.
	 */
    int offset;

    /**
     * An internal field for storing this state's count of right-language sequences
     * (for perfect hashing). Also used as a flag for interned vs. mutable state.
     */
    int number = -1;

    /**
     * Keep package-private, use the interning pool.
     */
    State() {
    }

    /**
	 * Returns the target state of a transition leaving this state and labeled
	 * with <code>label</code>. If no such transition exists, returns
	 * <code>null</code>.
	 */
	public State getState(byte label) {
		final int index = binarySearch(label);
		if (index >= 0) {
			return states[index];
		} else
			return null;
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
		final State that = (State) obj;
		
		if (that.arcs != this.arcs)
		    return false;

		return
			   Arrays.equals(this.labels, this.start, 
			                 that.labels, that.start, this.arcs)
			&& Arrays.referenceEquals(this.states, this.start, 
			                          that.states, that.start, this.arcs)
            && Arrays.equals(this.final_transitions, this.start, 
                             that.final_transitions, that.start, this.arcs);
	}

	/**
     * Return <code>true</code> if this state has any children (outgoing
     * transitions).
     */
    public boolean hasChildren() {
    	return arcs > 0;
    }

	/**
	 * Compute the hash code of the <i>current</i> status of this state.
	 */
	@Override
	public int hashCode() {
		int hash = 0;

		final int arcs = this.arcs;
		final int start = this.start;

		/*
		 * We don't include final flags here because they're mostly null; we allow
		 * more conflicts, but empirically there were nearly none. Hash on labels
		 * and states only. 
		 */

        final byte [] labels = this.labels;
        for (int i = start + arcs; --i >= start;)
			hash = hash * 31 + (labels[i] & 0xFF);

		/*
		 * Compare the right-language of this state using reference-identity of
		 * outgoing states. This is possible because states are interned (stored
		 * in registry) and traversed in post-order, so any outgoing transitions
		 * are already interned.
		 */
        final State [] states = this.states;
        for (int i = start + arcs; --i >= start;)
			hash ^= System.identityHashCode(states[i]);

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
	 * Add a single outgoing arc from this state to <code>newState</code>. Addition can
	 * only be performed if there are no arcs with this label. The label should be lexicographically
	 * higher than any other label in this state.
	 */
    State addArc(byte label, State newState, boolean finalTransition) {
        assert !interned() : "Shouldn't be interned.";
        assert start == 0 : "Expected start to be zero: " + start;
        assert binarySearch(label) < 0 : 
            "State already has transition labeled: " + label;

        final int index = arcs++; 
        labels[index] = label;
        final_transitions[index] = finalTransition;
        return states[index] = newState;
    }    

    /**
     * Binary search for <code>label</code> in the <code>array</code>.
     * We can't use {@link java.util.Arrays#binarySearch(byte[], byte)} because
     * the order of bytes in the <code>array</code> may not be the signed-value
     * order.
     */
    private final int binarySearch(byte label) {
        final byte [] a = labels; 
        final int key = label & 0xff;

        int low = start;
        int high = start + arcs - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = a[mid] & 0xff;

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid;
        }

        return -(low + 1);
    }
	
    /**
     * Return the most recent transitions's target state.
     */
    State lastChild() {
    	assert hasChildren() : "Should have outgoing transitions.";
    	assert !interned() : "Shouldn't be interned.";
    	assert start == 0 : "Expected start to be zero: " + start;

    	return states[arcs - 1];
    }

	/**
     * Return the associated state if the most recent transition
     * is labeled with <code>label</code>.
     */
    State lastChild(byte label) {
        assert !interned() : "Shouldn't be interned.";
        assert start == 0 : "Expected start to be zero: " + start;

    	final int index = arcs - 1;
    	State s = null;
    	if (index >= 0 && labels[index] == label) {
    		s = states[index];
    	}

    	assert s == getState(label) : "Expected last child.";
    	return s;
    }

	/**
     * Replace the last added outgoing transition's target state with the given
     * state.
     */
    void replaceLastChild(State state) {
        assert state.interned() : "Expected interned child.";
        assert !interned() : "Shouldn't be interned.";
        assert start == 0 : "Expected start to be zero: " + start;
    	assert hasChildren() : "No outgoing transitions.";

    	states[arcs - 1] = state;
    }

    /**
     * Reset to mutable state.
     */
    void reset() {
        arcs = 0;
        number = -1;
    }

    /**
     * This state has been interned and will not change again.
     */
    State intern(StateInterningPool pool) {
        assert !interned() : "Shouldn't be interned.";
        assert start == 0 : "Expected start to be zero: " + start;
        assert childrenInterned() : "Expected all children to be interned.";

        // Calculate the number of right-language states.
        int number = 0;
        for (int i = arcs; --i >= 0;) {
            if (final_transitions[i])
                number++;
            number += states[i].number;
        }

        // Set the right-language number, also marking this object as interned.
        this.number = number;

        // Copy over the current State's data to the interning arrays.
        return pool.intern(this);
    }
    
    /**
     * Assertion check for interned state.
     */
    boolean interned() {
        return this.number >= 0;
    }

    /**
     * Assertion that all children of this state should be interned.
     */
    private boolean childrenInterned() {
        for (int i = start + arcs; --i >= start;) {
            if (!states[i].interned())
                return false;
        }
        return true;
    }

	/**
	 * Internal recursive postorder traversal.
	 */
	private void postOrder(Visitor<? super State> v, IdentityHashMap<State, State> visited) {
		if (visited.containsKey(this))
			return;

		visited.put(this, this);
        for (int i = start; i < start + arcs; i++) {
			states[i].postOrder(v, visited);
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
		for (int i = start; i < start + arcs; i++) {
			states[i].preOrder(v, visited);
		}
	}

	/**
	 * Return the number of arcs leaving this state.
	 */
    int arcsCount() {
        return arcs;
    }

    /**
     * Return the state of arc number <code>i</code>. 
     */
    State arcState(int i) {
        return states[start + i];
    }
    
    /**
     * Return the label of arc number <code>i</code>. 
     */
    byte arcLabel(int i) {
        return labels[start + i];
    }

    /**
     * Return the final flag of arc number <code>i</code>. 
     */
    boolean arcFinal(int i) {
        return final_transitions[start + i];
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("arcs = " + arcs + ", number = " + number + " [");
        for (int i = 0; i < arcs; i++) {
            if (i > 0) b.append(", ");
            b.append((char) arcLabel(i));
            if (arcFinal(i)) b.append("!F");
        }
        b.append("]");
        return b.toString();
    }
}
