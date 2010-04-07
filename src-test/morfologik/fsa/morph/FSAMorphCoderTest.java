package morfologik.fsa.morph;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

/*
 * 
 */
public class FSAMorphCoderTest {

	@Test
	public void testCommonPrefix() {		
		assertEquals(3, FSAMorphCoder.commonPrefix("abc", "abcd"));
		assertEquals(0, FSAMorphCoder.commonPrefix("abc", "cba"));
	}			

	@Test
	public void testStandardEncode() {
		assertEquals("abc+Ad+tag", FSAMorphCoder.standardEncode("abc", "abcd", "tag"));
		assertEquals("abc+Dxyz+tag", FSAMorphCoder.standardEncode("abc", "xyz", "tag"));
		assertEquals("abc+Bć+tag", FSAMorphCoder.standardEncode("abc", "abć", "tag"));
	}

	@Test
	public void testPrefixEncode() {
		assertEquals("abc+AAd+tag", FSAMorphCoder.prefixEncode("abc", "abcd", "tag"));
		assertEquals("abcd+AB+tag", FSAMorphCoder.prefixEncode("abcd", "abc", "tag"));
		assertEquals("abc+ADxyz+tag", FSAMorphCoder.prefixEncode("abc", "xyz", "tag"));
		assertEquals("abc+ABć+tag", FSAMorphCoder.prefixEncode("abc", "abć", "tag"));
		assertEquals("postmodernizm+AAu+xyz", FSAMorphCoder.prefixEncode("postmodernizm", "postmodernizmu", "xyz"));
		assertEquals("postmodernizmu+AB+xyz", FSAMorphCoder.prefixEncode("postmodernizmu", "postmodernizm", "xyz"));
		assertEquals("nieduży+DA+adj", FSAMorphCoder.prefixEncode("nieduży", "duży", "adj"));
		assertEquals("postmodernizm+ANmodernizm+xyz", FSAMorphCoder.prefixEncode("postmodernizm", "modernizm", "xyz"));
	}

	@Test
	public void testInfixEncode() {
		assertEquals("abc+AAAd+tag", FSAMorphCoder.infixEncode("abc", "abcd", "tag"));
		assertEquals("abcd+AAB+tag", FSAMorphCoder.infixEncode("abcd", "abc", "tag"));
		assertEquals("abc+AADxyz+tag", FSAMorphCoder.infixEncode("abc", "xyz", "tag"));
		assertEquals("abc+AABć+tag", FSAMorphCoder.infixEncode("abc", "abć", "tag"));
		assertEquals("postmodernizm+AAAu+xyz", FSAMorphCoder.infixEncode("postmodernizm", "postmodernizmu", "xyz"));
		assertEquals("postmodernizmu+AAB+xyz", FSAMorphCoder.infixEncode("postmodernizmu", "postmodernizm", "xyz"));
		assertEquals("nieduży+ADA+adj", FSAMorphCoder.infixEncode("nieduży", "duży", "adj"));
		//real infix cases
		assertEquals("kcal+ABA+xyz", FSAMorphCoder.infixEncode("kcal", "cal", "xyz"));
		assertEquals("aillent+BBCr+xyz", FSAMorphCoder.infixEncode("aillent", "aller", "xyz"));
		assertEquals("laquelle+AAHequel+D f s", FSAMorphCoder.infixEncode("laquelle", "lequel", "D f s"));
		assertEquals("ccal+ABA+test", FSAMorphCoder.infixEncode("ccal", "cal", "test"));
	}

	@Test
	public void testUTF8Boundary() {
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
	public void testAsByteWideString() {
		assertEquals("passagÃ¨re", FSAMorphCoder.asByteWideString("passagère"));
		assertEquals(FSAMorphCoder.asNormalString("passagère"), FSAMorphCoder.asByteWideString("passagère"));
	}
}
