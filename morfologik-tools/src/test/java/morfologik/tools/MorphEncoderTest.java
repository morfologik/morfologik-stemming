package morfologik.tools;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;

import org.junit.Test;

/*
 * 
 */
public class MorphEncoderTest {
    private static final String UTF8 = "UTF-8"; 
	
    private MorphEncoder encoder = new MorphEncoder();

	@Test
	public void testCommonPrefix() {
		assertEquals(3, commonPrefix("abc".getBytes(), "abcd".getBytes()));
		assertEquals(0, commonPrefix("abc".getBytes(), "cba".getBytes()));
	}

	private Object commonPrefix(byte [] a, byte [] b)
    {
	    int max = Math.min(a.length, b.length);
	    for (int i = 0; i < max; i++) {
	        if (a[i] != b[i]) return i;
	    }
        return max;
    }

    @Test
	public void testStandardEncode() throws Exception {
		assertEquals("abc+Ad+tag", 
				asString(encoder.standardEncode(
						"abc".getBytes("UTF-8"), 
						"abcd".getBytes("UTF-8"), 
						"tag".getBytes("UTF-8")), "UTF-8"));
		
		assertEquals("abc+Dxyz+tag", standardEncodeUTF8("abc", "xyz", "tag"));
		assertEquals("abc+Bć+tag", standardEncodeUTF8("abc", "abć", "tag"));	
	}

	@Test
	public void testSeparatorChange() throws Exception {
		assertEquals("abc+Ad+tag", 
			asString(encoder.standardEncode(
					"abc".getBytes("UTF-8"), 
					"abcd".getBytes("UTF-8"), 
					"tag".getBytes("UTF-8")), "UTF-8"));

		encoder = new MorphEncoder((byte) '_');
		assertEquals("abc_Ad_tag", 
				asString(encoder.standardEncode(
								"abc".getBytes("UTF-8"), 
								"abcd".getBytes("UTF-8"), 
								"tag".getBytes("UTF-8")), "UTF-8"));
	
		encoder = new MorphEncoder((byte) '\t');
		assertEquals("abc\tAd\ttag", 
				asString(encoder.standardEncode(
						"abc".getBytes("UTF-8"), 
						"abcd".getBytes("UTF-8"), 
						"tag".getBytes("UTF-8")), "UTF-8"));
	}
	
	@Test
	public void testPrefixEncode() throws UnsupportedEncodingException {
		assertEquals("abc+AAd+tag", asString(
				encoder.prefixEncode(
						"abc".getBytes("UTF-8"),
						"abcd".getBytes("UTF-8"), 
						"tag".getBytes("UTF-8")), "UTF-8"));

		assertEquals("abcd+AB+tag", asString(
				encoder.prefixEncode(
						"abcd".getBytes("UTF-8"),
		                "abc".getBytes("UTF-8"), 
		                "tag".getBytes("UTF-8")), "US-ASCII"));

		assertEquals("abc+ADxyz+tag", prefixEncodeUTF8("abc", "xyz", "tag"));
		assertEquals("abc+ABć+tag", prefixEncodeUTF8("abc", "abć", "tag"));
		assertEquals("postmodernizm+AAu+xyz", prefixEncodeUTF8("postmodernizm", "postmodernizmu", "xyz"));
		assertEquals("postmodernizmu+AB+xyz", prefixEncodeUTF8("postmodernizmu", "postmodernizm", "xyz"));
		assertEquals("nieduży+DA+adj", prefixEncodeUTF8("nieduży", "duży", "adj"));
		assertEquals("postmodernizm+EA+xyz", prefixEncodeUTF8("postmodernizm", "modernizm", "xyz"));
	}

	@Test
	public void testInfixEncode() throws UnsupportedEncodingException {
        assertEquals("laquelle+AGAquel+D f s", infixEncodeUTF8("laquelle", "lequel", "D f s"));

		assertEquals("abc+AAAd+tag", infixEncodeUTF8("abc", "abcd", "tag"));
		assertEquals("abcd+AAB+tag", infixEncodeUTF8("abcd", "abc", "tag"));
		assertEquals("abc+AADxyz+tag", infixEncodeUTF8("abc", "xyz", "tag"));
		assertEquals("abc+AABć+tag", infixEncodeUTF8("abc", "abć", "tag"));
		assertEquals("postmodernizm+AAAu+xyz", 
				infixEncodeUTF8("postmodernizm", "postmodernizmu", "xyz"));
		assertEquals("postmodernizmu+AAB+xyz", 
				infixEncodeUTF8("postmodernizmu", "postmodernizm", "xyz"));
		assertEquals("nieduży+ADA+adj", 
				infixEncodeUTF8("nieduży", "duży", "adj"));

		// real infix cases
		assertEquals("kcal+ABA+xyz", infixEncodeUTF8("kcal", "cal", "xyz"));
		assertEquals("aillent+BBCr+xyz", infixEncodeUTF8("aillent", "aller", "xyz"));
		assertEquals("laquelle+AGAquel+D f s", infixEncodeUTF8("laquelle", "lequel", "D f s"));
		assertEquals("ccal+ABA+test", infixEncodeUTF8("ccal", "cal", "test"));
		assertEquals("ccal+ABA+test", infixEncodeUTF8("ccal", "cal", "test"));
	}

	@Test
	public void testUTF8Boundary() throws Exception {
		assertEquals("passagère+Eer+tag", standardEncodeUTF8("passagère", "passager", "tag"));
		assertEquals("passagère+GDAr+tag", infixEncodeUTF8("passagère", "passager", "tag"));
		assertEquals("passagère+AEer+tag", prefixEncodeUTF8("passagère", "passager", "tag"));
	}

	@Test
	public void testAsString() throws UnsupportedEncodingException {
		assertEquals("passagère", asString("passagère".getBytes("UTF-8"), "UTF-8"));
	}

    /**
     * Converts a byte array to a given encoding.
     * 
     * @param str
     *            Byte-array to be converted.
     * @return Java String. If decoding is unsuccessful, the string is empty.
     */
    protected static String asString(final byte[] str, final String encoding) {
        try {
            return new String(str, encoding);
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /**
     * A UTF-8 variant of {@link MorphEncoder#standardEncode(byte[], byte[], byte[])} This
     * method converts the wordForm, wordLemma and tag to the form:
     * 
     * <pre>
     * wordForm + Kending + tags
     * </pre>
     * 
     * where '+' is a separator, K is a character that specifies how many
     * characters should be deleted from the end of the inflected form to
     * produce the lexeme by concatenating the stripped string with the ending.
     * 
     * @throws UnsupportedEncodingException
     */
    String standardEncodeUTF8(final String wordForm,
            final String wordLemma, final String wordTag)
            throws Exception {
        return asString(
            encoder.standardEncode(
                wordForm.getBytes(UTF8), 
                wordLemma.getBytes(UTF8), 
                wordTag.getBytes(UTF8)), UTF8);
    }

    /**
     * A UTF-8 variant of {@link MorphEncoder#prefixEncode(byte[], byte[], byte[])} This
     * method converts wordform, wordLemma and the tag to the form:
     * <pre>
     * inflected_form + LKending + tags
     * </pre>
     * <p>
     * where '+' is a separator, L is the number of characters to be deleted
     * from the beginning of the word ("A" means none, "B" means one, "C" - 2,
     * etc.), K is a character that specifies how many characters should be
     * deleted from the end of the inflected form to produce the lexeme by
     * concatenating the stripped string with the ending ("A" means none,
     * "B' - 1, "C" - 2, and so on).
     * 
     * @param wordForm
     *            - inflected word form
     * @param wordLemma
     *            - canonical form
     * @param wordTag
     *            - tag
     * @return the encoded string
     * @throws UnsupportedEncodingException
     */
    String prefixEncodeUTF8(final String wordForm,
            final String wordLemma, final String wordTag)
            throws UnsupportedEncodingException {
        return asString(
            encoder.prefixEncode(
                wordForm.getBytes(UTF8), 
                wordLemma.getBytes(UTF8), 
                wordTag.getBytes(UTF8)), UTF8);
    }

    /**
     * A UTF-8 variant of {@link MorphEncoder#infixEncode(byte[], byte[], byte[])}.
     * 
     * This method converts wordform, wordLemma and the tag to the form:
     * <pre>
     * inflected_form + MLKending + tags
     * </pre>
     * <p>
     * where '+' is a separator, M is the position of characters to be deleted
     * towards the beginning of the inflected form ("A" means from the
     * beginning, "B" from the second character, "C" - from the third one, and
     * so on), L is the number of characters to be deleted from the position
     * specified by M ("A" means none, "B" means one, "C" - 2, etc.), K is a
     * character that specifies how many characters should be deleted from the
     * end of the inflected form to produce the lexeme by concatenating the
     * stripped string with the ending ("A" means none, "B' - 1, "C" - 2, and so
     * on).
     * 
     * @param wordForm
     *            - inflected word form
     * @param wordLemma
     *            - canonical form
     * @param wordTag
     *            - tag
     * @return the encoded string
     * @throws UnsupportedEncodingException
     */
    String infixEncodeUTF8(final String wordForm,
            final String wordLemma, final String wordTag)
            throws UnsupportedEncodingException {
        return asString(
            encoder.infixEncode(
                wordForm.getBytes(UTF8), 
                wordLemma.getBytes(UTF8), 
                wordTag.getBytes(UTF8)), UTF8);
    }
}
