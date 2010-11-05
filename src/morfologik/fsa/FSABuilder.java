package morfologik.fsa;

import java.util.Comparator;
import java.util.HashMap;

import morfologik.util.Arrays;

/**
 * Automaton builder that uses <code>byte</code>s to label transitions. 
 */
public class FSABuilder {
	/**
	 * Lexicographic order of input sequences.
	 */
	public static int compare(byte [] s1, int lens1, byte [] s2, int lens2) {
		final int max = Math.min(lens1, lens2);

		for (int i = 0; i < max; i++) {
			final byte c1 = s1[i];
			final byte c2 = s2[i];
			if (c1 != c2)
				return c1 - c2;
		}
		return lens1 - lens2;
	}

	/**
	 * Comparator comparing full byte arrays.
	 */
	public static final Comparator<byte[]> LEXICAL_ORDERING = new Comparator<byte[]>() {
		public int compare(byte[] o1, byte[] o2) {
		    return FSABuilder.compare(o1, o1.length, o2, o2.length);
		}
	};

	/**
	 * "register" for state interning.
	 */
	private HashMap<State, State> register = new HashMap<State, State>();

	/**
	 * Root automaton state.
	 */
	private State root = new State();

	/**
	 * Previous sequence added to the automaton in {@link #add(byte[], int)}. Used
	 * in assertions only.
	 */
	private byte [] previous;
	private int previousLength;

	/**
	 * Add another byte sequence to this automaton. The sequence must be
	 * lexicographically larger or equal compared to any previous sequences
	 * added to this automaton (the input must be sorted).
	 */
	public void add(byte [] current, int length) {
		assert register != null : "Automaton already built.";
		assert length > 0 : "Input sequences must not be empty.";
		assert previous == null || compare(previous, previousLength, current, length) <= 0 : 
			"Input must be sorted: " 
				+ Arrays.toString(previous, previousLength) + " >= " 
				+ Arrays.toString(current, length);
		assert setPrevious(current, length);

		// Descend in the automaton (find matching prefix). 
		int pos = 0, max = length;
		State next, state = root;
		while (pos < max && (next = state.lastChild(current[pos])) != null) {
			state = next;
			pos++;
		}

		if (state.hasChildren())
			replaceOrRegister(state);

		addSuffix(state, current, length, pos);
    }

	/**
	 * Finalize the automaton and return the root state. No more strings can be
	 * added to the builder after this call.
	 * 
	 * @return Root automaton state.
	 */
	public State complete() {
		if (this.register == null)
			throw new IllegalStateException();

		if (root.hasChildren())
			replaceOrRegister(root);

        register = null; // Help the GC.

		root.intern();
		return root;
    }

	/**
     * Build a minimal, deterministic automaton from a sorted list of byte sequences.
     */
    public static State build(byte[][] input) {
    	final FSABuilder builder = new FSABuilder(); 
    
    	for (byte [] chs : input)
    		builder.add(chs, chs.length);

    	return builder.complete();
    }

	/**
     * Build a minimal, deterministic automaton from an iterable list of byte sequences.
     */
    public static State build(Iterable<byte[]> input) {
    	final FSABuilder builder = new FSABuilder(); 

    	for (byte [] chs : input)
    		builder.add(chs, chs.length);

    	return builder.complete();
    }

	/**
     * Copy <code>current</code> into an internal buffer.
     */
    private boolean setPrevious(byte [] current, int length) {
    	if (previous == null || previous.length < current.length) {
    		previous = new byte [current.length];
    	}
    	System.arraycopy(current, 0, previous, 0, length);
    	previousLength = length;
        return true;
    }

	/**
	 * Replace last child of <code>state</code> with an already registered
	 * state or register the last child state.
	 */
	private void replaceOrRegister(State state) {
		final State child = state.lastChild();

		if (child.hasChildren())
			replaceOrRegister(child);

		final State registered = register.get(child);
		if (registered != null) {
			state.replaceLastChild(registered);
		} else {
		    child.intern();
			register.put(child, child);
		}
	}

	/**
	 * Add a suffix of <code>current</code> starting at <code>fromIndex</code>
	 * (inclusive) to state <code>state</code>.
	 */
	private void addSuffix(State state, byte[] current, int length, int fromIndex) {
		final int lastIndex = length - 1;
		for (int i = fromIndex; i <= lastIndex; i++) {
			state = state.newState(current[i], i == lastIndex);
		}
	}
}
