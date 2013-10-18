package morfologik.tools;

import static com.google.common.base.Charsets.UTF_8;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.carrotsearch.hppc.ByteArrayList;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;

public class MorphEncoder2Test extends RandomizedTest {
    
    private final MorphEncoder2.IEncoder coder;

    public MorphEncoder2Test(@Name("coder") MorphEncoder2.IEncoder coder)
    {
        this.coder = coder;
    }

    @ParametersFactory
    public static List<Object[]> testFactory() {
        return Arrays.asList($$($(new MorphEncoder2.TrimSuffixEncoder())));
    }

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

        coder.encode(src, dst, encoded);
        coder.decode(src, encoded, decoded);

        assertEquals(dst, decoded);
    }

    public static void main(String [] args) {
        //ByteBuffer src = ByteBuffer.wrap("x123foo45abc".getBytes(UTF_8));
        //ByteBuffer dst = ByteBuffer.wrap("12345def".getBytes(UTF_8));
        ByteArrayList src = ByteArrayList.from("0123123456789".getBytes(UTF_8));
        ByteArrayList dst = ByteArrayList.from("123456789X".getBytes(UTF_8));

        MorphEncoder2.IEncoder coder = new MorphEncoder2.TrimPrefixAndSuffixEncoder();
        ByteArrayList out = coder.encode(src, dst, ByteArrayList.newInstance());
        System.out.println(new String(out.toArray()));
        System.out.println(new String(coder.decode(src, out, ByteArrayList.newInstance()).toArray()));
    }    
}
