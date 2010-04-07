package morfologik.fsa.morph;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import org.junit.Test;

/*
 * 
 */
public class FSAMorphCoderTest {

	@Test
	public void testCommonPrefix() {		
		assertEquals(3, FSAMorphCoder.commonPrefix("abc".getBytes(), "abcd".getBytes()));
		assertEquals(0, FSAMorphCoder.commonPrefix("abc".getBytes(), "cba".getBytes()));
	}			

	@Test
	public void testStandardEncode() throws UnsupportedEncodingException {
		assertEquals("abc+Ad+tag", FSAMorphCoder.standardEncode("abc".getBytes(), "abcd".getBytes(), "tag".getBytes(), "UTF-8"));
		assertEquals("abc+Dxyz+tag", FSAMorphCoder.standardEncodeUTF8("abc", "xyz", "tag"));
		assertEquals("abc+Bć+tag", FSAMorphCoder.standardEncodeUTF8("abc", "abć", "tag"));
	}

	@Test
	public void testPrefixEncode() throws UnsupportedEncodingException {
		assertEquals("abc+AAd+tag", FSAMorphCoder.prefixEncode("abc".getBytes(), "abcd".getBytes(), "tag".getBytes(), "ISO-8859-1"));
		assertEquals("abcd+AB+tag", FSAMorphCoder.prefixEncode("abcd".getBytes(), "abc".getBytes(), "tag".getBytes(), "ISO-8859-2"));
		assertEquals("abc+ADxyz+tag", FSAMorphCoder.prefixEncode("abc".getBytes(), "xyz".getBytes(), "tag".getBytes(), "UTF-8"));
		assertEquals("abc+ABć+tag", FSAMorphCoder.prefixEncode("abc".getBytes(), "abć".getBytes(), "tag".getBytes(), "UTF-8"));
		assertEquals("postmodernizm+AAu+xyz", FSAMorphCoder.prefixEncode("postmodernizm".getBytes(), "postmodernizmu".getBytes(), "xyz".getBytes(), "US-ASCII"));
		assertEquals("postmodernizmu+AB+xyz", FSAMorphCoder.prefixEncode("postmodernizmu".getBytes(), "postmodernizm".getBytes(), "xyz".getBytes(), "UTF-8"));
		assertEquals("nieduży+DA+adj", FSAMorphCoder.prefixEncodeUTF8("nieduży", "duży", "adj"));
		assertEquals("postmodernizm+ANmodernizm+xyz", FSAMorphCoder.prefixEncodeUTF8("postmodernizm", "modernizm", "xyz"));
	}

	@Test
	public void testInfixEncode() throws UnsupportedEncodingException {
		assertEquals("abc+AAAd+tag", FSAMorphCoder.infixEncodeUTF8("abc", "abcd", "tag"));
		assertEquals("abcd+AAB+tag", FSAMorphCoder.infixEncodeUTF8("abcd", "abc", "tag"));
		assertEquals("abc+AADxyz+tag", FSAMorphCoder.infixEncodeUTF8("abc", "xyz", "tag"));
		assertEquals("abc+AABć+tag", FSAMorphCoder.infixEncodeUTF8("abc", "abć", "tag"));
		assertEquals("postmodernizm+AAAu+xyz", FSAMorphCoder.infixEncodeUTF8("postmodernizm", "postmodernizmu", "xyz"));
		assertEquals("postmodernizmu+AAB+xyz", FSAMorphCoder.infixEncodeUTF8("postmodernizmu", "postmodernizm", "xyz"));
		assertEquals("nieduży+ADA+adj", FSAMorphCoder.infixEncodeUTF8("nieduży", "duży", "adj"));
		//real infix cases
		assertEquals("kcal+ABA+xyz", FSAMorphCoder.infixEncodeUTF8("kcal", "cal", "xyz"));
		assertEquals("aillent+BBCr+xyz", FSAMorphCoder.infixEncodeUTF8("aillent", "aller", "xyz"));
		assertEquals("laquelle+AAHequel+D f s", FSAMorphCoder.infixEncodeUTF8("laquelle", "lequel", "D f s"));
		assertEquals("ccal+ABA+test", FSAMorphCoder.infixEncodeUTF8("ccal", "cal", "test"));
	}

	@Test
	public void testUTF8Boundary() throws UnsupportedEncodingException {
		/*
		 * TODO: for byte-labeled automata you should take into account the encoding
		 * the data will be finally in. UTF8 automata will be smaller than character
		 * automata, so it makes sense to stick to UTF8; perhaps convert FSAMorphCoder to
		 * accept byte[] on input? Alternatively, convert to UTF8 internally and calculate
		 * offsets there? 
		 */
		assertEquals("passagère+Eer+tag", FSAMorphCoder.standardEncodeUTF8("passagère", "passager", "tag"));
		assertEquals("passagère+AAEer+tag", FSAMorphCoder.infixEncodeUTF8("passagère", "passager", "tag"));
		assertEquals("passagère+AEer+tag", FSAMorphCoder.prefixEncodeUTF8("passagère", "passager", "tag"));
	}
	
	@Test
	public void testAsString() {		
		assertEquals("passagère", FSAMorphCoder.asString("passagère".getBytes(), "UTF-8"));		
	}
}
