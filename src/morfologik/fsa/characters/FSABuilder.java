package morfologik.fsa.characters;

import java.util.Comparator;
import java.util.HashMap;

/**
 * Automaton builder that uses <code>char</code>s to label transitions. 
 */
public class FSABuilder {
	/**
	 * Lexicographic order of input sequences.
	 */
	public final static Comparator<CharSequence> LEXICOGRAPHIC_ORDER = new Comparator<CharSequence>() {
		public int compare(CharSequence s1, CharSequence s2) {
			final int lens1 = s1.length();
			final int lens2 = s2.length();
			final int max = Math.min(lens1, lens2);

			for (int i = 0; i < max; i++) {
				final char c1 = s1.charAt(i);
				final char c2 = s2.charAt(i);
				if (c1 != c2)
					return c1 - c2;
			}
			return lens1 - lens2;
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
	 * Previous sequence added to the automaton in {@link #add(CharSequence)}.
	 */
	private StringBuilder previous;

	/**
	 * Add another character sequence to this automaton. The sequence must be
	 * lexicographically larger or equal compared to any previous sequences
	 * added to this automaton (the input must be sorted).
	 */
	public void add(CharSequence current) {
		assert register != null : "Automaton already built.";
		assert current.length() > 0 : "Input sequences must not be empty.";
		assert previous == null || LEXICOGRAPHIC_ORDER.compare(previous, current) <= 0 : 
			"Input must be sorted: " + previous + " >= " + current;
		assert setPrevious(current);

		// Descend in the automaton (find matching prefix). 
		int pos = 0, max = current.length();
		State next, state = root;
		while (pos < max && (next = state.lastChild(current.charAt(pos))) != null) {
			state = next;
			pos++;
		}

		if (state.hasChildren())
			replaceOrRegister(state);

		addSuffix(state, current, pos);
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

		register = null;
		return root;
    }

	/**
     * Build a minimal, deterministic automaton from a sorted list of strings.
     */
    public static State build(CharSequence[] input) {
    	final FSABuilder builder = new FSABuilder(); 
    
    	for (CharSequence chs : input)
    		builder.add(chs);
    
    	return builder.complete();
    }

	/**
     * Copy <code>current</code> into an internal buffer.
     */
    private boolean setPrevious(CharSequence current) {
    	if (previous == null) 
    		previous = new StringBuilder();
    	
    	previous.setLength(0);
    	previous.append(current);
    
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
			register.put(child, child);
		}
	}

	/**
	 * Add a suffix of <code>current</code> starting at <code>fromIndex</code>
	 * (inclusive) to state <code>state</code>.
	 */
	private void addSuffix(State state, CharSequence current, int fromIndex) {
		final int len = current.length();
		for (int i = fromIndex; i < len; i++) {
			state = state.newState(current.charAt(i));
		}
		state.is_final = true;
	}
}
