package morfologik.fsa;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;


/**
 * Serializes in-memory {@link State} graphs to a binary format {@link CFSA}
 * (compacted most frequent arcs with {@link FSA5#BIT_TARGET_NEXT} bit set).
 * 
 * <p>It is possible to serialize the automaton with numbers required for 
 * perfect hashing. See {@link #withNumbers()} method.</p>
 * 
 * @see CFSA
 * @see FSA#read(java.io.InputStream)
 */
public final class CFSASerializer implements FSASerializer {
	/**
	 * Maximum number of bytes for a serialized arc. 
	 */
	private final static int MAX_ARC_SIZE = 1 + 5;
	
	/**
	 * Maximum number of bytes for per-node data.
	 */
	private final static int MAX_NODE_DATA_SIZE = 16;

	/**
	 * @see FSA5#filler
	 */
	public byte fillerByte = FSASerializer.DEFAULT_FILLER;

	/**
	 * @see FSA5#annotation
	 */
	public byte annotationByte = FSASerializer.DEFAULT_ANNOTATION;

	/**
	 * <code>true</code> if we should serialize with numbers.
	 * 
	 * @see #withNumbers()
	 */
	private boolean withNumbers;

    /**
     * A mapping of compressed labels.
     */
    private final byte[] compressedLabels = new byte[1 << 5];

    /**
     * Indicates which byte values should be placed together with flags.
     */
    private final byte[] compressedLabelsIndex = new byte[256];

	/**
	 * Serialize the automaton with the number of right-language sequences in
	 * each node. This is required to implement perfect hashing. The numbering
	 * also preserves the order of input sequences.
	 * 
	 * @return Returns the same object for easier call chaining.
	 */
	public CFSASerializer withNumbers() {
		withNumbers = true;
	    return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CFSASerializer withFiller(byte filler) {
        this.fillerByte = filler;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CFSASerializer withAnnotationSeparator(byte annotationSeparator) {
        this.annotationByte = annotationSeparator;
        return this;
    }

	/**
	 * Serialize root state <code>s</code> to an output stream in {@link CFSA} 
	 * format. The serialization process is not thread-safe on the set of 
	 * the same {@link State}s (mutates {@link State} internals).
	 * 
	 * @see #withNumbers
	 * 
	 * @return Returns <code>os</code> for chaining.
	 */
	public <T extends OutputStream> T serialize(State s, T os) throws IOException
	{
	    Arrays.fill(compressedLabelsIndex, (byte) -1);
	    Arrays.fill(compressedLabels, (byte) 0);

		final ArrayList<State> linearized = new ArrayList<State>();
		final StateInterningPool pool = new StateInterningPool(
		        StateInterningPool.MINIMUM_BLOCK_SIZE);

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

		// Calculate most frequent labels on arcs with NEXT bit set.
		doLabelMapping(s, linearized);

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
		os.write(CFSA.VERSION);
		os.write(fillerByte);
		os.write(annotationByte);
		os.write((nodeDataLength << 4) | gtl);

		/*
         * Emit label mapping for arc.
         */
        os.write(compressedLabels);

		/*
		 * Emit the automaton.
		 */
		boolean gtlUnchanged = emitArcs(os, linearized, gtl, nodeDataLength);
		assert gtlUnchanged : "gtl changed in the final pass.";

		return os;
	}

	/**
	 * Calculate most frequent labels on arcs with NEXT bit set.
	 */
	private void doLabelMapping(State root, ArrayList<State> linearized) {
        class IndexValue {
            int index;
            int value;

            public IndexValue(int index) {
                this.index = index;
            }
        }

        final IndexValue[] counts = new IndexValue[256];
        for (int i = 0; i < counts.length; i++)
            counts[i] = new IndexValue(i);

        // Count the distribution of labels on arcs with NEXT bit set.
        int maxStates = linearized.size() - 1;
        for (int j = 0; j < maxStates; j++) {
            final State state = linearized.get(j);

            if (state.hasChildren()) {
                final int arcIndex = state.arcsCount() - 1;
                if (state.arcState(arcIndex) == linearized.get(j + 1)) {
                    counts[state.arcLabel(arcIndex) & 0xff].value++;
                }
            }
        }

        // Pick the most frequent labels for mapping.
        Arrays.sort(counts, new Comparator<IndexValue>() {
            public int compare(IndexValue o1, IndexValue o2) {
                return o2.value - o1.value;
            };
        });

        // Take the 31 most frequent values.
        for (int i = 0; i < (1 << 5) - 1; i++) {
            compressedLabelsIndex[counts[i].index] = (byte) (i + 1);
            if (counts[i].value > 0)
                compressedLabels[i + 1] = (byte) counts[i].index;
            else
                compressedLabels[i + 1] = -1;
        }
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

			final int lastTransition = s.arcsCount() - 1;

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
                    targetOffset = 0;
				} else {
                    targetOffset = target.offset;
				}

				int combined = 0;

				if (s.arcFinal(i)) {
					combined |= FSA5.BIT_FINAL_ARC;
				}

				if (i == lastTransition) {
					combined |= FSA5.BIT_LAST_ARC;

					if (j + 1 < maxStates && 
							target == linearized.get(j + 1) && 
							targetOffset != 0) {
						combined |= FSA5.BIT_TARGET_NEXT;
						targetOffset = 0;
					}
				}

				final byte label = s.arcLabel(i);

				if ((combined & FSA5.BIT_TARGET_NEXT) != 0) {
	                byte index = compressedLabelsIndex[label & 0xff];
	                if (index > 0) {
	                    // arc version 1
	                    bb.put((byte) ((index << 3) | combined));
	                } else {
	                    // arc version 2
	                    bb.put((byte) combined);
	                    bb.put(label);
	                }
				} else {
	                // flags, label, goto field
				    combined |= (targetOffset << 3);
	                bb.put((byte) (combined & 0xff));
	                bb.put(label);

                    combined >>>= 8;
	                for (int b = 1; b < gtl; b++) {
	                    bb.put((byte) combined);
                        combined >>>= 8;
	                }
	                
	                if (combined != 0) {
	                    // gtl too small. interrupt eagerly.
	                    return false;
	                }
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
		return !state.hasChildren();
	}
}
