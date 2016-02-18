package morfologik.stemming;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;

public final class BufferUtils {
  /**
   * No instances.
   */
  private BufferUtils() {
    // empty
  }

  /**
   * Ensure the buffer's capacity is large enough to hold a given number
   * of elements. If the input buffer is not large enough, a new buffer is allocated
   * and returned.
   * 
   * @param elements The required number of elements to be appended to the buffer.
   * 
   * @param buffer
   *          The buffer to check or <code>null</code> if a new buffer should be
   *          allocated.
   *
   * @return Returns the same buffer or a new buffer with the given capacity. 
   */
  public static ByteBuffer clearAndEnsureCapacity(ByteBuffer buffer, int elements) {
    if (buffer == null || buffer.capacity() < elements) {
      buffer = ByteBuffer.allocate(elements);
    } else {
      buffer.clear();
    }
    return buffer;
  }

  /**
   * Ensure the buffer's capacity is large enough to hold a given number
   * of elements. If the input buffer is not large enough, a new buffer is allocated
   * and returned.
   * 
   * @param elements The required number of elements to be appended to the buffer.
   * 
   * @param buffer
   *          The buffer to check or <code>null</code> if a new buffer should be
   *          allocated.
   *
   * @return Returns the same buffer or a new buffer with the given capacity. 
   */
  public static CharBuffer clearAndEnsureCapacity(CharBuffer buffer, int elements) {
    if (buffer == null || buffer.capacity() < elements) {
      buffer = CharBuffer.allocate(elements);
    } else {
      buffer.clear();
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

  public static String toString(CharBuffer buffer) {
    buffer = buffer.slice();
    char [] buf = new char [buffer.remaining()];
    buffer.get(buf);
    return new String(buf);
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

  /**
   * Convert byte buffer's content into characters. The input buffer's bytes are not
   * consumed (mark is set and reset).
   */
  public static CharBuffer bytesToChars(CharsetDecoder decoder, ByteBuffer bytes, CharBuffer chars) {
    assert decoder.malformedInputAction() == CodingErrorAction.REPORT;

    chars = clearAndEnsureCapacity(chars, (int) (bytes.remaining() * decoder.maxCharsPerByte()));

    bytes.mark();
    decoder.reset();
    CoderResult cr = decoder.decode(bytes, chars, true);
    if (cr.isError()) {
      bytes.reset();
      try {
        cr.throwException();
      } catch (CharacterCodingException e) {
        throw new RuntimeException("Input cannot be mapped to bytes using encoding "
            + decoder.charset().name() + ": " + Arrays.toString(toArray(bytes)), e);
      }
    }

    assert cr.isUnderflow();  // This should be guaranteed by ensuring max. capacity.
    cr = decoder.flush(chars);
    assert cr.isUnderflow();

    chars.flip();
    bytes.reset();

    return chars;
  }

  /**
   * Convert chars into bytes.
   */
  public static ByteBuffer charsToBytes(CharsetEncoder encoder, CharBuffer chars, ByteBuffer bytes) 
      throws UnmappableInputException {
    assert encoder.malformedInputAction() == CodingErrorAction.REPORT;

    bytes = clearAndEnsureCapacity(bytes, (int) (chars.remaining() * encoder.maxBytesPerChar()));

    chars.mark();
    encoder.reset();

    CoderResult cr = encoder.encode(chars, bytes, true);
    if (cr.isError()) {
      chars.reset();
      try {
        cr.throwException();
      } catch (CharacterCodingException e) {
        throw new UnmappableInputException("Input cannot be mapped to characters using encoding "
            + encoder.charset().name() + ": " + Arrays.toString(toArray(bytes)), e);
      }
    }

    assert cr.isUnderflow();  // This should be guaranteed by ensuring max. capacity.
    cr = encoder.flush(bytes);
    assert cr.isUnderflow();

    bytes.flip();
    chars.reset();

    return bytes;
  }
}
