package morfologik.stemming;

import java.nio.ByteBuffer;

/**
 * Encodes <code>dst</code> relative to <code>src</code> by trimming whatever
 * non-equal suffix and prefix <code>src</code> and <code>dst</code> have. The
 * output code is (bytes):
 * 
 * <pre>
 * {P}{K}{suffix}
 * </pre>
 * 
 * where (<code>P</code> - 'A') bytes should be trimmed from the start of
 * <code>src</code>, (<code>K</code> - 'A') bytes should be trimmed from the
 * end of <code>src</code> and then the <code>suffix</code> should be appended
 * to the resulting byte sequence.
 * 
 * <p>
 * Examples:
 * </p>
 * 
 * <pre>
 * src: abc
 * dst: abcd
 * encoded: AAd
 * 
 * src: abc
 * dst: xyz
 * encoded: ADxyz
 * </pre>
 */
public class TrimPrefixAndSuffixEncoder implements ISequenceEncoder {
  /**
   * Maximum encodable single-byte code.
   */
  private static final int REMOVE_EVERYTHING = 255;

  public ByteBuffer encode(ByteBuffer reuse, ByteBuffer source, ByteBuffer target) {
    // Search for the maximum matching subsequence that can be encoded. 
    int maxSubsequenceLength = 0;
    int maxSubsequenceIndex = 0;
    for (int i = 0; i < source.remaining(); i++) {
      // prefix at i => shared subsequence (infix)
      int sharedPrefix = BufferUtils.sharedPrefixLength(source, i, target, 0);
      // Only update maxSubsequenceLength if we will be able to encode it.
      if (sharedPrefix > maxSubsequenceLength && i < REMOVE_EVERYTHING
          && (source.remaining() - (i + sharedPrefix)) < REMOVE_EVERYTHING) {
        maxSubsequenceLength = sharedPrefix;
        maxSubsequenceIndex = i;
      }
    }

    // Determine how much to remove (and where) from src to get a prefix of dst.
    int truncatePrefixBytes = maxSubsequenceIndex;
    int truncateSuffixBytes = (source.remaining() - (maxSubsequenceIndex + maxSubsequenceLength));
    if (truncatePrefixBytes >= REMOVE_EVERYTHING || truncateSuffixBytes >= REMOVE_EVERYTHING) {
      maxSubsequenceIndex = maxSubsequenceLength = 0;
      truncatePrefixBytes = truncateSuffixBytes = REMOVE_EVERYTHING;
    }

    final int len1 = target.remaining() - maxSubsequenceLength;
    reuse = BufferUtils.clearAndEnsureCapacity(reuse, 2 + len1);

    assert target.hasArray() && 
           target.position() == 0 && 
           target.arrayOffset() == 0;

    reuse.put((byte) ((truncatePrefixBytes + 'A') & 0xFF));
    reuse.put((byte) ((truncateSuffixBytes + 'A') & 0xFF));
    reuse.put(target.array(), maxSubsequenceLength, len1);
    reuse.flip();

    return reuse;
  }
  
  @Override
  public int prefixBytes() {
    return 2;
  }

  public ByteBuffer decode(ByteBuffer reuse, ByteBuffer source, ByteBuffer encoded) {
    assert encoded.remaining() >= 2;

    final int p = encoded.position();
    int truncatePrefixBytes = (encoded.get(p)     - 'A') & 0xFF;
    int truncateSuffixBytes = (encoded.get(p + 1) - 'A') & 0xFF;

    if (truncatePrefixBytes == REMOVE_EVERYTHING || 
        truncateSuffixBytes == REMOVE_EVERYTHING) {
      truncatePrefixBytes = source.remaining();
      truncateSuffixBytes = 0;
    }

    assert source.hasArray() && 
           source.position() == 0 && 
           source.arrayOffset() == 0;

    assert encoded.hasArray() && 
           encoded.position() == 0 && 
           encoded.arrayOffset() == 0;

    final int len1 = source.remaining() - (truncateSuffixBytes + truncatePrefixBytes);
    final int len2 = encoded.remaining() - 2;
    reuse = BufferUtils.clearAndEnsureCapacity(reuse, len1 + len2);

    reuse.put(source.array(), truncatePrefixBytes, len1);
    reuse.put(encoded.array(), 2, len2);
    reuse.flip();

    return reuse;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}