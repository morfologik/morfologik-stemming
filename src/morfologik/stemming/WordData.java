package morfologik.stemming;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

import morfologik.util.BufferUtils;

/**
 * Stem and tag data associated with a given word.
 * 
 * <p>
 * <b>Important notes:</b>
 * <ul>
 * <li>Objects of this class are <i>volatile</i> (their content changes on
 * subsequent calls to {@link DictionaryLookup} class. If you need a copy of the
 * stem or tag data for a given word, you have to create a custom buffer
 * yourself and copy the associated data. Alternatively, create Strings (they
 * are immutable) using {@link #getStem()} and then
 * {@link CharSequence#toString()}.</li>
 * <li>Objects of this class must not be used in any Java collections. In fact
 * both equals and hashCode methods are overridden and throw exceptions to
 * prevent accidental damage.</li>
 * </ul>
 */
public final class WordData {
    /**
     * Error information if somebody puts us in a Java collection.
     */
    private static final String COLLECTIONS_ERROR_MESSAGE = "Not suitable for use"
	    + " in Java collections framework (volatile content). Refer to documentation.";

    /** Character encoding in internal buffers. */
    private final CharsetDecoder decoder;

    /**
     * Character sequence after converting {@link #stemBuffer} using
     * {@link #decoder}.
     */
    private CharBuffer stemCharSequence;

    /**
     * Character sequence after converting {@link #tagBuffer} using
     * {@link #decoder}.
     */
    private CharBuffer tagCharSequence;

    /** Byte buffer holding stem data. */
    ByteBuffer stemBuffer;

    /** Byte buffer holding tag data. */
    ByteBuffer tagBuffer;

    /**
     * Package scope constructor.
     */
    WordData(CharsetDecoder decoder) {
	this.decoder = decoder;

	stemBuffer = ByteBuffer.allocate(0);
	tagBuffer = ByteBuffer.allocate(0);
	stemCharSequence = CharBuffer.allocate(0);
	tagCharSequence = CharBuffer.allocate(0);
    }

    /**
     * A constructor for tests only.
     */
    WordData(String stem, String tag, String encoding) {
	this(Charset.forName(encoding).newDecoder());

	try {
	    if (stem != null)
		stemBuffer.put(stem.getBytes(encoding));
	    if (tag != null)
		tagBuffer.put(tag.getBytes(encoding));
	} catch (UnsupportedEncodingException e) {
	    throw new RuntimeException(e);
	}
    }

    /**
     * Copy the stem's binary data (no charset decoding) to a custom byte
     * buffer. If the buffer is null or not large enough to hold the result, a
     * new buffer is allocated.
     * 
     * @param target
     *            Target byte buffer to copy the stem buffer to or
     *            <code>null</code> if a new buffer should be allocated.
     * 
     * @return Returns <code>target</code> or the new reallocated buffer.
     */
    public ByteBuffer getStemBytes(ByteBuffer target) {
	target = BufferUtils.ensureCapacity(target, stemBuffer.remaining());
	target.put(stemBuffer);
	return target;
    }

    /**
     * Copy the tag's binary data (no charset decoding) to a custom byte buffer.
     * If the buffer is null or not large enough to hold the result, a new
     * buffer is allocated.
     * 
     * @param target
     *            Target byte buffer to copy the tag buffer to or
     *            <code>null</code> if a new buffer should be allocated.
     * 
     * @return Returns <code>target</code> or the new reallocated buffer.
     */
    public ByteBuffer getTagBytes(ByteBuffer target) {
	target = BufferUtils.ensureCapacity(target, tagBuffer.remaining());
	target.put(tagBuffer);
	return target;
    }

    /**
     * @return Return tag data decoded to a character sequence or
     *         <code>null</code> if no associated tag data exists.
     */
    public CharSequence getTag() {
	tagCharSequence = decode(tagBuffer, tagCharSequence);
	return tagCharSequence.remaining() == 0 ? null : tagCharSequence;
    }

    /**
     * @return Return stem data decoded to a character sequence or
     *         <code>null</code> if no associated stem data exists.
     */
    public CharSequence getStem() {
	stemCharSequence = decode(stemBuffer, stemCharSequence);
	return stemCharSequence.remaining() == 0 ? null : stemCharSequence;
    }

    /*
     * 
     */
    @Override
    public boolean equals(Object obj) {
	throw new UnsupportedOperationException(COLLECTIONS_ERROR_MESSAGE);
    }

    /*
     * 
     */
    @Override
    public int hashCode() {
	throw new UnsupportedOperationException(COLLECTIONS_ERROR_MESSAGE);
    }

    /**
     * Reset internal structures for storing another word's data.
     */
    void reset() {
	this.stemCharSequence.clear();
	this.tagCharSequence.clear();
	this.stemBuffer.clear();
	this.tagBuffer.clear();
    }

    /**
     * Decode byte buffer, optionally expanding the char buffer to.
     */
    private CharBuffer decode(ByteBuffer bytes, CharBuffer chars) {
	chars.clear();
	final int maxCapacity = (int) (bytes.remaining() * decoder.maxCharsPerByte());
	if (chars.capacity() <= maxCapacity) {
	    chars = CharBuffer.allocate(maxCapacity);
	}

	bytes.mark();
	decoder.decode(bytes, chars, true);
	chars.flip();
	bytes.reset();

	return chars;
    }
}
