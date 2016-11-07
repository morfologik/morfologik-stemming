package morfologik.stemming;

import java.nio.ByteBuffer;

/**
 * Encodes <code>dst</code> relative to <code>src</code> by trimming whatever
 * non-equal suffix and infix <code>src</code> and <code>dst</code> have. The
 * output code is (bytes):
 * 
 * <pre>
 * {X}{L}{K}{suffix}
 * </pre>
 * 
 * where <code>src's</code> infix at position (<code>X</code> - 'A') and of
 * length (<code>L</code> - 'A') should be removed, then (<code>K</code> -
 * 'A') bytes should be trimmed from the end and then the <code>suffix</code>
 * should be appended to the resulting byte sequence.
 * 
 * <p>
 * Examples:
 * </p>
 * 
 * <pre>
 * src: ayz
 * dst: abc
 * encoded: AACbc
 * 
 * src: aillent
 * dst: aller
 * encoded: BBCr
 * </pre>
 */
public class TrimInfixAndSuffixEncoder implements ISequenceEncoder {
  /**
   * Maximum encodable single-byte code.
   */
  private static final int REMOVE_EVERYTHING = 255;
  private ByteBuffer scratch = ByteBuffer.allocate(0);

  public ByteBuffer encode(ByteBuffer reuse, ByteBuffer source, ByteBuffer target) {
    assert source.hasArray() && 
           source.position() == 0 && 
           source.arrayOffset() == 0;

    assert target.hasArray() && 
           target.position() == 0 && 
           target.arrayOffset() == 0;

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
    int maxSubsequenceLength = BufferUtils.sharedPrefixLength(source, target);
    int maxInfixLength = 0;
    for (int i : new int[] { 0, maxSubsequenceLength }) {
      for (int j = 1; j <= source.remaining() - i; j++) {
        // Compute temporary src with the infix removed.
        // Concatenate in scratch space for simplicity.
        final int len2 = source.remaining() - (i + j);
        scratch = BufferUtils.clearAndEnsureCapacity(scratch, i + len2);
        scratch.put(source.array(), 0, i);
        scratch.put(source.array(), i + j, len2);
        scratch.flip();

        int sharedPrefix = BufferUtils.sharedPrefixLength(scratch, target);

        // Only update maxSubsequenceLength if we will be able to encode it.
        if (sharedPrefix > 0 && sharedPrefix > maxSubsequenceLength && i < REMOVE_EVERYTHING && j < REMOVE_EVERYTHING) {
          maxSubsequenceLength = sharedPrefix;
          maxInfixIndex = i;
          maxInfixLength = j;
        }
      }
    }

    int truncateSuffixBytes = source.remaining() - (maxInfixLength + maxSubsequenceLength);

    // Special case: if we're removing the suffix in the infix code, move it
    // to the suffix code instead.
    if (truncateSuffixBytes == 0 && 
        maxInfixIndex + maxInfixLength == source.remaining()) {
      truncateSuffixBytes = maxInfixLength;
      maxInfixIndex = maxInfixLength = 0;
    }

    if (maxInfixIndex >= REMOVE_EVERYTHING || 
        maxInfixLength >= REMOVE_EVERYTHING || 
        truncateSuffixBytes >= REMOVE_EVERYTHING) {
      maxInfixIndex = maxSubsequenceLength = 0;
      maxInfixLength = truncateSuffixBytes = REMOVE_EVERYTHING;
    }

    final int len1 = target.remaining() - maxSubsequenceLength;
    reuse = BufferUtils.clearAndEnsureCapacity(reuse, 3 + len1);

    reuse.put((byte) ((maxInfixIndex + 'A') & 0xFF));
    reuse.put((byte) ((maxInfixLength + 'A') & 0xFF));
    reuse.put((byte) ((truncateSuffixBytes + 'A') & 0xFF));
    reuse.put(target.array(), maxSubsequenceLength, len1);
    reuse.flip();

    return reuse;
  }

  @Override
  public int prefixBytes() {
    return 3;
  }
  
  public ByteBuffer decode(ByteBuffer reuse, ByteBuffer source, ByteBuffer encoded) {
    assert encoded.remaining() >= 3;

    final int p = encoded.position();
    int infixIndex = (encoded.get(p) - 'A') & 0xFF;
    int infixLength = (encoded.get(p + 1) - 'A') & 0xFF;
    int truncateSuffixBytes = (encoded.get(p + 2) - 'A') & 0xFF;

    if (infixLength == REMOVE_EVERYTHING || 
        truncateSuffixBytes == REMOVE_EVERYTHING) {
      infixIndex = 0;
      infixLength = source.remaining();
      truncateSuffixBytes = 0;
    }

    final int len1 = source.remaining() - (infixIndex + infixLength + truncateSuffixBytes);
    final int len2 = encoded.remaining() - 3;
    reuse = BufferUtils.clearAndEnsureCapacity(reuse, infixIndex + len1 + len2);

    assert encoded.hasArray() && 
           encoded.position() == 0 && 
           encoded.arrayOffset() == 0;

    assert source.hasArray() && 
           source.position() == 0 && 
           source.arrayOffset() == 0;

    reuse.put(source.array(), 0, infixIndex);
    reuse.put(source.array(), infixIndex + infixLength, len1);
    reuse.put(encoded.array(), 3, len2);
    reuse.flip();

    return reuse;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}