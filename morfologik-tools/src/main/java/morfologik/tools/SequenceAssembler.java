package morfologik.tools;

import morfologik.fsa.FSA5;
import morfologik.tools.SequenceEncoders.IEncoder;

import com.carrotsearch.hppc.ByteArrayList;

final class SequenceAssembler {
	private final byte annotationSeparator;

	private final ByteArrayList src = new ByteArrayList();
	private final ByteArrayList dst = new ByteArrayList();
	private final ByteArrayList tmp = new ByteArrayList();

    private final IEncoder encoder;

	public SequenceAssembler(SequenceEncoders.IEncoder encoder) {
		this(encoder, FSA5.DEFAULT_ANNOTATION);
	}

	public SequenceAssembler(SequenceEncoders.IEncoder encoder, byte annotationSeparator) {
		this.annotationSeparator = annotationSeparator;
		this.encoder = encoder;
	}

    byte [] encode(byte [] wordForm, byte [] wordLemma, byte [] wordTag)
    {
        src.clear(); 
        dst.clear(); 
        tmp.clear();
    
        tmp.add(wordForm);
        tmp.add(annotationSeparator);
        
        src.add(wordForm);
        dst.add(wordLemma);
        encoder.encode(src, dst, tmp);

        tmp.add(annotationSeparator);
        if (wordTag != null) {
    	    tmp.add(wordTag);
        }
    
        return tmp.toArray();
    }
}
