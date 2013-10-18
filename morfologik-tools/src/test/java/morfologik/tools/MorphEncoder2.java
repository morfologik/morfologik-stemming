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
            return "TrimSuffixEncoder";
        }
    }

    /**
     * TODO: add javadoc on encoding format.
     */
    public static class TrimPrefixAndSuffixEncoder implements IEncoder {
        public ByteArrayList encode(ByteArrayList src, ByteArrayList dst, ByteArrayList encoded) {
            // Search for the maximum matching subsequence that is encodable. 
            int maxSubsequenceLength = 0;
            int maxSubsequenceIndex = 0;
            for (int i = 0; i < src.size(); i++) {
                int sharedPrefix = sharedPrefixLength(src, i, dst, 0);
                if (sharedPrefix > maxSubsequenceLength
                        && i < REMOVE_EVERYTHING
                        && (src.size() - (i + sharedPrefix)) < REMOVE_EVERYTHING) {
                    maxSubsequenceLength = sharedPrefix;
                    maxSubsequenceIndex = i;
                }
            }

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
            // TODO: write me.
            return dst;
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
