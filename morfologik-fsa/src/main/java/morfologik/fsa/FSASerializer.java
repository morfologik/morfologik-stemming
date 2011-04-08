package morfologik.fsa;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 * All FSA serializers to binary formats will implement this interface.
 */
public interface FSASerializer {
    /**
     * Serialize a finite state automaton to an output stream.
     */
    public <T extends OutputStream> T serialize(FSA fsa, T os) throws IOException;

    /**
     * Returns the set of flags supported by the serializer (and the output automaton).
     */
    public Set<FSAFlags> getFlags();

    /**
     * Log extra messages during construction. 
     */
    public FSASerializer withLogger(IMessageLogger logger);
    
    /**
     * Supports built-in filler separator. Only if {@link #getFlags()} returns 
     * {@link FSAFlags#SEPARATORS}.
     */
    public FSASerializer withFiller(byte filler);

    /**
     * Supports built-in annotation separator. Only if {@link #getFlags()} returns 
     * {@link FSAFlags#SEPARATORS}.
     */
    public FSASerializer withAnnotationSeparator(byte annotationSeparator);

    /**
     * Supports built-in right language count on nodes, speeding up perfect hash counts. 
     * Only if {@link #getFlags()} returns {@link FSAFlags#NUMBERS}.
     */
    public FSASerializer withNumbers();
}
