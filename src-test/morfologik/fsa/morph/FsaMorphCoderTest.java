package morfologik.fsa.morph;

import static org.junit.Assert.*;

import org.junit.Test;

public class FsaMorphCoderTest {

	@Test
	public void testCommonPrefix() {		
		assertEquals(3, FsaMorphCoder.commonPrefix("abc", "abcd"));
		assertEquals(0, FsaMorphCoder.commonPrefix("abc", "cba"));
	}			

	@Test
	public void testStandardEncode() {
		assertEquals("abc+Ad+tag", FsaMorphCoder.standardEncode("abc", "abcd", "tag"));
		assertEquals("abc+Dxyz+tag", FsaMorphCoder.standardEncode("abc", "xyz", "tag"));
		assertEquals("abc+Bć+tag", FsaMorphCoder.standardEncode("abc", "abć", "tag"));
	}

	@Test
	public void testPrefixEncode() {
		assertEquals("abc+AAd+tag", FsaMorphCoder.prefixEncode("abc", "abcd", "tag"));
		assertEquals("abcd+AB+tag", FsaMorphCoder.prefixEncode("abcd", "abc", "tag"));
		assertEquals("abc+ADxyz+tag", FsaMorphCoder.prefixEncode("abc", "xyz", "tag"));
		assertEquals("abc+ABć+tag", FsaMorphCoder.prefixEncode("abc", "abć", "tag"));
		assertEquals("postmodernizm+AAu+xyz", FsaMorphCoder.prefixEncode("postmodernizm", "postmodernizmu", "xyz"));
		assertEquals("postmodernizmu+AB+xyz", FsaMorphCoder.prefixEncode("postmodernizmu", "postmodernizm", "xyz"));
		assertEquals("nieduży+DA+adj", FsaMorphCoder.prefixEncode("nieduży", "duży", "adj"));
		assertEquals("postmodernizm+ANmodernizm+xyz", FsaMorphCoder.prefixEncode("postmodernizm", "modernizm", "xyz"));
	}

	@Test
	public void testInfixEncode() {
		assertEquals("abc+AAAd+tag", FsaMorphCoder.infixEncode("abc", "abcd", "tag"));
		assertEquals("abcd+AAB+tag", FsaMorphCoder.infixEncode("abcd", "abc", "tag"));
		assertEquals("abc+AADxyz+tag", FsaMorphCoder.infixEncode("abc", "xyz", "tag"));
		assertEquals("abc+AABć+tag", FsaMorphCoder.infixEncode("abc", "abć", "tag"));
		assertEquals("postmodernizm+AAAu+xyz", FsaMorphCoder.infixEncode("postmodernizm", "postmodernizmu", "xyz"));
		assertEquals("postmodernizmu+AAB+xyz", FsaMorphCoder.infixEncode("postmodernizmu", "postmodernizm", "xyz"));
		assertEquals("nieduży+ADA+adj", FsaMorphCoder.infixEncode("nieduży", "duży", "adj"));
		//real infix cases
		assertEquals("kcal+ABA+xyz", FsaMorphCoder.infixEncode("kcal", "cal", "xyz"));
		assertEquals("aillent+BBCr+xyz", FsaMorphCoder.infixEncode("aillent", "aller", "xyz"));
		assertEquals("laquelle+AAHequel+D f s", FsaMorphCoder.infixEncode("laquelle", "lequel", "D f s"));
		assertEquals("ccal+ABA+test", FsaMorphCoder.infixEncode("ccal", "cal", "test"));
	}

	
}
