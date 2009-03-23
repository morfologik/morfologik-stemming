package morfologik.fsa;

/**
 * FSA automaton flags.
 */
public enum FSAFlags {
    FLEXIBLE(1 << 0),
    STOPBIT(1 << 1),
    NEXTBIT(1 << 2),
    TAILS(1 << 3),
    WEIGHTED(1 << 4),
    LARGE_DICTIONARIES(1 << 5);
    
    /**
     * Bit mask for the corresponding flag.
     */
    public final int bits;

    /*
     * 
     */
    private FSAFlags(int bits) {
        this.bits = bits;
    }

    /**
     * Returns <code>true</code> if the corresponding flag is set in the
     * bit set.
     */
    public static boolean isSet(int flags, FSAFlags flag) {
	return (flags & flag.bits) != 0;
    }
}