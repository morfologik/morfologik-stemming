package morfologik.fsa;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

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
			int nodeNumber = fsa.getNumberAtNode(s);
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
			if (fsa instanceof CFSA) {
				if (((CFSA) fsa).isNextSet(arc)) {
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
	 * Return an {@link Iterable} over all suffixes from a given node. 
	 */
	static Iterable<ByteBuffer> getSequences(final FSA fsa, final int node) {
		if (node == 0) {
			return Collections.<ByteBuffer> emptyList();
		}

		return new Iterable<ByteBuffer>() {
			public Iterator<ByteBuffer> iterator() {
			    return new FSAFinalStatesIterator(fsa, node);
			}
		};
    }
}
