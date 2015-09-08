package morfologik.stemming;

import java.nio.ByteBuffer;

/**
 * Container class for sequence encoders.
 */
public final class Encoders {
  private Encoders() {
  }

  /**
   * Maximum encodable single-byte code.
   */
  private static final int REMOVE_EVERYTHING = 255;

  /**
   * Encodes <code>dst</code> relative to <code>src</code> by trimming whatever
   * non-equal suffix <code>src</code> has. The output code is (bytes):
   * 
   * <pre>
   * {K}{suffix}
   * </pre>
   * 
   * where (<code>K</code> - 'A') bytes should be trimmed from the end of
   * <code>src</code> and then the <code>suffix</code> should be appended to the
   * resulting byte sequence.
   * 
   * <p>
   * Examples:
   * </p>
   * 
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
   * <p>
   * <strong>Note:</strong> The code length is a single byte. If equal to
   * {@link Encoders#REMOVE_EVERYTHING} the entire <code>src</code>
   * sequence should be discarded.
   * </p>
   */
  public static class TrimSuffixEncoder implements ISequenceEncoder {
    public ByteBuffer encode(ByteBuffer reuse, ByteBuffer source, ByteBuffer target) {
      int sharedPrefix = sharedPrefixLength(source, target);
      int truncateBytes = source.remaining() - sharedPrefix;
      if (truncateBytes >= REMOVE_EVERYTHING) {
        truncateBytes = REMOVE_EVERYTHING;
        sharedPrefix = 0;
      }

      reuse = BufferUtils.ensureCapacity(reuse, 1 + target.remaining() - sharedPrefix); 
      reuse.clear();

      assert target.hasArray() && 
             target.position() == 0 && 
             target.arrayOffset() == 0;

      final byte suffixTrimCode = (byte) (truncateBytes + 'A');
      reuse.put(suffixTrimCode)
           .put(target.array(), sharedPrefix, target.remaining() - sharedPrefix)
           .flip();

      return reuse;
    }

    public ByteBuffer decode(ByteBuffer reuse, ByteBuffer source, ByteBuffer encoded) {
      assert encoded.remaining() >= 1;

      int suffixTrimCode = encoded.get(encoded.position());
      int truncateBytes = (suffixTrimCode - 'A') & 0xFF;
      if (truncateBytes == REMOVE_EVERYTHING) {
        truncateBytes = source.remaining();
      }

      final int len1 = source.remaining() - truncateBytes;
      final int len2 = encoded.remaining() - 1;

      reuse = BufferUtils.ensureCapacity(reuse, len1 + len2);
      reuse.clear();

      assert source.hasArray() && 
             source.position() == 0 && 
             source.arrayOffset() == 0;

      assert encoded.hasArray() && 
             encoded.position() == 0 && 
             encoded.arrayOffset() == 0;

      reuse.put(source.array(), 0, len1)
           .put(encoded.array(), 1, len2)
           .flip();

      return reuse;
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
   * 
   * <p>
   * <strong>Note:</strong> Each code's length is a single byte. If any is equal
   * to {@link Encoders#REMOVE_EVERYTHING} the entire <code>src</code>
   * sequence should be discarded.
   * </p>
   */
  public static class TrimPrefixAndSuffixEncoder implements ISequenceEncoder {
    public ByteBuffer encode(ByteBuffer reuse, ByteBuffer source, ByteBuffer target) {
      // Search for the maximum matching subsequence that can be encoded. 
      int maxSubsequenceLength = 0;
      int maxSubsequenceIndex = 0;
      for (int i = 0; i < source.remaining(); i++) {
        // prefix at i => shared subsequence (infix)
        int sharedPrefix = sharedPrefixLength(source, i, target, 0);
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
      reuse = BufferUtils.ensureCapacity(reuse, 2 + len1);
      reuse.clear();

      assert target.hasArray() && 
             target.position() == 0 && 
             target.arrayOffset() == 0;

      reuse.put((byte) ((truncatePrefixBytes + 'A') & 0xFF));
      reuse.put((byte) ((truncateSuffixBytes + 'A') & 0xFF));
      reuse.put(target.array(), maxSubsequenceLength, len1);
      reuse.flip();

      return reuse;
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
      reuse = BufferUtils.ensureCapacity(reuse, len1 + len2);
      reuse.clear();

      reuse.put(source.array(), truncatePrefixBytes, len1);
      reuse.put(encoded.array(), 2, len2);
      reuse.flip();

      return reuse;
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
   * 
   * <p>
   * <strong>Note:</strong> Each code's length is a single byte. If any is equal
   * to {@link Encoders#REMOVE_EVERYTHING} the entire <code>src</code>
   * sequence should be discarded.
   * </p>
   */
  public static class TrimInfixAndSuffixEncoder implements ISequenceEncoder {
    ByteBuffer scratch = ByteBuffer.allocate(0);

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
      int maxSubsequenceLength = sharedPrefixLength(source, target);
      int maxInfixLength = 0;
      for (int i : new int[] { 0, maxSubsequenceLength }) {
        for (int j = 1; j <= source.remaining() - i; j++) {
          // Compute temporary src with the infix removed.
          // Concatenate in scratch space for simplicity.
          final int len2 = source.remaining() - (i + j);
          scratch = BufferUtils.ensureCapacity(scratch, i + len2);
          scratch.clear();
          scratch.put(source.array(), 0, i);
          scratch.put(source.array(), i + j, len2);
          scratch.flip();

          int sharedPrefix = sharedPrefixLength(scratch, target);

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
      reuse = BufferUtils.ensureCapacity(reuse, 3 + len1);
      reuse.clear();

      reuse.put((byte) ((maxInfixIndex + 'A') & 0xFF));
      reuse.put((byte) ((maxInfixLength + 'A') & 0xFF));
      reuse.put((byte) ((truncateSuffixBytes + 'A') & 0xFF));
      reuse.put(target.array(), maxSubsequenceLength, len1);
      reuse.flip();

      return reuse;
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
      reuse = BufferUtils.ensureCapacity(reuse, infixIndex + len1 + len2);
      reuse.clear();

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
    public EncoderType type() {
      return EncoderType.INFIX;
    }

    @Override
    public String toString() {
      return getClass().getSimpleName();
    }
  }

  /**
   * No relative encoding at all (full target form is returned).
   */
  public static class NoEncoder implements ISequenceEncoder {
    @Override
    public ByteBuffer encode(ByteBuffer reuse, ByteBuffer source, ByteBuffer target) {
      reuse = BufferUtils.ensureCapacity(reuse, target.remaining());
      reuse.clear();

      assert target.hasArray() && 
             target.position() == 0 && 
             target.arrayOffset() == 0;

      reuse.put(target.array(), 0, target.remaining())
           .flip();

      return reuse;
    }

    @Override
    public ByteBuffer decode(ByteBuffer reuse, ByteBuffer source, ByteBuffer encoded) {
      reuse = BufferUtils.ensureCapacity(reuse, encoded.remaining());
      reuse.clear();

      assert encoded.hasArray() && 
             encoded.position() == 0 && 
             encoded.arrayOffset() == 0;

      reuse.put(encoded.array(), 0, encoded.remaining());
      reuse.flip();
      return reuse;
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
  static int sharedPrefixLength(ByteBuffer a, ByteBuffer b) {
    final int max = Math.min(a.remaining(), b.remaining());
    int i = 0;
    final int pa = a.position();
    final int pb = b.position();
    while (i < max && a.get(pa + i) == b.get(pb + i)) {
      i++;
    }
    return i;
  }

  /**
   * Compute the length of the shared prefix between two byte sequences.
   */
  private static int sharedPrefixLength(ByteBuffer a, int aStart, ByteBuffer b, int bStart) {
    int i = 0;
    final int max = Math.min(a.remaining() - aStart, b.remaining() - bStart);
    aStart += a.position();
    bStart += b.position();
    while (i < max && a.get(aStart++) == b.get(bStart++)) {
      i++;
    }
    return i;
  }

  public static ISequenceEncoder forType(EncoderType encType) {
    switch (encType) {
      case INFIX:
        return new TrimInfixAndSuffixEncoder();
      case PREFIX:
        return new TrimPrefixAndSuffixEncoder();
      case SUFFIX:
        return new TrimSuffixEncoder();
      case NONE:
        return new NoEncoder();
    }
    throw new RuntimeException("Unknown encoder: " + encType);
  }
}
