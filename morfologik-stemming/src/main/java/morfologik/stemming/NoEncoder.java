package morfologik.stemming;

import java.nio.ByteBuffer;

/**
 * No relative encoding at all (full target form is returned).
 */
public class NoEncoder implements ISequenceEncoder {
  @Override
  public ByteBuffer encode(ByteBuffer reuse, ByteBuffer source, ByteBuffer target) {
    reuse = BufferUtils.clearAndEnsureCapacity(reuse, target.remaining());

    target.mark();
    reuse.put(target)
         .flip();
    target.reset();

    return reuse;
  }

  @Override
  public ByteBuffer decode(ByteBuffer reuse, ByteBuffer source, ByteBuffer encoded) {
    reuse = BufferUtils.clearAndEnsureCapacity(reuse, encoded.remaining());

    encoded.mark();
    reuse.put(encoded)
         .flip();
    encoded.reset();
         
    return reuse;
  }

  @Override
  public int prefixBytes() {
    return 0;
  }
  
  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}