package morfologik.tools;

import morfologik.stemming.EncoderType;

import com.carrotsearch.hppc.ByteArrayList;

/**
 * Container class for sequence encoders.
 */
public final class SequenceEncoders {
    private SequenceEncoders() {}

    /**
     * Maximum encodable single-byte code.
     */
    private static final int REMOVE_EVERYTHING = 255;

    public static interface IEncoder {
        public ByteArrayList encode(ByteArrayList src, ByteArrayList derived, ByteArrayList encodedBuffer);
        public ByteArrayList decode(ByteArrayList src, ByteArrayList encoded, ByteArrayList derivedBuffer);
        public EncoderType type();
    }

    /**
     * Encodes <code>dst</code> relative to <code>src</code> by trimming 
     * whatever non-equal suffix <code>src</code> has. The output code is (bytes):
     * <pre>
     * {K}{suffix}
     * </pre>
     * where (<code>K</code> - 'A') bytes should be trimmed from the end of <code>src</code> 
     * and then the <code>suffix</code> should be appended to the resulting byte sequence.
     * 
     * <p>Examples:</p>
     * <pre>
     * src: foo
     * dst: foobar
     * encoded: Abar
     * 
     * src: foo
     * dst: bar
     * encoded: Dbar
     * </pre>
     * 
     * <p><strong>Note:</strong> The code length is a single byte. If equal to 
     * {@link SequenceEncoders#REMOVE_EVERYTHING} the entire <code>src</code> sequence
     * should be discarded.</p>
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
        public EncoderType type() {
            return EncoderType.SUFFIX;
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    /**
     * Encodes <code>dst</code> relative to <code>src</code> by trimming 
     * whatever non-equal suffix and prefix <code>src</code> and <code>dst</code> have. 
     * The output code is (bytes):
     * <pre>
     * {P}{K}{suffix}
     * </pre>
     * where (<code>P</code> - 'A') bytes should be trimmed from the start of <code>src</code>,
     *  (<code>K</code> - 'A') bytes should be trimmed from the end of <code>src</code>
     * and then the <code>suffix</code> should be appended to the resulting byte sequence.
     * 
     * <p>Examples:</p>
     * <pre>
     * src: abc
     * dst: abcd
     * encoded: AAd
     * 
     * src: abc
     * dst: xyz
     * encoded: ADxyz
     * </pre>
     * 
     * <p><strong>Note:</strong> Each code's length is a single byte. If any is equal to 
     * {@link SequenceEncoders#REMOVE_EVERYTHING} the entire <code>src</code> sequence
     * should be discarded.</p>
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
        public EncoderType type() {
            return EncoderType.PREFIX;
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName();
        }        
    }

    /**
     * Encodes <code>dst</code> relative to <code>src</code> by trimming 
     * whatever non-equal suffix and infix <code>src</code> and <code>dst</code> have. 
     * The output code is (bytes):
     * <pre>
     * {X}{L}{K}{suffix}
     * </pre>
     * where <code>src's</code> infix at position (<code>X</code> - 'A') and of length
     * (<code>L</code> - 'A') should be removed, then (<code>K</code> - 'A') bytes 
     * should be trimmed from the end
     * and then the <code>suffix</code> should be appended to the resulting byte sequence.
     * 
     * <p>Examples:</p>
     * <pre>
     * src: ayz
     * dst: abc
     * encoded: AACbc
     * 
     * src: aillent
     * dst: aller
     * encoded: BBCr
     * </pre>
     * 
     * <p><strong>Note:</strong> Each code's length is a single byte. If any is equal to 
     * {@link SequenceEncoders#REMOVE_EVERYTHING} the entire <code>src</code> sequence
     * should be discarded.</p>
     */
    public static class TrimInfixAndSuffixEncoder implements IEncoder {
        ByteArrayList scratch = new ByteArrayList();

        public ByteArrayList encode(ByteArrayList src, ByteArrayList dst, ByteArrayList encoded) {
            // Search for the infix that can we can encode and remove from src
            // to get a maximum-length prefix of dst. This could be done more efficiently
            // by running a smarter longest-common-subsequence algorithm and some pruning (?).
            //
            // For now, naive loop should do.

            // There can be only two positions for the infix to delete:
            // 1) we remove leading bytes, even if they are partially matching (but a longer match
            //    exists somewhere later on).
            // 2) we leave max. matching prefix and remove non-matching bytes that follow. 
            int maxInfixIndex = 0;
            int maxSubsequenceLength = sharedPrefixLength(src, dst);
            int maxInfixLength = 0;
            for (int i : new int [] {0, maxSubsequenceLength}) {
                for (int j = 1; j <= src.size() - i; j++) {
                    // Compute temporary src with the infix removed.
                    // Concatenate in scratch space for simplicity.
                    scratch.clear();
                    scratch.add(src.buffer, 0, i);
                    scratch.add(src.buffer, i + j, src.size() - (i + j));
    
                    int sharedPrefix = sharedPrefixLength(scratch, dst);

                    // Only update maxSubsequenceLength if we will be able to encode it.
                    if (sharedPrefix > 0 && 
                        sharedPrefix > maxSubsequenceLength &&
                        i < REMOVE_EVERYTHING &&
                        j < REMOVE_EVERYTHING) {
                        maxSubsequenceLength = sharedPrefix;
                        maxInfixIndex = i;
                        maxInfixLength = j;
                    }
                }
            }
            
            int truncateSuffixBytes = src.size() - (maxInfixLength + maxSubsequenceLength);
            
            // Special case: if we're removing the suffix in the infix code, move it
            // to the suffix code instead.
            if (truncateSuffixBytes == 0 &&
                maxInfixIndex + maxInfixLength == src.size()) {
                truncateSuffixBytes = maxInfixLength;
                maxInfixIndex = maxInfixLength = 0;
            }
                
            
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
        public EncoderType type() {
            return EncoderType.INFIX;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }        
    }
    
    /**
     * 
     */
    public static class CopyEncoder implements IEncoder {
        @Override
        public ByteArrayList encode(ByteArrayList src, ByteArrayList derived, ByteArrayList encodedBuffer)
        {
            encodedBuffer.add(derived.buffer, 0, derived.size());
            return encodedBuffer;
        }
        
        @Override
        public ByteArrayList decode(ByteArrayList src, ByteArrayList encoded, ByteArrayList derivedBuffer)
        {
            derivedBuffer.add(encoded.buffer, 0, encoded.size());
            return derivedBuffer;
        }
        
        @Override
        public EncoderType type() {
            return EncoderType.NONE;
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

    public static IEncoder forType(EncoderType encType)
    {
        switch (encType) {
            case INFIX:  return new TrimInfixAndSuffixEncoder();
            case PREFIX: return new TrimPrefixAndSuffixEncoder();
            case SUFFIX: return new TrimSuffixEncoder();
            case NONE:   return new CopyEncoder();
        }
        throw new RuntimeException("Unknown encoder: " + encType); 
    }
}
