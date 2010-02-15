package morfologik.fsa;

import static morfologik.fsa.FSA5.ADDRESS_OFFSET;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

/**
 * Re-encode FSA5 automaton to a more compact representation ({@link CFSA}).
 */
public final class CFSAEncoder {
	/**
	 * The FSA to shrink.
	 */
	private final FSA5 fsa;

	/**
	 * A list of all arcs and extra elements in the {@link #fsa} data stream,
	 * ordered.
	 */
	private ArrayList<StreamElement> nodes = new ArrayList<StreamElement>();

	/**
	 * A mapping of compressed labels.
	 */
	private final byte[] compressedLabels = new byte[1 << 5];

	/**
	 * Indicates which labels are compressed.
	 */
	private final byte[] compressedLabelsIndex = new byte[256];

	/**
	 * A single element in the {@link FSA5} data stream.
	 */
	private abstract class StreamElement {
		/**
		 * The address (offset) in the original data stream.
		 */
		public final int address;

		/**
		 * Recalculated node's offset.
		 */
		public int offset;

		protected StreamElement(int address) {
			assert address >= 0 : "Address must not be negative.";
			this.address = address;
		}

		/**
		 * Serialize this FSA element to the stream.
		 */
		public abstract void serialize(OutputStream os) throws IOException;

		/**
		 * Calculate the current representation's length.
		 */
		public abstract int size();
	}

	/**
	 * Representation of node's data, if any.
	 */
	private class Node extends StreamElement {
		public Node(int address) {
			super(address);
		}

		/**
		 * Serialize the current node's state to an output stream.
		 */
		public void serialize(OutputStream os) throws IOException {
			if (fsa.nodeDataLength > 0) {
				os.write(fsa.arcs, address, fsa.nodeDataLength);
			}
		}

		/* */
		public int size() {
			return fsa.nodeDataLength;
		}
	}

	/**
	 * Representation of a single arc.
	 */
	private class Arc extends StreamElement {
		public byte label;
		public boolean flagFinal;
		public boolean flagLast;
		public boolean flagNext;

		/** Target node for this arc. */
		public StreamElement target;

		public Arc(int address) {
			super(address);

			this.flagFinal = fsa.isArcFinal(address);
			this.flagLast = fsa.isArcLast(address);
			this.flagNext = fsa.isNextSet(address);
			this.label = fsa.getArcLabel(address);
		}

		/**
		 * Serialize the current arc's state to an output stream.
		 */
		public void serialize(OutputStream os) throws IOException {
			// Prepare flags.
			int flags = (flagFinal ? FSA5.BIT_FINAL_ARC : 0)
			        | (flagLast ? FSA5.BIT_LAST_ARC : 0)
			        | (flagNext ? FSA5.BIT_TARGET_NEXT : 0);

			if (flagNext) {
				byte index = compressedLabelsIndex[label & 0xff];
				if (index >= 0) {
					// arc version 1
					os.write((index << 3) | flags);
				} else {
					// arc version 2
					os.write(flags);
					os.write(label);
				}
			} else {
				// flags, label, goto field
				int combined = (target.offset << 3) | flags;
				os.write(combined & 0xff);
				os.write(label);
				for (int i = 1; i < fsa.gotoLength; i++) {
					combined >>>= 8;
					os.write(combined & 0xff);
				}
			}
		}

		/* */
		public int size() {
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				serialize(baos);
				return baos.size();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
     * 
     */
	CFSAEncoder(FSA fsa) {
		if (!(fsa instanceof FSA5))
			throw new IllegalArgumentException("FSA in version 5 expected.");

		this.fsa = (FSA5) fsa;
		createNodeRepresentation();
	}

	/**
	 * Create initial representation as a linear sequence of
	 * {@link StreamElement}s.
	 */
	private void createNodeRepresentation() {
		final HashMap<Integer, StreamElement> indexByAddress = new HashMap<Integer, StreamElement>();

		StreamElement n;
		int arcOffset = 0;

		// Add root node (sink) first.
		nodes.add(n = new Node(arcOffset));
		indexByAddress.put(arcOffset, n);
		arcOffset += fsa.nodeDataLength;

		// Add a single arc pointing to the sink.
		nodes.add(n = new Arc(arcOffset));
		indexByAddress.put(arcOffset, n);
		arcOffset += (1 + fsa.gotoLength);

		if (fsa.nodeDataLength > 0) {
			/*
			 * There is a node after the first state, even though it is not
			 * consistently marked in the original FSA (sink node has no flags).
			 */
			nodes.add(n = new Node(arcOffset));
			indexByAddress.put(arcOffset, n);
			arcOffset += fsa.nodeDataLength;
		}

		byte flags;
		while (arcOffset < fsa.arcs.length) {
			// Process the arc.
			nodes.add(n = new Arc(arcOffset));
			indexByAddress.put(arcOffset, n);
			flags = fsa.arcs[arcOffset + ADDRESS_OFFSET];
			if ((flags & FSA5.BIT_TARGET_NEXT) != 0) {
				/* The next arc is right after this one (no address). */
				arcOffset += ADDRESS_OFFSET + 1;
			} else {
				/* The next arc is after the address. */
				arcOffset += ADDRESS_OFFSET + fsa.gotoLength;
			}

			if ((flags & FSA5.BIT_LAST_ARC) != 0) {
				if (arcOffset + fsa.nodeDataLength < fsa.arcs.length) {
					nodes.add(n = new Node(arcOffset));
					indexByAddress.put(arcOffset, n);
				}

				/*
				 * This is the last arc of the current node, skip the next
				 * node's data.
				 */
				arcOffset += fsa.nodeDataLength;
			}
		}

		/*
		 * Update target nodes for ArcNodes.
		 */
		for (int i = 0; i < nodes.size(); i++) {
			n = nodes.get(i);
			if (n instanceof Arc) {
				final Arc arc = (Arc) n;
				arc.target = indexByAddress.get(fsa
				        .getDestinationNodeOffset(arc.address));
				assert arc.target != null : "No target node for arc "
				        + arc.address + " -> "
				        + fsa.getDestinationNodeOffset(arc.address) + " " + i;
			}
		}

		/*
		 * Clean up additional structures.
		 */
		Arrays.fill(compressedLabelsIndex, (byte) -1);
		Arrays.fill(compressedLabels, (byte) 0);
	}

	/**
	 * Attempt to fit labels into the flags field in arcs with NEXT bit set.
	 */
	public void doLabelMapping() {
		class IndexValue {
			int index;
			int value;

			public IndexValue(int index) {
				this.index = index;
			}
		}

		IndexValue[] counts = new IndexValue[256];
		for (int i = 0; i < counts.length; i++)
			counts[i] = new IndexValue(i);

		// Count the distribution of labels in NEXT arcs.
		for (StreamElement n : nodes) {
			if (n instanceof Arc) {
				final Arc a = (Arc) n;
				if (a.flagNext) {
					counts[a.label & 0xff].value++;
				}
			}
		}

		// Pick the most frequent labels for mapping.
		Arrays.sort(counts, new Comparator<IndexValue>() {
			public int compare(IndexValue o1, IndexValue o2) {
				return o2.value - o1.value;
			};
		});

		for (int i = 0; i < (1 << 5) - 1; i++) {
			compressedLabelsIndex[counts[i].index] = (byte) (i + 1);
			compressedLabels[i + 1] = (byte) counts[i].index;
		}
	}

	/**
	 * Update node offsets according to their current states and encoding
	 * schemes.
	 * 
	 * @return Returns the total length of automaton's arcs and nodes at the
	 *         moment.
	 */
	public int updateOffsets() {
		int offset = 0;
		for (StreamElement n : nodes) {
			n.offset = offset;
			offset += n.size();
		}
		return offset;
	}

	/**
	 * Serialize the current representation of the automaton.
	 */
	public void serialize(OutputStream os) throws IOException {
		/*
		 * Emit the header.
		 */
		os.write(new byte[] { '\\', 'f', 's', 'a' });
		os.write(/* version */FSA.VERSION_CFSA);
		os.write(/* filler */fsa.filler);
		os.write(/* annot */fsa.annotationSeparator);
		os.write(/* ctl/gtl */(fsa.nodeDataLength << 4) | fsa.gotoLength);

		/*
		 * Emit label mapping for arc.
		 */
		os.write(compressedLabels);

		/*
		 * Emit arcs and nodes.
		 */
		for (StreamElement n : nodes) {
			n.serialize(os);
		}
	}
}
