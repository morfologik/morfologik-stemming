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