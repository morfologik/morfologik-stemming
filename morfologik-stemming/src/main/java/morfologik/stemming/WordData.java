package morfologik.stemming;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

/**
 * Stem and tag data associated with a given word.
 * 
 * Instances of this class are reused and mutable (values
 * returned from {@link #getStem()}, {@link #getWord()}
 * and other related methods change on subsequent calls to 
 * {@link DictionaryLookup} class that returned a given
 * instance of {@link WordData}.
 * 
 * If you need a copy of the
 * stem or tag data for a given word, you have to create a custom buffer
 * yourself and copy the associated data, perform {@link #clone()} or create
 * strings (they are immutable) using {@link #getStem()} and then
 * {@link CharSequence#toString()}.
 * 
 * For reasons above it makes no sense to use instances
 * of this class in associative containers or lists. In fact,
 * both {@link #equals(Object)} and {@link #hashCode()} are overridden and throw 
 * exceptions to prevent accidental damage.
 */
public final class WordData implements Cloneable {
	/**
	 * Error information if somebody puts us in a Java collection.
	 */
	private static final String COLLECTIONS_ERROR_MESSAGE = "Not suitable for use"
	        + " in Java collections framework (volatile content). Refer to documentation.";

	/** Character encoding in internal buffers. */
	private final CharsetDecoder decoder;

	/**
	 * Inflected word form data.
	 */
	private CharSequence wordCharSequence;

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

	/** Byte buffer holding the inflected word form data. */
	ByteBuffer wordBuffer;

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
	 * buffer.
	 *
	 * The buffer is cleared prior to copying and flipped for reading
	 * upon returning from this method. If the buffer is null or not large 
	 * enough to hold the result, a new buffer is allocated.
	 * 
	 * @param target
	 *            Target byte buffer to copy the stem buffer to or
	 *            <code>null</code> if a new buffer should be allocated.
	 * 
	 * @return Returns <code>target</code> or the new reallocated buffer.
	 */
	public ByteBuffer getStemBytes(ByteBuffer target) {
		target = BufferUtils.clearAndEnsureCapacity(target, stemBuffer.remaining());
		stemBuffer.mark();
		target.put(stemBuffer);
		stemBuffer.reset();
		target.flip();
		return target;
	}

	/**
	 * Copy the tag's binary data (no charset decoding) to a custom byte buffer.
	 * 
   * The buffer is cleared prior to copying and flipped for reading
   * upon returning from this method. If the buffer is null or not large 
   * enough to hold the result, a new buffer is allocated.
	 * 
	 * @param target
	 *            Target byte buffer to copy the tag buffer to or
	 *            <code>null</code> if a new buffer should be allocated.
	 * 
	 * @return Returns <code>target</code> or the new reallocated buffer.
	 */
	public ByteBuffer getTagBytes(ByteBuffer target) {
		target = BufferUtils.clearAndEnsureCapacity(target, tagBuffer.remaining());
		tagBuffer.mark();
		target.put(tagBuffer);
		tagBuffer.reset();
		target.flip();
		return target;
	}

	/**
	 * Copy the inflected word's binary data (no charset decoding) to a custom
	 * byte buffer.
	 * 
   * The buffer is cleared prior to copying and flipped for reading
   * upon returning from this method. If the buffer is null or not large 
   * enough to hold the result, a new buffer is allocated. 
	 * 
	 * @param target
	 *            Target byte buffer to copy the word buffer to or
	 *            <code>null</code> if a new buffer should be allocated.
	 * 
	 * @return Returns <code>target</code> or the new reallocated buffer.
	 */
	public ByteBuffer getWordBytes(ByteBuffer target) {
		target = BufferUtils.clearAndEnsureCapacity(target, wordBuffer.remaining());
		wordBuffer.mark();
		target.put(wordBuffer);
		wordBuffer.reset();
		target.flip();
		return target;
	}

	/**
	 * @return Return tag data decoded to a character sequence or
	 *         <code>null</code> if no associated tag data exists.
	 */
	public CharSequence getTag() {
		tagCharSequence = BufferUtils.bytesToChars(decoder, tagBuffer, tagCharSequence);
		return tagCharSequence.remaining() == 0 ? null : tagCharSequence;
	}

	/**
	 * @return Return stem data decoded to a character sequence or
	 *         <code>null</code> if no associated stem data exists.
	 */
	public CharSequence getStem() {
		stemCharSequence = BufferUtils.bytesToChars(decoder, stemBuffer, stemCharSequence);
		return stemCharSequence.remaining() == 0 ? null : stemCharSequence;
	}

	/**
	 * @return Return inflected word form data. Usually the parameter passed to
	 *         {@link DictionaryLookup#lookup(CharSequence)}.
	 */
	public CharSequence getWord() {
		return wordCharSequence;
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

	@Override
	public String toString() {
	    return "WordData["
	        + this.getWord() + ","
	        + this.getStem() + ","
	        + this.getTag() + "]";
	}
	
	/**
	 * Declare a covariant of {@link Object#clone()} that returns a deep copy of
	 * this object. The content of all internal buffers is copied.
	 */
	@Override
	public WordData clone() {
		final WordData clone = new WordData(this.decoder);
		clone.wordCharSequence = cloneCharSequence(wordCharSequence);
		clone.wordBuffer = getWordBytes(null);
		clone.stemBuffer = getStemBytes(null);
		clone.tagBuffer = getTagBytes(null);
		return clone;
	}

	/**
	 * Clone char sequences only if not immutable.
	 */
	private CharSequence cloneCharSequence(CharSequence chs) {
		if (chs instanceof String)
			return chs;
		return chs.toString();
	}

  void update(ByteBuffer wordBuffer, CharSequence word) {
    this.stemCharSequence.clear();
    this.tagCharSequence.clear();
    this.stemBuffer.clear();
    this.tagBuffer.clear();

    this.wordBuffer = wordBuffer;
    this.wordCharSequence = word;
  }
}
