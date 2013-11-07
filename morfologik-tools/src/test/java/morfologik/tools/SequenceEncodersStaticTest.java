package morfologik.tools;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import morfologik.stemming.EncoderType;

import org.junit.Test;

import com.google.common.base.Charsets;

/*
 * 
 */
public class SequenceEncodersStaticTest {
    private SequenceAssembler suffix = new SequenceAssembler(new SequenceEncoders.TrimSuffixEncoder());
    private SequenceAssembler prefix = new SequenceAssembler(new SequenceEncoders.TrimPrefixAndSuffixEncoder());
    private SequenceAssembler infix = new SequenceAssembler(new SequenceEncoders.TrimInfixAndSuffixEncoder());

    @Test
	public void testStandardEncode() throws Exception {
		assertEquals("abc+Ad+tag", encode(suffix, "abc", "abcd", "tag"));
		assertEquals("abc+Dxyz+tag", encode(suffix, "abc", "xyz", "tag"));
		assertEquals("abc+Bć+tag", encode(suffix, "abc", "abć", "tag"));	
	}

    @Test
	public void testSeparatorChange() throws Exception {
		assertEquals("abc+Ad+tag", encode(suffix, "abc", "abcd", "tag")); 

		SequenceAssembler assembler = new SequenceAssembler(new SequenceEncoders.TrimSuffixEncoder(), (byte) '_');
		assertEquals("abc_Ad_tag", encode(assembler, "abc", "abcd", "tag"));

		assembler = new SequenceAssembler(new SequenceEncoders.TrimSuffixEncoder(), (byte) '\t');
        assertEquals("abc\tAd\ttag", encode(assembler, "abc", "abcd", "tag"));
	}
	
	@Test
	public void testPrefixEncode() throws UnsupportedEncodingException {
        assertEquals("abc+AAd+tag", encode(prefix, "abc", "abcd", "tag"));
        assertEquals("abcd+AB+tag", encode(prefix, "abcd", "abc", "tag"));
		assertEquals("abc+ADxyz+tag", encode(prefix, "abc", "xyz", "tag"));
		assertEquals("abc+ABć+tag", encode(prefix, "abc", "abć", "tag"));
		assertEquals("postmodernizm+AAu+xyz", encode(prefix, "postmodernizm", "postmodernizmu", "xyz"));
		assertEquals("postmodernizmu+AB+xyz", encode(prefix, "postmodernizmu", "postmodernizm", "xyz"));
		assertEquals("nieduży+DA+adj", encode(prefix, "nieduży", "duży", "adj"));
		assertEquals("postmodernizm+EA+xyz", encode(prefix, "postmodernizm", "modernizm", "xyz"));
	}

	@Test
	public void testInfixEncode() throws UnsupportedEncodingException {
        assertEquals("ayz+AACbc+tag", encode(infix, "ayz", "abc", "tag"));
	    assertEquals("xyz+AADabc+tag", encode(infix, "xyz", "abc", "tag"));

		assertEquals("abc+AAAd+tag", encode(infix, "abc", "abcd", "tag"));
		assertEquals("abcd+AAB+tag", encode(infix, "abcd", "abc", "tag"));
		assertEquals("abc+AADxyz+tag", encode(infix, "abc", "xyz", "tag"));
		assertEquals("abc+AABć+tag", encode(infix, "abc", "abć", "tag"));
		assertEquals("postmodernizm+AAAu+xyz", encode(infix, "postmodernizm", "postmodernizmu", "xyz"));
		assertEquals("postmodernizmu+AAB+xyz", encode(infix, "postmodernizmu", "postmodernizm", "xyz"));
		assertEquals("nieduży+ADA+adj", encode(infix, "nieduży", "duży", "adj"));

		// real infix cases
		assertEquals("kcal+ABA+xyz", encode(infix, "kcal", "cal", "xyz"));
		assertEquals("aillent+BBCr+xyz", encode(infix, "aillent", "aller", "xyz"));
        assertEquals("laquelle+AGAquel+D f s", encode(infix, "laquelle", "lequel", "D f s"));
		assertEquals("ccal+ABA+test", encode(infix, "ccal", "cal", "test"));
		assertEquals("ccal+ABA+test", encode(infix, "ccal", "cal", "test"));
	}

	@Test
	public void testUTF8Boundary() throws Exception {
		assertEquals("passagère+Eer+tag", encode(suffix, "passagère", "passager", "tag"));
		assertEquals("passagère+GDAr+tag", encode(infix, "passagère", "passager", "tag"));
		assertEquals("passagère+AEer+tag", encode(prefix, "passagère", "passager", "tag"));
	}
	
	@Test
	public void testAllEncodersHaveImplementations() {
	    for (EncoderType t : EncoderType.values()) {
	        assertNotNull(null != SequenceEncoders.forType(t));
	    }
	}

    private String encode(SequenceAssembler assembler, String wordForm,
        String wordLemma, String wordTag)
    {
        Charset UTF8 = Charsets.UTF_8;
        return new String(assembler.encode(
            wordForm.getBytes(UTF8), 
            wordLemma.getBytes(UTF8), 
            wordTag.getBytes(UTF8)), UTF8);
    }
}
