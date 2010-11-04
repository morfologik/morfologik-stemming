package morfologik.tools;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;

import morfologik.tools.MorphEncoder;

import org.junit.Before;
import org.junit.Test;

import static morfologik.tools.MorphEncoder.*;

/*
 * 
 */
public class MorphEncoderTest {
	private MorphEncoder encoder; 

	@Before
	public void setUp() {
		encoder = new MorphEncoder();
	}
	
	@Test
	public void testCommonPrefix() {
		assertEquals(3, commonPrefix("abc".getBytes(), "abcd".getBytes()));
		assertEquals(0, commonPrefix("abc".getBytes(), "cba".getBytes()));
	}

	@Test
	public void testStandardEncode() throws UnsupportedEncodingException {
		assertEquals("abc+Ad+tag", 
				asString(encoder.standardEncode(
						"abc".getBytes("UTF-8"), 
						"abcd".getBytes("UTF-8"), 
						"tag".getBytes("UTF-8")), "UTF-8"));
		
		assertEquals("abc+Dxyz+tag", encoder.standardEncodeUTF8("abc", "xyz", "tag"));
		assertEquals("abc+Bć+tag", encoder.standardEncodeUTF8("abc", "abć", "tag"));	
	}

	@Test
	public void testSeparatorChange() throws UnsupportedEncodingException {
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

		assertEquals("abc+ADxyz+tag", 
				encoder.prefixEncodeUTF8("abc", "xyz", "tag"));
		assertEquals("abc+ABć+tag", 
				encoder.prefixEncodeUTF8("abc", "abć", "tag"));
		assertEquals("postmodernizm+AAu+xyz", 
				encoder.prefixEncodeUTF8("postmodernizm", "postmodernizmu", "xyz"));
		assertEquals("postmodernizmu+AB+xyz", 
				encoder.prefixEncodeUTF8("postmodernizmu", "postmodernizm", "xyz"));
		assertEquals("nieduży+DA+adj", 
				encoder.prefixEncodeUTF8("nieduży", "duży", "adj"));
		assertEquals("postmodernizm+ANmodernizm+xyz", 
				encoder.prefixEncodeUTF8("postmodernizm", "modernizm", "xyz"));
	}

	@Test
	public void testInfixEncode() throws UnsupportedEncodingException {
		assertEquals("abc+AAAd+tag", encoder.infixEncodeUTF8("abc", "abcd", "tag"));
		assertEquals("abcd+AAB+tag", encoder.infixEncodeUTF8("abcd", "abc", "tag"));
		assertEquals("abc+AADxyz+tag", encoder.infixEncodeUTF8("abc", "xyz", "tag"));
		assertEquals("abc+AABć+tag", encoder.infixEncodeUTF8("abc", "abć", "tag"));
		assertEquals("postmodernizm+AAAu+xyz", 
				encoder.infixEncodeUTF8("postmodernizm", "postmodernizmu", "xyz"));
		assertEquals("postmodernizmu+AAB+xyz", 
				encoder.infixEncodeUTF8("postmodernizmu", "postmodernizm", "xyz"));
		assertEquals("nieduży+ADA+adj", 
				encoder.infixEncodeUTF8("nieduży", "duży", "adj"));

		// real infix cases
		assertEquals("kcal+ABA+xyz", encoder.infixEncodeUTF8("kcal", "cal", "xyz"));
		assertEquals("aillent+BBCr+xyz", encoder.infixEncodeUTF8("aillent", "aller", "xyz"));
		assertEquals("laquelle+AAHequel+D f s", encoder.infixEncodeUTF8("laquelle", "lequel", "D f s"));
		assertEquals("ccal+ABA+test", encoder.infixEncodeUTF8("ccal", "cal", "test"));
	}

	@Test
	public void testUTF8Boundary() throws UnsupportedEncodingException {
		assertEquals("passagère+Eer+tag", encoder.standardEncodeUTF8("passagère", "passager", "tag"));
		assertEquals("passagère+AAEer+tag", encoder.infixEncodeUTF8("passagère", "passager", "tag"));
		assertEquals("passagère+AEer+tag", encoder.prefixEncodeUTF8("passagère", "passager", "tag"));
	}

	@Test
	public void testAsString() throws UnsupportedEncodingException {
		assertEquals("passagère", asString("passagère".getBytes("UTF-8"), "UTF-8"));
	}
}
