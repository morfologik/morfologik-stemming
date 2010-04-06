package morfologik.fsa.morph;

import static org.junit.Assert.*;

import org.junit.Test;

public class FsaMorphCoderTest {

	@Test
	public void testCommonPrefix() {		
		assertEquals(3, FsaMorphCoder.commonPrefix("abc", "abcd", 3));
		assertEquals(0, FsaMorphCoder.commonPrefix("abc", "cba", 3));
	}
	

	@Test
	public void testStandardEncode() {
		assertEquals("abc+Ad+tag", FsaMorphCoder.standardEncode("abc", "abcd", "tag"));
		assertEquals("abc+Dxyz+tag", FsaMorphCoder.standardEncode("abc", "xyz", "tag"));
		assertEquals("abc+Bć+tag", FsaMorphCoder.standardEncode("abc", "abć", "tag"));
	}

}
