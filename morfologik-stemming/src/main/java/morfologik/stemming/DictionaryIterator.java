package morfologik.stemming;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;

/**
 * An iterator over {@link WordData} entries of a {@link Dictionary}. The stems can be decoded from compressed format or
 * the compressed form can be preserved.
 */
public final class DictionaryIterator implements Iterator<WordData> {
  private final CharsetDecoder decoder;
  private final Iterator<ByteBuffer> entriesIter;
  private final WordData entry;
  private final byte separator;
  private final boolean decodeStems;

  private ByteBuffer inflectedBuffer = ByteBuffer.allocate(0);
  private CharBuffer inflectedCharBuffer = CharBuffer.allocate(0);
  private ByteBuffer temp = ByteBuffer.allocate(0);
  private final ISequenceEncoder sequenceEncoder;

  public DictionaryIterator(Dictionary dictionary, CharsetDecoder decoder, boolean decodeStems) {
    this.entriesIter = dictionary.fsa.iterator();
    this.separator = dictionary.metadata.getSeparator();
    this.sequenceEncoder = dictionary.metadata.getSequenceEncoderType().get();
    this.decoder = decoder;
    this.entry = new WordData(decoder);
    this.decodeStems = decodeStems;
  }

  public boolean hasNext() {
    return entriesIter.hasNext();
  }

  public WordData next() {
    final ByteBuffer entryBuffer = entriesIter.next();

    /*
     * Entries are typically: inflected<SEP>codedBase<SEP>tag so try to find this split.
     */
    byte[] ba = entryBuffer.array();
    int bbSize = entryBuffer.remaining();

    int sepPos;
    for (sepPos = 0; sepPos < bbSize; sepPos++) {
      if (ba[sepPos] == separator) {
        break;
      }
    }

    if (sepPos == bbSize) {
      throw new RuntimeException("Invalid dictionary " + "entry format (missing separator).");
    }

    inflectedBuffer = BufferUtils.clearAndEnsureCapacity(inflectedBuffer, sepPos);
    inflectedBuffer.put(ba, 0, sepPos);
    inflectedBuffer.flip();

    inflectedCharBuffer = BufferUtils.bytesToChars(decoder, inflectedBuffer, inflectedCharBuffer);
    entry.update(inflectedBuffer, inflectedCharBuffer);

    temp = BufferUtils.clearAndEnsureCapacity(temp, bbSize - sepPos);
    sepPos++;
    temp.put(ba, sepPos, bbSize - sepPos);
    temp.flip();

    ba = temp.array();
    bbSize = temp.remaining();

    /*
     * Find the next separator byte's position splitting word form and tag.
     */
    assert sequenceEncoder.prefixBytes() <= bbSize : sequenceEncoder.getClass() + " >? " + bbSize;
    sepPos = sequenceEncoder.prefixBytes();
    for (; sepPos < bbSize; sepPos++) {
      if (ba[sepPos] == separator)
        break;
    }

    /*
     * Decode the stem into stem buffer.
     */
    if (decodeStems) {
      entry.stemBuffer = sequenceEncoder.decode(entry.stemBuffer,
                                            inflectedBuffer,
                                            ByteBuffer.wrap(ba, 0, sepPos));
    } else {
      entry.stemBuffer = BufferUtils.clearAndEnsureCapacity(entry.stemBuffer, sepPos);
      entry.stemBuffer.put(ba, 0, sepPos);
      entry.stemBuffer.flip();
    }

    // Skip separator character, if present.
    if (sepPos + 1 <= bbSize) {
      sepPos++;
    }

    /*
     * Decode the tag data.
     */
    entry.tagBuffer = BufferUtils.clearAndEnsureCapacity(entry.tagBuffer, bbSize - sepPos);
    entry.tagBuffer.put(ba, sepPos, bbSize - sepPos);
    entry.tagBuffer.flip();

    return entry;
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }
}
