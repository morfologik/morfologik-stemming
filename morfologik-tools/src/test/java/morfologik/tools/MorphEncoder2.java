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
