package morfologik.stemming;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public final class BufferUtils {
  /**
   * No instances.
   */
  private BufferUtils() {
    // empty
  }

  /**
   * Ensure the byte buffer's capacity. If a new buffer is allocated, its
   * content is empty (the old buffer's contents is not copied).
   * 
   * @param capacity The required capacity of the buffer.
   * 
   * @param buffer
   *          The buffer to check or <code>null</code> if a new buffer should be
   *          allocated.
   *
   * @return Returns the same buffer or a new buffer with the given capacity. 
   */
  public static ByteBuffer ensureCapacity(ByteBuffer buffer, int capacity) {
    // TODO: GH-63 
    if (buffer == null || buffer.capacity() < capacity) {
      buffer = ByteBuffer.allocate(capacity);
    }
    return buffer;
  }

  /**
   * Ensure the char buffer's capacity. If a new buffer is allocated, its
   * content is empty (the old buffer's contents is not copied).
   * 
   * @param capacity The required capacity of the buffer.
   * 
   * @param buffer
   *          The buffer to check or <code>null</code> if a new buffer should be
   *          allocated.
   *
   * @return Returns the same buffer or a new buffer with the given capacity. 
   */
  public static CharBuffer ensureCapacity(CharBuffer buffer, int capacity) {
    // TODO: GH-63 
    if (buffer == null || buffer.capacity() < capacity) {
      buffer = CharBuffer.allocate(capacity);
    }
    return buffer;
  }

  /**
   * @param buffer The buffer to convert to a string.
   * @param charset The charset to use when converting bytes to characters.
   * @return A string representation of buffer's content.
   */
  public static String toString(ByteBuffer buffer, Charset charset) {
    buffer = buffer.slice();
    byte [] buf = new byte [buffer.remaining()];
    buffer.get(buf);
    return new String(buf, charset);
  }

  /**
   * @param buffer The buffer to read from.
   * @return Returns the remaining bytes from the buffer copied to an array.
   */
  public static byte[] toArray(ByteBuffer buffer) {
    byte [] dst = new byte [buffer.remaining()];
    buffer.mark();
    buffer.get(dst);
    buffer.reset();
    return dst;
  }

  /**
   * Compute the length of the shared prefix between two byte sequences.
   */
  static int sharedPrefixLength(ByteBuffer a, int aStart, ByteBuffer b, int bStart) {
    int i = 0;
    final int max = Math.min(a.remaining() - aStart, b.remaining() - bStart);
    aStart += a.position();
    bStart += b.position();
    while (i < max && a.get(aStart++) == b.get(bStart++)) {
      i++;
    }
    return i;
  }

  /**
   * Compute the length of the shared prefix between two byte sequences.
   */
  static int sharedPrefixLength(ByteBuffer a, ByteBuffer b) {
    return sharedPrefixLength(a, 0, b, 0);
  }
}
