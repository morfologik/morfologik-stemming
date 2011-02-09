package morfologik.fsa;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 
 */
public class CFSA2SerializerTest extends SerializerTestBase {
    protected CFSA2Serializer createSerializer() {
        return new CFSA2Serializer();
    }

    @Test
    public void testVIntCoding() {
        byte [] scratch = new byte [5];

        int [] values = {0, 1, 128, 256, 0x1000, Integer.MAX_VALUE }; 

        for (int v : values) {
            int len = CFSA2.writeVInt(scratch, 0, v);
            assertEquals(v, CFSA2.readVInt(scratch, 0));
            assertEquals(len, CFSA2.vIntLength(v));
        }
    }
}
