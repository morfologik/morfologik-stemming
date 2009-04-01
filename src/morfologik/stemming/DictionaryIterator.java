package morfologik.stemming;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;

import morfologik.util.BufferUtils;

/**
 * An iterator over {@link WordData} entries of a {@link Dictionary}. The stems
 * can be decoded from compressed format or the compressed form can be preserved.
 */
public final class DictionaryIterator implements Iterator<WordData> {
    private final CharsetDecoder decoder;
    private final Iterator<ByteBuffer> entriesIter;
    private final WordData entry;
    private final byte separator;
    private final DictionaryMetadata dictionaryMetadata;
    private final boolean decodeStems;

    private ByteBuffer inflectedBuffer = ByteBuffer.allocate(0);
    private CharBuffer inflectedCharBuffer = CharBuffer.allocate(0);
    private ByteBuffer temp = ByteBuffer.allocate(0);

    public DictionaryIterator(Dictionary dictionary, CharsetDecoder decoder, boolean decodeStems) {
	this.entriesIter = dictionary.fsa.iterator();
	this.separator = dictionary.metadata.separator;
	this.dictionaryMetadata = dictionary.metadata;
	this.decoder = decoder;
	this.entry = new WordData(decoder);
	this.decodeStems = decodeStems;
    }

    public boolean hasNext() {
	return entriesIter.hasNext();
    }

    public WordData next() {
	final ByteBuffer entryBuffer = entriesIter.next();
	entry.reset();

	/*
	 * Entries are typically: inflected<SEP>codedBase<SEP>tag so try to find
	 * this split.
	 */
	byte[] ba = entryBuffer.array();
	int bbSize = entryBuffer.remaining();

	int sepPos;
	for (sepPos = 0; sepPos < bbSize; sepPos++) {
	    if (ba[sepPos] == separator)
		break;
	}

	if (sepPos == bbSize) {
	    throw new RuntimeException("Invalid dictionary "
		    + "entry format (missing separator).");
	}

	inflectedBuffer.clear();
	inflectedBuffer = BufferUtils.ensureCapacity(inflectedBuffer, sepPos);
	inflectedBuffer.put(ba, 0, sepPos);
	inflectedBuffer.flip();

	inflectedCharBuffer = bytesToChars(inflectedBuffer, inflectedCharBuffer);
	entry.wordBuffer = inflectedBuffer;
	entry.wordCharSequence = inflectedCharBuffer;

	temp.clear();
	temp = BufferUtils.ensureCapacity(temp, bbSize - sepPos);
	sepPos++;
	temp.put(ba, sepPos, bbSize - sepPos);
	temp.flip();

	ba = temp.array();
	bbSize = temp.remaining();

	/*
	 * Find the next separator byte's position splitting word form and tag.
	 */
	sepPos = 0;
	for (; sepPos < bbSize; sepPos++) {
	    if (ba[sepPos] == separator)
		break;
	}

	/*
	 * Decode the stem into stem buffer.
	 */
	entry.stemBuffer.clear();
	if (decodeStems) {
	    entry.stemBuffer = DictionaryLookup.decodeStem(entry.stemBuffer, ba,
		    sepPos, inflectedBuffer, dictionaryMetadata);
	} else {
	    entry.stemBuffer = BufferUtils.ensureCapacity(entry.stemBuffer, sepPos);
	    entry.stemBuffer.put(ba, 0, sepPos);
	}
	entry.stemBuffer.flip();

	// Skip separator character, if present.
	if (sepPos + 1 <= bbSize) {
	    sepPos++;
	}

	/*
	 * Decode the tag data.
	 */
	entry.tagBuffer = BufferUtils.ensureCapacity(entry.tagBuffer, bbSize
		- sepPos);
	entry.tagBuffer.clear();
	entry.tagBuffer.put(ba, sepPos, bbSize - sepPos);
	entry.tagBuffer.flip();

	return entry;
    }

    /**
     * Decode the byte buffer, optionally expanding the char buffer.
     */
    private CharBuffer bytesToChars(ByteBuffer bytes, CharBuffer chars) {
	chars.clear();
	final int maxCapacity = (int) (bytes.remaining() * decoder.maxCharsPerByte());
	if (chars.capacity() <= maxCapacity) {
	    chars = CharBuffer.allocate(maxCapacity);
	}

	bytes.mark();
	decoder.reset();
	decoder.decode(bytes, chars, true);
	chars.flip();
	bytes.reset();

	return chars;
    }
    
    public void remove() {
	throw new UnsupportedOperationException();
    }
}
