package morfologik.tools;

import static com.google.common.base.Charsets.UTF_8;

import org.junit.Test;

import com.carrotsearch.hppc.ByteArrayList;
import com.carrotsearch.randomizedtesting.RandomizedTest;

import static morfologik.tools.MorphEncoder2.*;

public class MorphEncoder2Test extends RandomizedTest {
    @Test
    public void testEncodeSuffixOnRandomSequences() {
        for (int i = 0; i < 10000; i++) {
            assertEncodeSuffix(
                randomAsciiOfLengthBetween(0, 500),
                randomAsciiOfLengthBetween(0, 500));
        }
    }

    @Test
    public void testEncodeSuffixOnSimpleCases() {
        assertEncodeSuffix("", "");
        assertEncodeSuffix("abc", "ab");
        assertEncodeSuffix("abc", "abx");
        assertEncodeSuffix("ab", "abc");
        assertEncodeSuffix("xabc", "abc");
    }

    private void assertEncodeSuffix(String srcString, String dstString)
    {
        ByteArrayList src = ByteArrayList.from(srcString.getBytes(UTF8));
        ByteArrayList dst = ByteArrayList.from(dstString.getBytes(UTF8));
        ByteArrayList encoded = ByteArrayList.newInstance();
        ByteArrayList decoded = ByteArrayList.newInstance();

        encodeSuffix(src, dst, encoded);
        decodeSuffix(src, encoded, decoded);

        assertEquals(dst, decoded);
    }
    

    public static void main(String [] args) {
        //ByteBuffer src = ByteBuffer.wrap("x123foo45abc".getBytes(UTF_8));
        //ByteBuffer dst = ByteBuffer.wrap("12345def".getBytes(UTF_8));

        ByteArrayList src = ByteArrayList.from("abcd".getBytes(UTF_8));
        ByteArrayList dst = ByteArrayList.from("abcx".getBytes(UTF_8));

        ByteArrayList out = encodeSuffix(src, dst, ByteArrayList.newInstance());
        System.out.println(new String(out.toArray()));
        
        System.out.println(new String(decodeSuffix(src, out, ByteArrayList.newInstance()).toArray()));
    }    
}
