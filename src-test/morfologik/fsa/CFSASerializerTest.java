package morfologik.fsa;

import static org.junit.Assert.assertTrue;

import java.io.*;

import org.junit.Test;

/*
 * 
 */
public class CFSASerializerTest extends SerializerTestBase {
    /*
     * 
     */
    @Test
    public void testBinaryIsCFSA() throws IOException {
        byte[][] input = new byte[][] {};
        State s = FSABuilder.build(input);

        FSA fsa = FSA.read(new ByteArrayInputStream(
                createSerializer().serialize(s, new ByteArrayOutputStream()).toByteArray()));
        
        assertTrue(fsa instanceof CFSA);
    }
   
    /*
     * 
     */
	protected FSASerializer createSerializer() {
        return new CFSASerializer();
    }
}
