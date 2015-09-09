package morfologik.stemming;

import java.nio.ByteBuffer;

/**
 * No relative encoding at all (full target form is returned).
 */
public class NoEncoder implements ISequenceEncoder {
  @Override
  public ByteBuffer encode(ByteBuffer reuse, ByteBuffer source, ByteBuffer target) {
    reuse = BufferUtils.ensureCapacity(reuse, target.remaining());
    reuse.clear();

    target.mark();
    reuse.put(target)
         .flip();
    target.reset();

    return reuse;
  }

  @Override
  public ByteBuffer decode(ByteBuffer reuse, ByteBuffer source, ByteBuffer encoded) {
    reuse = BufferUtils.ensureCapacity(reuse, encoded.remaining());
    reuse.clear();

    encoded.mark();
    reuse.put(encoded)
         .flip();
    encoded.reset();
         
    return reuse;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}