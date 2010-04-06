package morfologik.fsa.bytes;

import java.util.ArrayList;
import java.util.IdentityHashMap;

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
					if (Character.isLetterOrDigit(s.labels[i]))
						b.append((char) s.labels[i]);
					else {
						b.append("0x");
						b.append(Integer.toHexString(s.labels[i] & 0xFF));
					}
					i++;
					b.append("\"]\n");
				}
			}
		});

		return b.append("}\n").toString();
	}

	/**
	 * All byte sequences generated as the right language of <code>state</code>.
	 */
	public static ArrayList<byte[]> rightLanguage(State state) {
		final ArrayList<byte[]> rl = new ArrayList<byte[]>();
		final byte [] buffer = new byte [0];

		if (state.hasChildren())
			descend(state, buffer, 0, rl);

		return rl;
	}

	/**
	 * Recursive descend and collection of the right language.
	 */
	private static byte[] descend(State state, byte [] b, int position, 
			ArrayList<byte[]> rl) {
		if (state.isFinal()) {
			rl.add(morfologik.util.Arrays.copyOf(b, position));
		}

		if (state.hasChildren()) {
			final State[] states = state.states;
			final byte[] labels = state.labels;
			if (b.length <= position) {
				b = morfologik.util.Arrays.copyOf(b, position + 1);
			}

			for (int i = 0; i < labels.length; i++) {
				b[position] = labels[i];
				b = descend(states[i], b, position + 1, rl);
			}
		}
		return b;
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
