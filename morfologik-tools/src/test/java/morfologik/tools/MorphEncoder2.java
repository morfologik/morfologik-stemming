package morfologik.tools;

import com.carrotsearch.hppc.ByteArrayList;

public final class MorphEncoder2 {
    private MorphEncoder2() {}

    /**
     * Maximum encodable single-byte code.
     */
    private static final int REMOVE_EVERYTHING = 255;

    public static interface IEncoder {
        public ByteArrayList encode(ByteArrayList src, ByteArrayList derived, ByteArrayList encodedBuffer);
        public ByteArrayList decode(ByteArrayList src, ByteArrayList encoded, ByteArrayList derivedBuffer);
    }

    /**
     * TODO: add javadoc on encoding format.
     */
    public static class TrimSuffixEncoder implements IEncoder {
        public ByteArrayList encode(ByteArrayList src, ByteArrayList dst, ByteArrayList encoded) {
            int sharedPrefix = sharedPrefixLength(src, dst);
            int truncateBytes = src.size() - sharedPrefix;
            if (truncateBytes >= REMOVE_EVERYTHING) {
                truncateBytes = REMOVE_EVERYTHING;
                sharedPrefix = 0;
            }

            final byte suffixTrimCode = (byte) (truncateBytes + 'A');
            encoded.add(suffixTrimCode);
            encoded.add(dst.buffer, sharedPrefix, dst.size() - sharedPrefix);

            return encoded;
        }

        public ByteArrayList decode(ByteArrayList src, ByteArrayList encoded, ByteArrayList dst) {
            int suffixTrimCode = encoded.get(0);
            int truncateBytes = (suffixTrimCode - 'A') & 0xFF;
            if (truncateBytes == REMOVE_EVERYTHING) {
                truncateBytes = src.size();
            }

            dst.add(src.buffer, 0, src.size() - truncateBytes);
            dst.add(encoded.buffer, 1, encoded.size() - 1);

            return dst;
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    /**
     * TODO: add javadoc on encoding format.
     */
    public static class TrimPrefixAndSuffixEncoder implements IEncoder {
        public ByteArrayList encode(ByteArrayList src, ByteArrayList dst, ByteArrayList encoded) {
            // Search for the maximum matching subsequence that can be encoded. 
            int maxSubsequenceLength = 0;
            int maxSubsequenceIndex = 0;
            for (int i = 0; i < src.size(); i++) {
                // prefix at i => shared subsequence (infix)
                int sharedPrefix = sharedPrefixLength(src, i, dst, 0);
                // Only update maxSubsequenceLength if we will be able to encode it.
                if (sharedPrefix > maxSubsequenceLength
                        && i < REMOVE_EVERYTHING
                        && (src.size() - (i + sharedPrefix)) < REMOVE_EVERYTHING) {
                    maxSubsequenceLength = sharedPrefix;
                    maxSubsequenceIndex = i;
                }
            }

            // Determine how much to remove (and where) from src to get a prefix of dst.
            int truncatePrefixBytes = maxSubsequenceIndex;
            int truncateSuffixBytes = (src.size() - (maxSubsequenceIndex + maxSubsequenceLength));
            if (truncatePrefixBytes >= REMOVE_EVERYTHING ||
                truncateSuffixBytes >= REMOVE_EVERYTHING) {
                maxSubsequenceIndex = maxSubsequenceLength = 0;
                truncatePrefixBytes = truncateSuffixBytes = REMOVE_EVERYTHING;
            }

            encoded.add((byte) ((truncatePrefixBytes + 'A') & 0xFF));
            encoded.add((byte) ((truncateSuffixBytes + 'A') & 0xFF));
            encoded.add(dst.buffer, maxSubsequenceLength, dst.size() - maxSubsequenceLength);

            return encoded;
        }

        public ByteArrayList decode(ByteArrayList src, ByteArrayList encoded, ByteArrayList dst) {
            int truncatePrefixBytes = (encoded.get(0) - 'A') & 0xFF;
            int truncateSuffixBytes = (encoded.get(1) - 'A') & 0xFF;

            if (truncatePrefixBytes == REMOVE_EVERYTHING ||
                truncateSuffixBytes == REMOVE_EVERYTHING) {
                truncatePrefixBytes = src.size();
                truncateSuffixBytes = 0;
            }

            dst.add(src.buffer, truncatePrefixBytes, src.size() - (truncateSuffixBytes + truncatePrefixBytes));
            dst.add(encoded.buffer, 2, encoded.size() - 2);

            return dst;
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName();
        }        
    }

    /**
     * TODO: add javadoc on encoding format.
     */
    public static class TrimInfixAndSuffixEncoder implements IEncoder {
        ByteArrayList scratch = new ByteArrayList();

        public ByteArrayList encode(ByteArrayList src, ByteArrayList dst, ByteArrayList encoded) {
            // Search for the infix that can we can encode and remove from src
            // to get a maximum-length prefix of dst. This could be done more efficiently
            // by running a smarter longest-common-subsequence algorithm and some pruning (?).

            // For now, naive loop should do.

            int maxInfixIndex = 0;
            int maxInfixLength = 0;
            int maxSubsequenceLength = 0;
            for (int i = 0; i < src.size(); i++) {
                // TODO: (opt) if current prefix length < i we can break immediately. 

                for (int j = 0; j < src.size() - i; j++) {
                    // Compute temporary src with the infix removed.
                    scratch.clear();
                    scratch.add(src.buffer, 0, i);
                    scratch.add(src.buffer, i + j, src.size() - (i + j));

                    int sharedPrefix = sharedPrefixLength(scratch, dst);

                    // Only update maxSubsequenceLength if we will be able to encode it.
                    if (sharedPrefix > maxSubsequenceLength
                            && i < REMOVE_EVERYTHING
                            && j < REMOVE_EVERYTHING) {
                        maxSubsequenceLength = sharedPrefix;
                        maxInfixIndex = i;
                        maxInfixLength = j;
                    }
                }
            }

            int truncateSuffixBytes = src.size() - (maxInfixLength + maxSubsequenceLength);
            if (maxInfixIndex >= REMOVE_EVERYTHING ||
                maxInfixLength >= REMOVE_EVERYTHING ||
                truncateSuffixBytes >= REMOVE_EVERYTHING) {
                maxInfixIndex = maxSubsequenceLength = 0;
                maxInfixLength = truncateSuffixBytes = REMOVE_EVERYTHING;
            }

            encoded.add((byte) ((maxInfixIndex       + 'A') & 0xFF));
            encoded.add((byte) ((maxInfixLength      + 'A') & 0xFF));
            encoded.add((byte) ((truncateSuffixBytes + 'A') & 0xFF));
            encoded.add(dst.buffer, maxSubsequenceLength, dst.size() - maxSubsequenceLength);

            return encoded;
        }

        public ByteArrayList decode(ByteArrayList src, ByteArrayList encoded, ByteArrayList dst) {
            int infixIndex  = (encoded.get(0) - 'A') & 0xFF;
            int infixLength = (encoded.get(1) - 'A') & 0xFF;
            int truncateSuffixBytes = (encoded.get(2) - 'A') & 0xFF;

            if (infixLength == REMOVE_EVERYTHING ||
                truncateSuffixBytes == REMOVE_EVERYTHING) {
                infixIndex = 0;
                infixLength = src.size();
                truncateSuffixBytes = 0;
            }

            dst.add(src.buffer, 0, infixIndex);
            dst.add(src.buffer, infixIndex + infixLength, src.size() - (infixIndex + infixLength + truncateSuffixBytes));
            dst.add(encoded.buffer, 3, encoded.size() - 3);

            return dst;
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName();
        }        
    }

    /**
     * Compute the length of the shared prefix between two byte sequences.
     */
    private static int sharedPrefixLength(ByteArrayList a, ByteArrayList b) {
        final int max = Math.min(a.size(), b.size());
        int i = 0;
        while (i < max && a.get(i) == b.get(i)) {
            i++;
        }
        return i;
    }
    
    /**
     * Compute the length of the shared prefix between two byte sequences.
     */
    private static int sharedPrefixLength(ByteArrayList a, int aStart, ByteArrayList b, int bStart) {

        int i = 0;
        while (aStart < a.size() && 
               bStart < b.size() &&
               a.get(aStart++) == b.get(bStart++)) {
            i++;
        }
        return i;
    }
}
