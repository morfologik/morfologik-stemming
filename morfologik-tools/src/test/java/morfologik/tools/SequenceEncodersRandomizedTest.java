package morfologik.tools;

import java.nio.ByteBuffer;
import java.util.List;

import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.DictionaryMetadataBuilder;
import morfologik.stemming.EncoderType;

import org.junit.Test;

import com.carrotsearch.hppc.ByteArrayList;
import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

public class SequenceEncodersRandomizedTest extends RandomizedTest {
    private final SequenceEncoders.IEncoder coder;

    public SequenceEncodersRandomizedTest(@Name("coder") SequenceEncoders.IEncoder coder)
    {
        this.coder = coder;
    }

    @ParametersFactory
    public static List<Object[]> testFactory() {
        List<Object[]> encoders = Lists.newArrayList();
        for (EncoderType t : EncoderType.values()) {
            encoders.add(new Object [] {SequenceEncoders.forType(t)});
        }
        return encoders;
    }

    @Test
    public void testEncodeSuffixOnRandomSequences() {
        for (int i = 0; i < 10000; i++) {
            assertRoundtripEncode(
                randomAsciiOfLengthBetween(0, 500),
                randomAsciiOfLengthBetween(0, 500));
        }
    }

    @Test
    public void testEncodeSamples() {
        assertRoundtripEncode("", "");
        assertRoundtripEncode("abc", "ab");
        assertRoundtripEncode("abc", "abx");
        assertRoundtripEncode("ab", "abc");
        assertRoundtripEncode("xabc", "abc");
        assertRoundtripEncode("axbc", "abc");
        assertRoundtripEncode("axybc", "abc");
        assertRoundtripEncode("axybc", "abc");
        assertRoundtripEncode("azbc", "abcxy");

        assertRoundtripEncode("Niemcami", "Niemiec");
        assertRoundtripEncode("Niemiec", "Niemcami");
    }

    private void assertRoundtripEncode(String srcString, String dstString)
    {
        ByteArrayList src = ByteArrayList.from(srcString.getBytes(UTF8));
        ByteArrayList dst = ByteArrayList.from(dstString.getBytes(UTF8));
        ByteArrayList encoded = ByteArrayList.newInstance();
        ByteArrayList decoded = ByteArrayList.newInstance();

        coder.encode(src, dst, encoded);
        coder.decode(src, encoded, decoded);

        if (!dst.equals(decoded)) {
            System.out.println("src: " + new String(src.toArray(), Charsets.UTF_8));
            System.out.println("dst: " + new String(dst.toArray(), Charsets.UTF_8));
            System.out.println("enc: " + new String(encoded.toArray(), Charsets.UTF_8));
            System.out.println("dec: " + new String(decoded.toArray(), Charsets.UTF_8));
        }
        
        assertEquals(dst, decoded);
        
        // DictionaryLookup.decodeBaseForm decoding testing
        DictionaryMetadataBuilder builder = new DictionaryMetadataBuilder();
        builder.encoding(Charsets.UTF_8);
        builder.encoder(coder.type());
            
        ByteBuffer bb = DictionaryLookup.decodeBaseForm(
            ByteBuffer.allocate(0),
            encoded.toArray(), 
            encoded.size(), 
            ByteBuffer.wrap(src.toArray()), builder.build());
        
        ByteArrayList decoded2 = ByteArrayList.newInstance();
        bb.flip();
        while (bb.hasRemaining()) decoded2.add(bb.get());

        if (!dst.equals(decoded2)) {
            System.out.println("DictionaryLookup.decodeBaseForm incorrect, coder: " + coder);
            System.out.println("src : " + new String(src.toArray(), Charsets.UTF_8));
            System.out.println("dst : " + new String(dst.toArray(), Charsets.UTF_8));
            System.out.println("enc : " + new String(encoded.toArray(), Charsets.UTF_8));
            System.out.println("dec : " + new String(decoded.toArray(), Charsets.UTF_8));
            System.out.println("dec2: " + new String(decoded2.toArray(), Charsets.UTF_8));
        }

        assertEquals(dst, decoded2);       
    }
}
