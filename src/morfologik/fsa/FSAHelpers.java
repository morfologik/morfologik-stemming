package morfologik.fsa;

import static morfologik.fsa.FSAFlags.*;

/**
 * This class has several static utility methods for use 
 * with the FSA package.
 */
public final class FSAHelpers
{
    /** Prevent instantiation. */
    private FSAHelpers() {/* empty */}

    /**
     * Converts an integer with FSA flags to a human-readable string.
     */
    public static String flagsToString(int flags) {
        final StringBuilder res = new StringBuilder();

        for (FSAFlags flag : FSAFlags.values()) {
            if ((flags & flag.bits) != 0) {
                flags = flags & ~flag.bits;
                if (res.length() > 0) res.append(',');
                res.append(flag.toString());
            }
        }

        if (flags != 0) {
            if (res.length() > 0) {
                res.append(' ');
            }
            res.append("(Some flags were not recognized: " 
        	    + Integer.toBinaryString(flags) + ")");
        }

        return res.toString();
    }


    /**
     * Returns a version number for a set of flags.
     */
    public static byte getVersion(int flags) {
        final byte version;

        if (FSAFlags.isSet(flags, FLEXIBLE)) {
            if (FSAFlags.isSet(flags, STOPBIT)) {
                if (FSAFlags.isSet(flags, NEXTBIT)) {
                    if (FSAFlags.isSet(flags, TAILS)) {
                        version = 7;
                    } else {
                        if (FSAFlags.isSet(flags, WEIGHTED))
                            version = 8;
                        else
                            version = 5;
                    }
                } else {
                    if (FSAFlags.isSet(flags, TAILS))
                        version = 6;
                    else
                        version = 4;
                }
            } else {
                if (FSAFlags.isSet(flags, NEXTBIT))
                    version = 2;
                else
                    version = 1;
            }
        } else {
            if (FSAFlags.isSet(flags, LARGE_DICTIONARIES))
                version = (byte) 0x80;
            else
                version = 0;
        }

        return version;
    }


    /**
     * Returns flags as an integer for a given version number.
     * 
     * @throws RuntimeException if the version number is not recognized.
     */
    public static int getFlags(int version) {
        final int flags;
        switch (version) {
            case 0:     
                flags = 0; break;
            case (byte) 0x80:  
                flags = LARGE_DICTIONARIES.bits; break;
            case 1:     
                flags = FLEXIBLE.bits; break;
            case 2:     
                flags = FLEXIBLE.bits |                NEXTBIT.bits; break;
            case 4:     
                flags = FLEXIBLE.bits | STOPBIT.bits; break;
            case FSA.VERSION_5:
                flags = FLEXIBLE.bits | STOPBIT.bits | NEXTBIT.bits; break;
            case 6:     
                flags = FLEXIBLE.bits | STOPBIT.bits |                TAILS.bits; break;
            case 7:     
                flags = FLEXIBLE.bits | STOPBIT.bits | NEXTBIT.bits | TAILS.bits; break;
            default:
                throw new RuntimeException("Unknown version number. FSA created with unknown options.");
        }

        return flags;
    }
}