package morfologik.fsa;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;


/**
 * Serializes in-memory {@link State} graphs to a binary format compatible
 * with Jan Daciuk's <code>fsa</code>'s package <code>FSA5</code> format.
 * 
 * <p>It is possible to serialize the automaton with numbers required for 
 * perfect hashing. See {@link #withNumbers()} method.</p>
 * 
 * @see FSA5
 * @see FSA#read(java.io.InputStream)
 */
public final class FSA5Serializer {
	/**
	 * Default filler.
	 */
	public final static byte DEFAULT_FILLER = '_';
	
	/**
	 * Default annotation separator.
	 */
	public final static byte DEFAULT_ANNOTATION = '+';
	
	/**
	 * Maximum number of bytes for a serialized arc. 
	 */
	private final static int MAX_ARC_SIZE = 1 + 5;
	
	/**
	 * Maximum number of bytes for per-node data.
	 */
	private final static int MAX_NODE_DATA_SIZE = 16;

	/**
	 * Number of bytes for the arc's flags header (arc representation
	 * without the goto address).
	 */
	private final static int SIZEOF_FLAGS = 1;

	/**
	 * @see FSA5#filler
	 */
	public byte fillerByte = DEFAULT_FILLER;

	/**
	 * @see FSA5#annotation
	 */
	public byte annotationByte = DEFAULT_ANNOTATION;

	/**
	 * <code>true</code> if we should serialize with numbers.
	 * 
	 * @see #withNumbers()
	 */
	private boolean withNumbers;

	/**
	 * Serialize the automaton with the number of right-language sequences in
	 * each node. This is required to implement perfect hashing. The numbering
	 * also preserves the order of input sequences.
	 * 
	 * @return Returns the same object for easier call chaining.
	 */
	public FSA5Serializer withNumbers() {
		withNumbers = true;
	    return this;
    }

	/**
	 * Serialize root state <code>s</code> to an output stream in <code>FSA5</code> 
	 * format. The serialization process is not thread-safe on the set of 
	 * the same {@link State}s (mutates {@link State} internals).
	 * 
	 * @see #withNumbers
	 * 
	 * @return Returns <code>os</code> for chaining.
	 */
	public <T extends OutputStream> T serialize(State s, T os)
	        throws IOException {

		final ArrayList<State> linearized = new ArrayList<State>();
		final State.InterningPool pool = new State.InterningPool();

		// Add the "sink node", with a single arc pointing to itself.
		State sink = pool.createState();
		sink.addArc((byte) 0, sink, false);

		linearized.add(sink); // Sink is not part of the automaton.

		// Add a special, initial "meta state".
		State meta = pool.createState();
        meta.addArc((byte) '^', s, false);
		s = meta.intern(pool);

		// Prepare space for arc offsets and linearize all the states.
		s.preOrder(new Visitor<State>() {
			public void accept(State s) {
				s.offset = 0;
				linearized.add(s);
			}
		});

		/*
		 * Calculate the number of bytes required for the node data,
		 * if serializing with numbers.
		 */
		int nodeDataLength = 0;
		if (withNumbers) {
			int maxNumber = s.number;
			while (maxNumber > 0) {
				nodeDataLength++;
				maxNumber >>>= 8;
			}
		}

		// Calculate minimal goto length.
		int gtl = 1;
		while (true) {
			// First pass: calculate offsets of states.
			if (!emitArcs(null, linearized, gtl, nodeDataLength)) {
				gtl++;
				continue;
			}

			// Second pass: check if goto overflows anywhere.
			if (emitArcs(null, linearized, gtl, nodeDataLength))
				break;

			gtl++;
		}

		/*
		 * Emit the header.
		 */
		os.write(new byte[] { '\\', 'f', 's', 'a' });
		os.write(FSA5.VERSION);
		os.write(fillerByte);
		os.write(annotationByte);
		os.write((nodeDataLength << 4) | gtl);

		/*
		 * Emit the automaton.
		 */
		boolean gtlUnchanged = emitArcs(os, linearized, gtl, nodeDataLength);
		assert gtlUnchanged : "gtl changed in the final pass.";

		return os;
	}

	/**
	 * Update arc offsets assuming the given goto length.
	 */
	private boolean emitArcs(OutputStream os, 
							 ArrayList<State> linearized,
							 int gtl, 
							 int nodeDataLength)
        throws IOException
    {
		final ByteBuffer bb = ByteBuffer.allocate(
				Math.max(MAX_NODE_DATA_SIZE, MAX_ARC_SIZE));

		int offset = 0;
		int maxStates = linearized.size();
		for (int j = 0; j < maxStates; j++) {
			final State s = linearized.get(j);

			if (os == null) {
				s.offset = offset;
			} else {
				assert s.offset == offset;
			}

			final int lastTransition = s.getArcs() - 1;

			offset += nodeDataLength;
			if (nodeDataLength > 0 && os != null) {
				int number = s.number;
				for (int i = 0; i < nodeDataLength; i++) {
					bb.put((byte) number);
					number >>>= 8;
				}

				bb.flip();
				os.write(bb.array(), bb.position(), bb.remaining());
				bb.clear();
			}

			for (int i = 0; i <= lastTransition; i++) {
				final State target = s.arcState(i);

				int targetOffset;
				if (isTerminal(target)) {
					targetOffset = target.offset;
				} else {
					targetOffset = 0;
				}

				int combined = 0;
				int arcBytes = gtl;

				if (s.arcFinal(i)) {
					combined |= FSA5.BIT_FINAL_ARC;
				}

				if (i == lastTransition) {
					combined |= FSA5.BIT_LAST_ARC;

					if (j + 1 < maxStates && 
							target == linearized.get(j + 1) && 
							targetOffset != 0) {
						combined |= FSA5.BIT_TARGET_NEXT;
						arcBytes = SIZEOF_FLAGS;
						targetOffset = 0;
					}
				}

				combined |= (targetOffset << 3);
				bb.put(s.arcLabel(i));
				for (int b = 0; b < arcBytes; b++) {
					bb.put((byte) combined);
					combined >>>= 8;
				}

				if (combined != 0) {
					// gtl too small. interrupt eagerly.
					return false;
				}

				bb.flip();
				offset += bb.remaining();
				if (os != null) {
					os.write(bb.array(), bb.position(), bb.remaining());
				}
				bb.clear();
			}
		}

		return true;
	}

	/**
	 * A terminal state does not have any outgoing transitions.
	 */
	private static boolean isTerminal(State state) {
		return state.hasChildren();
	}
}
