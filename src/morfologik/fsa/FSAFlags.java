package morfologik.fsa;

import java.util.Set;

/**
 * FSA automaton flags. Where applicable, flags follow Daciuk's <code>fsa</code> package.
 */
public enum FSAFlags {
    /** Daciuk: flexible FSA encoding. */
	FLEXIBLE(1 << 0), 

	/** Daciuk: stop bit in use. */
	STOPBIT(1 << 1),

	/** Daciuk: next bit in use. */
	NEXTBIT(1 << 2), 

	/** Daciuk: tails compression. */
	TAILS(1 << 3),

	/*
	 * These flags are outside of byte range (never occur in Daciuk's FSA).
	 */

	/** 
	 * The FSA contains right-language count numbers on states.
	 * 
	 * @see FSA#getRightLanguageCount(int)
	 */
	NUMBERS(1 << 8),

	/**
	 * The FSA supports legacy built-in separator and filler characters (Daciuk's FSA package
	 * compatibility). 
	 */
	SEPARATORS(1 << 9);

	/**
	 * Bit mask for the corresponding flag.
	 */
	public final int bits;

	/** */
	private FSAFlags(int bits) {
		this.bits = bits;
	}

	/**
	 * Returns <code>true</code> if the corresponding flag is set in the bit set.
	 */
	public static boolean isSet(int flags, FSAFlags flag) {
		return (flags & flag.bits) != 0;
	}

	/**
	 * Returns the set of flags encoded in a single short. 
	 */
    public static short asShort(Set<FSAFlags> flags) {
        short value = 0;
        for (FSAFlags f : flags)
            value |= f.bits;
        return value;
    }
}