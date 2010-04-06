package morfologik.fsa;

import java.util.BitSet;

/**
 * Other FSA-related utilities not directly associated with the class hierarchy.
 */
public final class FSAUtils {
	/**
	 * Returns the right-language reachable from a given FSA node, formatted
	 * as an input for the graphviz package (expressed in the <code>dot</code>
	 * language).
	 */
	public static String toDot(FSA fsa, int node) {
		final StringBuilder b = new StringBuilder("digraph Automaton {\n");
		b.append("  rankdir = LR;\n");

		final BitSet visited = new BitSet();

		b.append("  stop [shape=doublecircle,label=\"\"];\n");
		b.append("  initial [shape=plaintext,label=\"\"];\n");
		b.append("  initial -> ").append(node).append("\n\n");

		visitNode(0, b, fsa, node, visited);
		return b.append("}\n").toString();
	}

	private static void visitNode(int d, StringBuilder b, FSA fsa, int s, BitSet visited) {
		visited.set(s);
		b.append("  ").append(s);
		b.append(" [shape=circle,label=\"\"];\n");

		System.out.println("visiting: " + s);
		
		for (int arc = fsa.getFirstArc(s); arc != 0; arc = fsa.getNextArc(arc)) {
			b.append("  ");
			b.append(s);
			b.append(" -> ");
			if (fsa.isArcTerminal(arc)) {
				b.append("stop");
			} else {
				b.append(fsa.getEndNode(arc));
			}

			final byte label = fsa.getArcLabel(arc);
			b.append(" [label=\"");
			if (Character.isLetterOrDigit(label))
				b.append((char) label);
			else {
				b.append("0x");
				b.append(Integer.toHexString(label & 0xFF));
			}
			b.append("\"]\n");

			System.out.println(
					d + " " +
					(char) label + "->" 
					+ (!fsa.isArcTerminal(arc) ? fsa.getEndNode(arc) : 0)
					+ " " 
					+ (fsa.isArcFinal(arc) ? "F" : "")
					+ (fsa.isArcTerminal(arc) ? "T" : ""));
		}

		System.out.println("--");
		for (int arc = fsa.getFirstArc(s); arc != 0; arc = fsa.getNextArc(arc)) {
			if (!fsa.isArcTerminal(arc)) {
				int endNode = fsa.getEndNode(arc);
				if (!visited.get(endNode)) {
					visitNode(d + 1, b, fsa, endNode, visited);
				}
			}
		}
    }
}
