package morfologik.stemming;

import java.nio.ByteBuffer;

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
 */
public class TrimSuffixEncoder implements ISequenceEncoder {
  /**
   * Maximum encodable single-byte code.
   */
  private static final int REMOVE_EVERYTHING = 255;

  public ByteBuffer encode(ByteBuffer reuse, ByteBuffer source, ByteBuffer target) {
    int sharedPrefix = BufferUtils.sharedPrefixLength(source, target);
    int truncateBytes = source.remaining() - sharedPrefix;
    if (truncateBytes >= REMOVE_EVERYTHING) {
      truncateBytes = REMOVE_EVERYTHING;
      sharedPrefix = 0;
    }

    reuse = BufferUtils.clearAndEnsureCapacity(reuse, 1 + target.remaining() - sharedPrefix); 

    assert target.hasArray() && 
           target.position() == 0 && 
           target.arrayOffset() == 0;

    final byte suffixTrimCode = (byte) (truncateBytes + 'A');
    reuse.put(suffixTrimCode)
         .put(target.array(), sharedPrefix, target.remaining() - sharedPrefix)
         .flip();

    return reuse;
  }
  
  @Override
  public int prefixBytes() {
    return 1;
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

    reuse = BufferUtils.clearAndEnsureCapacity(reuse, len1 + len2);

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
  public String toString() {
    return getClass().getSimpleName();
  }
}