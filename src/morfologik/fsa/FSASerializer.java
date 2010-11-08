package morfologik.fsa;

import java.io.IOException;
import java.io.OutputStream;

/**
 * All FSA serializers to binary format will implement this interface.
 */
public interface FSASerializer {
    /**
     * Default filler.
     */
    public final static byte DEFAULT_FILLER = '_';
    
    /**
     * Default annotation separator.
     */
    public final static byte DEFAULT_ANNOTATION = '+';

    /**
     * Serialize with numbers allowing perfect hashing.
     */
    FSASerializer withNumbers();

    /**
     * Serialize to an output stream.
     */
    public <T extends OutputStream> T serialize(State s, T os) throws IOException;
    
    /**
     * Set special filler byte in automaton header, 
     * if applicable (<code>fsa</code> package legacy). 
     */
    public FSASerializer withFiller(byte filler);
    
    /**
     * Set special annotation separator byte in automaton header, 
     * if applicable (<code>fsa</code> package legacy). 
     */
    public FSASerializer withAnnotationSeparator(byte annotationSeparator);
}
