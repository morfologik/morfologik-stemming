package morfologik.fsa;

import static morfologik.fsa.CFSA2.BIT_FINAL_ARC;
import static morfologik.fsa.CFSA2.BIT_LAST_ARC;
import static morfologik.fsa.CFSA2.BIT_TARGET_NEXT;
import static morfologik.fsa.FSAFlags.FLEXIBLE;
import static morfologik.fsa.FSAFlags.NEXTBIT;
import static morfologik.fsa.FSAFlags.NUMBERS;
import static morfologik.fsa.FSAFlags.STOPBIT;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.PriorityQueue;
import java.util.Set;

import morfologik.fsa.FSAUtils.IntIntHolder;
import morfologik.tools.IMessageLogger;
import morfologik.util.FileUtils;

import com.carrotsearch.hppc.BitSet;
import com.carrotsearch.hppc.BoundedProportionalArraySizingStrategy;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.IntStack;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.IntIntCursor;

/**
 * Serializes in-memory {@link FSA} graphs to {@link CFSA2}.
 * 
 * <p>
 * It is possible to serialize the automaton with numbers required for perfect
 * hashing. See {@link #withNumbers()} method.
 * </p>
 * 
 * @see CFSA2
 * @see FSA#read(java.io.InputStream)
 */
public final class CFSA2Serializer implements FSASerializer {
    /**
     * Supported flags.
     */
    private final static EnumSet<FSAFlags> flags = EnumSet.of(NUMBERS, FLEXIBLE, STOPBIT, NEXTBIT);

    /**
     * No-state id.
     */
    private final static int NO_STATE = -1;
    
    /**
     * <code>true</code> if we should serialize with numbers.
     * 
     * @see #withNumbers()
     */
    private boolean withNumbers;

    /**
     * A hash map of [state, offset] pairs.
     */
    private IntIntOpenHashMap offsets = new IntIntOpenHashMap();

    /**
     * A hash map of [state, right-language-count] pairs.
     */
    private IntIntOpenHashMap numbers = new IntIntOpenHashMap();

    /**
     * Scratch array for serializing vints.
     */
    private final byte [] scratch = new byte [5];

    /**
     * The most frequent labels for integrating with the flags field.
     */
    private byte [] labelsIndex;
    
    /**
     * Inverted index of labels to be integrated with flags field. A label
     * at index <code>i<code> has the index or zero (no integration). 
     */
    private int [] labelsInvIndex;

    /**
     * Logger for progress.
     */
    private IMessageLogger logger = new NullMessageLogger(); 

    /**
     * Serialize the automaton with the number of right-language sequences in
     * each node. This is required to implement perfect hashing. The numbering
     * also preserves the order of input sequences.
     * 
     * @return Returns the same object for easier call chaining.
     */
    public CFSA2Serializer withNumbers() {
        withNumbers = true;
        return this;
    }

    /**
     * Serializes any {@link FSA} to {@link CFSA2} stream.
     * 
     * @see #withNumbers
     * @return Returns <code>os</code> for chaining.
     */
    @Override
    public <T extends OutputStream> T serialize(final FSA fsa, T os) throws IOException {
        /*
         * Calculate the most frequent labels and build indexed labels dictionary.
         */
        computeLabelsIndex(fsa);

        /*
         * Calculate the number of bytes required for the node data, if
         * serializing with numbers.
         */
        if (withNumbers) {
            this.numbers = FSAUtils.rightLanguageForAllStates(fsa);
        }

        /*
         * Linearize all the states, optimizing their layout.
         */
        IntArrayList linearized = linearize(fsa);

        /*
         * Emit the header.
         */
        FileUtils.writeInt(os, FSAHeader.FSA_MAGIC);
        os.write(CFSA2.VERSION);

        EnumSet<FSAFlags> fsaFlags = EnumSet.of(FLEXIBLE, STOPBIT, NEXTBIT);
        if (withNumbers) fsaFlags.add(NUMBERS);
        FileUtils.writeShort(os, FSAFlags.asShort(fsaFlags));

        /*
         * Emit labels index.
         */
        os.write(labelsIndex.length);
        os.write(labelsIndex);

        /*
         * Emit the automaton.
         */
        int size = emitNodes(fsa, os, linearized);
        assert size == 0 : "Size changed in the final pass?";

        return os;
    }

    /**
     * Compute a set of labels to be integrated with the flags field. 
     */
    private void computeLabelsIndex(final FSA fsa) {
        // Compute labels count.
        final int [] countByValue = new int [256];

        fsa.visitAllStates(new StateVisitor() {
            public boolean accept(int state) {
                for (int arc = fsa.getFirstArc(state); arc != 0; arc = fsa.getNextArc(arc))
                    countByValue[fsa.getArcLabel(arc) & 0xff]++;
                return true;
            }
        });

        // Order by descending frequency of counts and increasing label value.
        Comparator<IntIntHolder> comparator = new Comparator<IntIntHolder>() {
            public int compare(IntIntHolder o1, IntIntHolder o2) {
                int countDiff = o2.b - o1.b;
                if (countDiff == 0) {
                    countDiff = o1.a - o2.a;
                }
                return countDiff;
            }
        };

        PriorityQueue<IntIntHolder> labelAndCount = new PriorityQueue<IntIntHolder>(countByValue.length, comparator);
        for (int label = 0; label < countByValue.length; label++) {
            if (countByValue[label] > 0) {
                labelAndCount.add(new IntIntHolder(label, countByValue[label]));
            }
        }

        labelsIndex = new byte [1 + Math.min(labelAndCount.size(), CFSA2.LABEL_INDEX_SIZE)];
        labelsInvIndex = new int [256];
        for (int i = labelsIndex.length - 1; i > 0 && !labelAndCount.isEmpty(); i--) {
            IntIntHolder p = labelAndCount.remove();
            labelsInvIndex[p.a] = i;
            labelsIndex[i] = (byte) p.a;
        }
    }

    /**
     * Return supported flags.
     */
    @Override
    public Set<FSAFlags> getFlags() {
        return flags;
    }

    /**
     * Linearization of states.
     */
    private IntArrayList linearize(final FSA fsa) throws IOException {
        /*
         * Compute the states with most inlinks. These should be placed as close to the 
         * start of the automaton, as possible so that v-coded addresses are tiny.  
         */
        final IntIntOpenHashMap inlinkCount = computeInlinkCount(fsa);

        /*
         * An array of ordered states for serialization.
         */
        final IntArrayList linearized = new IntArrayList(0, 
                new BoundedProportionalArraySizingStrategy(1000, 10000, 1.5f));

        /*
         * Determine which states should be linearized first (at fixed positions) so as to
         * minimize the place occupied by goto fields.
         */
        int maxStates = Integer.MAX_VALUE;
        int minInlinkCount = 2;
        ArrayDeque<Integer> statesQueue = computeFirstStates(inlinkCount, maxStates, minInlinkCount);
        IntArrayList states = new IntArrayList();
        while (!statesQueue.isEmpty())
            states.add(statesQueue.pop());

        /*
         * Compute initial addresses, without node rearrangements.
         */
        int serializedSize = linearizeAndCalculateOffsets(fsa, new IntArrayList(), linearized, offsets);

        /*
         * Probe for better node arrangements by selecting between [lower, upper]
         * nodes from the potential candidate nodes list. 
         */
        IntArrayList sublist = new IntArrayList();
        sublist.buffer = states.buffer;
        sublist.elementsCount = states.elementsCount;

        /*
         * Probe the initial region a little bit, looking for optimal cut. It can't be binary search
         * because the result isn't monotonic.
         */
        logger.startPart("Compacting");
        logger.log("Initial output size", serializedSize);
        int cutAt = 0;
        for (int cut = Math.min(25, states.size()); cut <= Math.min(150, states.size()); cut += 25) {
            sublist.elementsCount = cut;
            int newSize = linearizeAndCalculateOffsets(fsa, sublist, linearized, offsets);
            logger.log("Moved " + sublist.size() + " states, output size", newSize);
            if (newSize >= serializedSize) {
                break;
            }
            cutAt = cut;
        }

        /*
         * Cut at the calculated point and repeat linearization.
         */
        sublist.elementsCount = cutAt;
        int size = linearizeAndCalculateOffsets(fsa, sublist, linearized, offsets);

        logger.log("Will move " + sublist.size() + " states, final size", size);
        logger.endPart();

        return linearized;
    }

    /**
     * Linearize all states, putting <code>states</code> in front of the automaton and
     * calculating stable state offsets.
     */
    private int linearizeAndCalculateOffsets(FSA fsa, IntArrayList states,
            IntArrayList linearized, IntIntOpenHashMap offsets) throws IOException
    {
        final BitSet visited = new BitSet();
        final IntStack nodes = new IntStack();
        linearized.clear();

        /*
         * Linearize states with most inlinks first.
         */
        for (int i = 0; i < states.size(); i++) {
            linearizeState(fsa, nodes, linearized, visited, states.get(i));
        }

        /*
         * Linearize the remaining states by chaining them one after another, in depth-order.
         */
        nodes.push(fsa.getRootNode());
        while (!nodes.isEmpty()) {
            final int node = nodes.pop();
            if (visited.get(node))
                continue;

            linearizeState(fsa, nodes, linearized, visited, node);
        }

        /*
         * Calculate new state offsets. This is iterative. We start with 
         * maximum potential offsets and recalculate until converged.
         */
        int MAX_OFFSET = Integer.MAX_VALUE;
        for (IntCursor c : linearized) {
            offsets.put(c.value, MAX_OFFSET);
        }

        int i, j = 0;
        while ((i = emitNodes(fsa, null, linearized)) > 0) {
            j = i;
        }
        return j;
    }

    /**
     * Add a state to linearized list.
     */
    private void linearizeState(final FSA fsa, 
            IntStack nodes, 
            IntArrayList linearized,
            BitSet visited, int node)
    {
        linearized.add(node);
        visited.set(node);
        for (int arc = fsa.getFirstArc(node); arc != 0; arc = fsa.getNextArc(arc)) {
            if (!fsa.isArcTerminal(arc)) {
                final int target = fsa.getEndNode(arc);
                if (!visited.get(target))
                    nodes.push(target);
            }
        }
    }

    /**
     * Compute the set of states that should be linearized first to minimize other
     * states goto length.
     */
    private ArrayDeque<Integer> computeFirstStates(IntIntOpenHashMap inlinkCount, 
            int maxStates,
            int minInlinkCount) 
    {
        Comparator<IntIntHolder> comparator = new Comparator<FSAUtils.IntIntHolder>() {
            public int compare(IntIntHolder o1, IntIntHolder o2) {
                int v = o1.a - o2.a;
                return v == 0 ? (o1.b - o2.b) : v;
            }
        };

        PriorityQueue<IntIntHolder> stateInlink = new PriorityQueue<IntIntHolder>(1, comparator);
        IntIntHolder scratch = new IntIntHolder();
        for (IntIntCursor c : inlinkCount) {
            if (c.value > minInlinkCount) {
                scratch.a = c.value;
                scratch.b = c.key;
               
                if (stateInlink.size() < maxStates || comparator.compare(scratch, stateInlink.peek()) > 0) {
                    stateInlink.add(new IntIntHolder(c.value, c.key));
                    if (stateInlink.size() > maxStates) stateInlink.remove();
                }
            }
        }

        ArrayDeque<Integer> states = new ArrayDeque<Integer>();
        while (!stateInlink.isEmpty()) {
            IntIntHolder i = stateInlink.remove();
            states.addFirst(i.b);
        }
        return states;
    }

    /**
     * Compute in-link count for each state.
     */
    private IntIntOpenHashMap computeInlinkCount(final FSA fsa) {
        IntIntOpenHashMap inlinkCount = new IntIntOpenHashMap();
        BitSet visited = new BitSet();
        IntStack nodes = new IntStack();
        nodes.push(fsa.getRootNode());
        
        while (!nodes.isEmpty()) {
            final int node = nodes.pop();
            if (visited.get(node))
                continue;

            visited.set(node);

            for (int arc = fsa.getFirstArc(node); arc != 0; arc = fsa.getNextArc(arc)) {
                if (!fsa.isArcTerminal(arc)) {
                    final int target = fsa.getEndNode(arc);
                    inlinkCount.putOrAdd(target, 1, 1);
                    if (!visited.get(target))
                        nodes.push(target);
                }
            }
        }

        return inlinkCount;
    }

    /**
     * Update arc offsets assuming the given goto length.
     */
    private int emitNodes(FSA fsa, OutputStream os, IntArrayList linearized) throws IOException {
        int offset = 0;

        // Add epsilon state.
        offset += emitNodeData(os, 0);
        if (fsa.getRootNode() != 0)
            offset += emitArc(os, BIT_LAST_ARC, (byte) '^', offsets.get(fsa.getRootNode()));
        else
            offset += emitArc(os, BIT_LAST_ARC, (byte) '^', 0);

        boolean offsetsChanged = false;
        final int max = linearized.size();
        for (IntCursor c : linearized) {
            final int state = c.value;
            final int nextState = c.index + 1 < max ? linearized.get(c.index + 1) : NO_STATE;

            if (os == null) {
                offsetsChanged |= (offsets.get(state) != offset);
                offsets.put(state, offset);
            } else {
                assert offsets.get(state) == offset : state + " " + offsets.get(state) + " " + offset;
            }

            offset += emitNodeData(os, withNumbers ? numbers.get(state) : 0);
            offset += emitNodeArcs(fsa, os, state, nextState);
        }

        return offsetsChanged ? offset : 0;
    }

    /**
     * Emit all arcs of a single node.
     */
    private int emitNodeArcs(FSA fsa, OutputStream os, 
                          final int state, final int nextState) throws IOException {
        int offset = 0;
        for (int arc = fsa.getFirstArc(state); arc != 0; arc = fsa.getNextArc(arc)) {
            int targetOffset;
            final int target;

            if (fsa.isArcTerminal(arc)) {
                target = 0;
                targetOffset = 0;
            } else {
                target = fsa.getEndNode(arc);
                targetOffset = offsets.get(target);
            }

            int flags = 0;

            if (fsa.isArcFinal(arc)) {
                flags |= BIT_FINAL_ARC;
            }

            if (fsa.getNextArc(arc) == 0) {
                flags |= BIT_LAST_ARC;
            }

            if (targetOffset != 0 && target == nextState) {
                flags |= BIT_TARGET_NEXT;
                targetOffset = 0;
            }

            offset += emitArc(os, flags, fsa.getArcLabel(arc), targetOffset);
        }

        return offset;
    }

    /** */
    private int emitArc(OutputStream os, int flags, byte label, int targetOffset)
        throws IOException
    {
        int length = 0;

        int labelIndex = labelsInvIndex[label & 0xff];
        if (labelIndex > 0) {
            if (os != null) os.write(flags | labelIndex);
            length++;
        } else {
            if (os != null) {
                os.write(flags);
                os.write(label);
            }
            length += 2;
        }

        if ((flags & BIT_TARGET_NEXT) == 0) {
            int len = CFSA2.writeVInt(scratch, 0, targetOffset);
            if (os != null) {
                os.write(scratch, 0, len);
            }
            length += len;
        }

        return length;
    }

    /** */
    private int emitNodeData(OutputStream os, int number) throws IOException {
        int size = 0;

        if (withNumbers) {
            size = CFSA2.writeVInt(scratch, 0, number);
            if (os != null) {
                os.write(scratch, 0, size);
            }
        }

        return size;
    }

    /** */
    @Override
    public CFSA2Serializer withFiller(byte filler) {
        throw new UnsupportedOperationException("CFSA2 does not support filler. Use .info file.");
    }

    /** */
    @Override
    public CFSA2Serializer withAnnotationSeparator(byte annotationSeparator) {
        throw new UnsupportedOperationException("CFSA2 does not support separator. Use .info file.");
    }

    @Override
    public CFSA2Serializer withLogger(IMessageLogger logger) {
        this.logger = logger;
        return this;
    }
}
