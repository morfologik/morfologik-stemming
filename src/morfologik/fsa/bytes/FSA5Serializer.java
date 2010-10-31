package morfologik.fsa.bytes;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;

import morfologik.fsa.FSA;
import morfologik.fsa.FSA5;
import morfologik.fsa.Visitor;

/**
 * Serializes in-memory {@link State} graphs to a binary format in compatible
 * with Jan Daciuk's <code>fsa</code>'s package <code>FSA5</code> format.
 * 
 * @see FSA5
 * @see FSA#getInstance(java.io.InputStream)
 */
public final class FSA5Serializer {
	/**
	 * Maximum number of bytes for a serialized arc. 
	 */
	private final static int MAX_ARC_SIZE = 1 + 5;
	
	/**
	 * Number of bytes for the arc's flags header (arc representation
	 * without the goto address).
	 */
	private final static int SIZEOF_FLAGS = 1;

	/**
	 * @see FSA5#filler
	 */
	public byte fillerByte = '_';

	/**
	 * @see FSA5#annotation
	 */
	public byte annotationByte = '+';

	/**
	 * Node data length. Must be zero (node data not supported).
	 */
	private final int nodeDataLength = 0;

	/**
	 * Mutable integer for offset calculation.
	 */
	final static class IntHolder {
		public int value;
	}

	/**
	 * Serialize root state <code>s</code> to an output stream.
	 * 
	 * @return Returns <code>os</code> for chaining.
	 */
	public <T extends OutputStream> T serialize(State s, T os)
	        throws IOException {

		final IdentityHashMap<State, IntHolder> offsets = new IdentityHashMap<State, IntHolder>();
		final ArrayList<State> linearized = new ArrayList<State>();

		// Add the "sink node", with a single arc pointing to itself.
		State sink = new State();
		sink.labels = new byte[] { 0 };
		sink.states = new State[] { sink };
		sink.final_transitions = new boolean [] {false};

		linearized.add(sink); // Sink is not part of the automaton.
		offsets.put(sink, new IntHolder());

		// Add a special, initial "meta state".
		State meta = new State();
		meta.labels = new byte[] { '^' };
		meta.states = new State[] { s };
		meta.final_transitions = new boolean [] {false};
		s = meta;

		// Prepare space for arc offsets and linearize all the states.
		s.preOrder(new Visitor<State>() {
			public void accept(State s) {
				offsets.put(s, new IntHolder());
				linearized.add(s);
			}
		});

		// Calculate minimal goto length.
		int gtl = 1;
		while (true) {
			// First pass: calculate offsets of states.
			if (!emitArcs(null, linearized, gtl, offsets)) {
				gtl++;
				continue;
			}

			// Second pass: check if goto overflows anywhere.
			if (emitArcs(null, linearized, gtl, offsets))
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
		boolean gtlUnchanged = emitArcs(os, linearized, gtl, offsets);
		assert gtlUnchanged : "gtl changed in the final pass.";

		return os;
	}

	/**
	 * Update arc offsets assuming the given goto length.
	 */
	private boolean emitArcs(OutputStream os, ArrayList<State> linearized,
	        final int gtl, final IdentityHashMap<State, IntHolder> offsets)
	        throws IOException {
		final ByteBuffer bb = ByteBuffer.allocate(MAX_ARC_SIZE);

		int offset = 0;
		int maxStates = linearized.size();
		for (int j = 0; j < maxStates; j++) {
			final State s = linearized.get(j);

			if (os == null) {
				offsets.get(s).value = offset;
			} else {
				assert offsets.get(s).value == offset;
			}

			final byte[] labels = s.labels;
			final State[] states = s.states;
			final boolean[] final_transitions = s.final_transitions;

			final int maxTransition = labels.length - 1;
			final int lastTransition = 0;
			for (int i = maxTransition; i >= 0; i--) {
				final State target = states[i];

				int targetOffset;
				if (isTerminal(target)) {
					targetOffset = offsets.get(target).value;
				} else {
					targetOffset = 0;
				}

				int combined = 0;
				int arcBytes = gtl;

				if (final_transitions[i]) {
					combined |= FSA5.BIT_FINAL_ARC;
				}

				if (i == lastTransition) {
					combined |= FSA5.BIT_LAST_ARC;

					if (j + 1 < maxStates && target == linearized.get(j + 1)
					        && targetOffset != 0) {
						combined |= FSA5.BIT_TARGET_NEXT;
						arcBytes = SIZEOF_FLAGS;
						targetOffset = 0;
					}
				}

				combined |= (targetOffset << 3);
				bb.put(labels[i]);
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
