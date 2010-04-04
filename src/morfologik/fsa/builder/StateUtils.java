package morfologik.fsa.builder;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Utilities that apply to {@link State}s. Extracted to a separate class for
 * clarity.
 */
public class StateUtils {
	/**
	 * Automaton statistics.
	 */
	public static class AutomatonInfo
	{
		public int states;
		public int transitions;
		
		@Override
		public String toString() {
			return "States: " + states + ", transitions: " + transitions;
		}
	}

	/**
	 * Returns the right-language reachable from a given state as an input for
	 * graphviz package (<code>dot</code> language).
	 */
	public static String toDot(State root) {
		final StringBuilder b = new StringBuilder("digraph Automaton {\n");
		b.append("  rankdir = LR;\n");

		final IdentityHashMap<State, Integer> codes = new IdentityHashMap<State, Integer>();
		root.preOrder(new State.Visitor() {
			public void accept(State s) {
				if (!codes.containsKey(s))
					codes.put(s, codes.size());
			}
		});

		b.append("  initial [shape=plaintext,label=\"\"];\n");
		b.append("  initial -> ").append(codes.get(root)).append("\n\n");

		// States and transitions.
		root.preOrder(new State.Visitor() {
			public void accept(State s) {
				b.append("  ").append(codes.get(s));
				if (s.isFinal())
					b.append(" [shape=doublecircle,label=\"\"];\n");
				else
					b.append(" [shape=circle,label=\"\"];\n");

				int i = 0;
				for (State sub : s.states) {
					b.append("  ");
					b.append(codes.get(s));
					b.append(" -> ");
					b.append(codes.get(sub));
					b.append(" [label=\"");
					b.append(s.labels[i++]);
					b.append("\"]\n");
				}
			}
		});

		return b.append("}\n").toString();
	}

	/**
	 * All strings generated as the right language of <code>state</code>.
	 */
	public static List<String> rightLanguage(State state) {
		final ArrayList<String> rl = new ArrayList<String>();
		final StringBuilder b = new StringBuilder();

		if (state.hasChildren())
			descend(state, b, rl);

		return rl;
	}

	/**
	 * Recursive descend and collection of the right language.
	 */
	private static void descend(State state, StringBuilder b, ArrayList<String> rl) {
		if (state.isFinal()) {
			rl.add(b.toString());
		}

		if (state.hasChildren()) {
			final State [] states = state.states;
			final char [] labels = state.labels;
			for (int i = 0; i < labels.length; i++) {
				b.append(labels[i]);
				descend(states[i], b, rl);
				b.deleteCharAt(b.length() - 1);
			}
		}
	}

	/**
	 * Calculate automaton statistics.
	 */
	public static AutomatonInfo getInfo(State s) {
		final AutomatonInfo info = new AutomatonInfo();
		s.preOrder(new State.Visitor() {
			public void accept(State s) {
				info.states++;
				info.transitions += s.labels.length;
			}
		});
		return info;
	}
}
