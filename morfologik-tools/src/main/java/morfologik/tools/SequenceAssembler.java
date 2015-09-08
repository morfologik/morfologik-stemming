package morfologik.tools;

import java.nio.ByteBuffer;

import morfologik.fsa.FSA5;
import morfologik.stemming.ISequenceEncoder;

final class SequenceAssembler {
  private final ByteArray ba = new ByteArray();
  private final byte annotationSeparator;
  private ByteBuffer tmp;

  private final ISequenceEncoder encoder;

  public SequenceAssembler(ISequenceEncoder encoder) {
    this(encoder, FSA5.DEFAULT_ANNOTATION);
  }

  public SequenceAssembler(ISequenceEncoder encoder, byte annotationSeparator) {
    this.annotationSeparator = annotationSeparator;
    this.encoder = encoder;
  }

  byte[] encode(byte[] wordForm, byte[] wordLemma, byte[] wordTag) {
    tmp = encoder.encode(tmp, ByteBuffer.wrap(wordForm), ByteBuffer.wrap(wordLemma));

    ba.clear();

    ba.add(wordForm);
    ba.add(annotationSeparator);
    
    assert tmp.hasArray() && tmp.arrayOffset() == 0 && tmp.position() == 0;
    ba.add(tmp.array(), 0, tmp.remaining());
    ba.add(annotationSeparator);

    if (wordTag != null) {
      ba.add(wordTag);
    }

    return ba.toArray();
  }
}
