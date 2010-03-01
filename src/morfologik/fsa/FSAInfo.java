package morfologik.fsa;

import java.util.BitSet;

/**
 * Compute additional information about an FSA: number of arcs, nodes, etc.
 */
public final class FSAInfo {
	/**
	 * Computes the number of states, nodes and final states by recursively
	 * traversing the FSA.
	 */
	private static class Walker
	{
		final BitSet visitedArcs = new BitSet();
		final BitSet visitedNodes = new BitSet();

		int nodes;
		int arcs;
		int finals;

		private final FSA fsa;

		Walker(FSA fsa) {
			this.fsa = fsa;
		}

		public void visitNode(int node) {
			for (int arc = fsa.getFirstArc(node); arc != 0; arc = fsa.getNextArc(arc))
			{
				// Care for arc path merging.
				if (!visitedArcs.get(arc)) {
					arcs++;
				}
				visitedArcs.set(arc);

				if (fsa.isArcFinal(arc))
					finals++;

				if (!fsa.isArcTerminal(arc))
				{
					final int firstArc = fsa.getEndNode(arc);

					if (!visitedNodes.get(firstArc)) {
						nodes++;
					}
					visitedNodes.set(firstArc);
					visitNode(fsa.getEndNode(arc));
				}
			}
        }
	}

	/**
	 * Number of nodes in the automaton.
	 */
	public final int nodeCount;
	
	/**
	 * Number of arcs in the automaton, excluding an arcs from the zero node (initial)
	 * and an arc from the start node to the root node.
	 */
	public final int arcsCount;

	/**
	 * Number of nodes with final flag (input sequences).
	 */
	public final int finalStatesCount;

	/*
	 * 
	 */
	public FSAInfo(FSA fsa) {
		Walker w = new Walker(fsa);
		int root = fsa.getRootNode();
		if (root > 0)
		{
			w.visitNode(root);
		}

		this.nodeCount = w.nodes;
		this.arcsCount = w.arcs;
		this.finalStatesCount = w.finals;
	}
}
