package morfologik.fsa;

import morfologik.util.Arrays;

/**
 * A pool of arrays for sharing arcs, labels and transition bit data.  
 */
final class StateInterningPool {
    /**
     * Default increment size for internal array blocks: one million arcs.
     */
    private final static int DEFAULT_BLOCK_SIZE = 1024 * 1024;

    /**
     * Minimum interning block size (max. number of outgoing arcs).
     */
    public static final int MINIMUM_BLOCK_SIZE = 256; 

    /**
     * Maximum number of arcs stored in one interned block.
     */
    private final int blockSize;

    /**
     * The next available index in {@link #labels},
     * {@link #states} and {@link #final_transitions}. 
     */
    private int last = 0;

    /** Label array, shared by all interned states. */
    private byte[] labels = new byte [0];
    
    /** Target state array, shared by all interned states. */
    private State[] states = new State [0];
    
    /** Final arc flags array, shared by all interned states. */
    private boolean[] final_transitions = new boolean [0];

    /**
     * A pool of mutable states, reusable once a state is interned.
     */
    private State [] statePool = new State [0];
    
    /**
     * Number of available states in {@link #statePool}.
     */
    private int lastState;

    /**
     * Create a state interning pool with the given internal block size for parallel
     * arc arrays. 
     */
    StateInterningPool() {
        this(DEFAULT_BLOCK_SIZE);
    }

    /**
     * Create a state interning pool with the given internal block size for parallel
     * arc arrays. 
     */
    StateInterningPool(int blockSize) {
        assert blockSize >= MINIMUM_BLOCK_SIZE : "Block size must be at least 256 arcs (maximum for a single state).";

        this.blockSize = blockSize;
    }

    /**
     * Intern a mutable state, return it to the pool, return an interned copy. 
     */
    State intern(State state) {
        final int arcs = state.arcs;

        if (last + arcs > labels.length) {
            /*
             * We could reallocate internal arrays to store all data in a contiguous array, but
             * we simply drop the previous block and allocate a new one (allowing some wasted space
             * at the end of the last block) to avoid copying large arrays.
             */
            labels = new byte [blockSize];
            states = new State [blockSize];
            final_transitions = new boolean [blockSize];
            last = 0;
        }

        // Copy over this state's data.
        System.arraycopy(state.labels, 0, labels, last, arcs);
        System.arraycopy(state.states, 0, states, last, arcs);
        System.arraycopy(state.final_transitions, 0, final_transitions, last, arcs);

        final State interned = new State();
        interned.arcs = state.arcs;
        interned.number = state.number;
        interned.start = last;
        interned.final_transitions = final_transitions;
        interned.labels = labels;
        interned.states = states;
        returnState(state);

        last += arcs;
        return interned;
    }

    /**
     * Return the given mutable state to the pool, without interning.
     */
    void returnState(State state) {
        state.reset();

        if (statePool.length == lastState) {
            statePool = Arrays.copyOf(statePool, lastState + 1);
        }
        statePool[lastState++] = state;
    }

    /**
     * Get a mutable state from the pool or create a new one.
     */
    State createState() {
        if (lastState == 0) {
            State s = new State();
            s.labels = new byte [256];
            s.final_transitions = new boolean [256];
            s.states = new State [256];
            return s;
        } else {
            return statePool[--lastState];
        }
    }
}