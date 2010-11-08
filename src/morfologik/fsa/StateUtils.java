package morfologik.fsa;

import java.util.ArrayList;
import java.util.IdentityHashMap;


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
				b.append(" [shape=circle,label=\"\"];\n");

				for (int i = 0; i < s.arcsCount(); i++) {
				    final State sub = s.arcState(i);

					b.append("  ");
					b.append(codes.get(s));
					b.append(" -> ");
					b.append(codes.get(sub));
					b.append(" [label=\"");
					if (Character.isLetterOrDigit(s.arcLabel(i)))
						b.append((char) s.arcLabel(i));
					else {
						b.append("0x");
						b.append(Integer.toHexString(s.arcLabel(i) & 0xFF));
					}
					b.append("\"");
					if (s.arcFinal(i)) b.append(" arrowhead=\"tee\"");
					b.append("]\n");
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
		if (state.hasChildren()) {
			if (b.length <= position) {
				b = morfologik.util.Arrays.copyOf(b, position + 1);
			}

			for (int i = 0; i < state.arcsCount(); i++) {
				b[position] = state.arcLabel(i);

				if (state.arcFinal(i)) {
					rl.add(morfologik.util.Arrays.copyOf(b, position + 1));
				}

				b = descend(state.arcState(i), b, position + 1, rl);
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
				// states
				counters[0]++; 
				// transitions
				counters[1] += s.arcsCount();
				// final states
				for (int i = 0; i < s.arcsCount(); i++)
					if (s.arcFinal(i)) counters[2]++;
			}
		});
		return new FSAInfo(counters[0], counters[1], counters[1], counters[2]);
	}
}
