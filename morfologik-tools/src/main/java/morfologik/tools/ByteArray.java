package morfologik.tools;

import java.util.Arrays;

/**
 * A simple (growable) byte buffer.
 */
public final class ByteArray {
  public byte[] buffer;

  private int elements;

  public ByteArray(byte[] bytes) {
    this.buffer = new byte [bytes.length];
    add(bytes);
  }
  
  public ByteArray() {
    this.buffer = new byte [16];
  }

  public int size() {
    return elements;
  }

  public void add(byte element) {
    ensureCapacity(size() + 1);
    buffer[elements++] = element;
  }

  public void add(byte[] array, int start, int length) {
    ensureCapacity(size() + length);
    System.arraycopy(array, start,
                     buffer, elements,
                     length);
    elements += length;
  }

  public void add(byte[] array) {
    add(array, 0, array.length);
  }

  public int get(int index) {
    assert index >= 0 && 
           index < elements;
    return buffer[index];
  }

  public void clear() {
    elements = 0;
  }

  private void ensureCapacity(int capacity) {
    if (capacity > buffer.length) {
      buffer = Arrays.copyOf(buffer, capacity);
    }
  }

  public byte[] toArray() {
    return Arrays.copyOf(buffer, elements);
  }
  
  @Override
  public boolean equals(Object obj) {
      return obj != null &&
             getClass() == obj.getClass() &&
             equalElements(getClass().cast(obj));
  }
  
  private boolean equalElements(ByteArray other) {
    int max = size();
    if (other.size() != max) {
      return false;
    }

    for (int i = 0; i < max; i++) {
      if (!((other.get(i)) == ( get(i)))) {
        return false;
      }
    }

    return true;
  }
}
