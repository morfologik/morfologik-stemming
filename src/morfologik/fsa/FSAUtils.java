package morfologik.fsa;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.TreeMap;

import com.carrotsearch.hppc.IntIntOpenHashMap;

/**
 * Other FSA-related utilities not directly associated with the class hierarchy.
 */
public final class FSAUtils {
    public final static class IntIntHolder {
        public int a;
        public int b;
        
        public IntIntHolder(int a, int b) {
            this.a = a;
            this.b = b;
        }

        public IntIntHolder() {
        }
    }

    /**
     * Returns the right-language reachable from a given FSA node, formatted
     * as an input for the graphviz package (expressed in the <code>dot</code>
     * language).
     */
	public static String toDot(FSA fsa, int node) {
		try {
    		StringWriter w = new StringWriter();
    		toDot(w, fsa, node);
    		return w.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Saves the right-language reachable from a given FSA node, formatted
	 * as an input for the graphviz package (expressed in the <code>dot</code>
	 * language), to the given writer.
	 */
	public static void toDot(Writer w, FSA fsa, int node) throws IOException {
		w.write("digraph Automaton {\n");
		w.write("  rankdir = LR;\n");

		final BitSet visited = new BitSet();

		w.write("  stop [shape=doublecircle,label=\"\"];\n");
		w.write("  initial [shape=plaintext,label=\"\"];\n");
		w.write("  initial -> " + node + "\n\n");

		visitNode(w, 0, fsa, node, visited);
		w.write("}\n"); 
	}

	private static void visitNode(Writer w, int d, FSA fsa, int s, BitSet visited) throws IOException {
		visited.set(s);
		w.write("  "); w.write(Integer.toString(s));

		if (fsa.getFlags().contains(FSAFlags.NUMBERS)) {
			int nodeNumber = fsa.getRightLanguageCount(s);
			w.write(" [shape=circle,label=\"" + nodeNumber + "\"];\n");
		} else {
			w.write(" [shape=circle,label=\"\"];\n");
		}

		for (int arc = fsa.getFirstArc(s); arc != 0; arc = fsa.getNextArc(arc)) {
			w.write("  ");
			w.write(Integer.toString(s));
			w.write(" -> ");
			if (fsa.isArcTerminal(arc)) {
				w.write("stop");
			} else {
				w.write(Integer.toString(fsa.getEndNode(arc)));
			}

			final byte label = fsa.getArcLabel(arc);
			w.write(" [label=\"");
			if (Character.isLetterOrDigit(label))
				w.write((char) label);
			else {
				w.write("0x");
				w.write(Integer.toHexString(label & 0xFF));
			}
			w.write("\"");
			if (fsa.isArcFinal(arc)) w.write(" arrowhead=\"tee\"");
			if (fsa instanceof FSA5) {
				if (((FSA5) fsa).isNextSet(arc)) {
					w.write(" color=\"blue\"");
				}
			}

			w.write("]\n");
		}

		for (int arc = fsa.getFirstArc(s); arc != 0; arc = fsa.getNextArc(arc)) {
			if (!fsa.isArcTerminal(arc)) {
				int endNode = fsa.getEndNode(arc);
				if (!visited.get(endNode)) {
					visitNode(w, d + 1, fsa, endNode, visited);
				}
			}
		}
    }

    /**
     * All byte sequences generated as the right language of <code>state</code>.
     */
    public static ArrayList<byte[]> rightLanguage(FSA fsa, int state) {
        final ArrayList<byte[]> rl = new ArrayList<byte[]>();
        final byte [] buffer = new byte [0];

        descend(fsa, state, buffer, 0, rl);

        return rl;
    }

    /**
     * Recursive descend and collection of the right language.
     */
    private static byte [] descend(FSA fsa, int state, byte [] b, int position, ArrayList<byte[]> rl) {

        if (b.length <= position) {
            b = Arrays.copyOf(b, position + 1);
        }

        for (int arc = fsa.getFirstArc(state); arc != 0; arc = fsa.getNextArc(arc)) {
            b[position] = fsa.getArcLabel(arc);

            if (fsa.isArcFinal(arc)) {
                rl.add(Arrays.copyOf(b, position + 1));
            }

            if (!fsa.isArcTerminal(arc))
                b = descend(fsa, fsa.getEndNode(arc), b, position + 1, rl);
        }

        return b;
    }

    /**
     * Calculate fan-out ratio.
     * @return The returned array: result[outgoing-arcs]
     */
    public static TreeMap<Integer, Integer> calculateFanOuts(final FSA fsa, int root) {
        final int [] result = new int [256];
        fsa.visitInPreOrder(new StateVisitor() {
            public boolean accept(int state) {
                int count = 0;
                for (int arc = fsa.getFirstArc(state); arc != 0; arc = fsa.getNextArc(arc))
                    count++;
                result[count]++;
                return true;
            }
        });

        TreeMap<Integer, Integer> output = new TreeMap<Integer, Integer>();
        
        int low = 1; // Omit #0, there is always a single node like that (dummy).
        while (low < result.length && result[low] == 0) low++;

        int high = result.length - 1;
        while (high >= 0 && result[high] == 0) high--;

        for (int i = low; i <= high; i++) {
            output.put(i, result[i]);
        }

        return output;
    }

    /**
     * Calculate the size of right language for each state in an FSA.
     */
    public static IntIntOpenHashMap rightLanguageForAllStates(final FSA fsa) {
        final IntIntOpenHashMap numbers = new IntIntOpenHashMap();

        fsa.visitInPostOrder(new StateVisitor() {
            public boolean accept(int state) {
                int thisNodeNumber = 0;
                for (int arc = fsa.getFirstArc(state); arc != 0; arc = fsa.getNextArc(arc)) {
                    thisNodeNumber +=
                        (fsa.isArcFinal(arc) ? 1 : 0) +
                        (fsa.isArcTerminal(arc) ? 0 : numbers.get(fsa.getEndNode(arc)));
                }
                numbers.put(state, thisNodeNumber);

                return true;
            }
        });
        
        return numbers;
    }
}
