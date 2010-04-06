package morfologik.fsa.characters;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import morfologik.fsa.FSAInfo;
import morfologik.fsa.Visitor;

/**
 * Utilities that apply to {@link State}s. Extracted to a separate class for
 * clarity.
 */
public class StateUtils {
	/**
	 * Returns the right-language reachable from a given graph node, formatted
	 * as an input for the graphviz package (expressed in the <code>dot</code>
	 * language).
	 */
	public static String toDot(State root) {
		final StringBuilder b = new StringBuilder("digraph Automaton {\n");
		b.append("  rankdir = LR;\n");

		final IdentityHashMap<State, Integer> codes = new IdentityHashMap<State, Integer>();
		root.preOrder(new Visitor<State>() {
			public void accept(State s) {
				if (!codes.containsKey(s))
					codes.put(s, codes.size());
			}
		});

		b.append("  initial [shape=plaintext,label=\"\"];\n");
		b.append("  initial -> ").append(codes.get(root)).append("\n\n");

		// States and transitions.
		root.preOrder(new Visitor<State>() {
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
	private static void descend(State state, StringBuilder b,
	        ArrayList<String> rl) {
		if (state.isFinal()) {
			rl.add(b.toString());
		}

		if (state.hasChildren()) {
			final State[] states = state.states;
			final char[] labels = state.labels;
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
	public static FSAInfo getInfo(State s) {
		final int [] counters = new int [] {
				0, 0, 0
		};
		s.preOrder(new Visitor<State>() {
			public void accept(State s) {
				counters[0]++; // states
				counters[1] += s.labels.length; // transitions
				if (s.isFinal()) counters[2]++; // final states
			}
		});
		return new FSAInfo(counters[0], counters[1], counters[0], counters[2]);
	}
}
