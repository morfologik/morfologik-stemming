package morfologik.tools;

import morfologik.fsa.builders.CFSA2Serializer;
import morfologik.fsa.builders.FSA5Serializer;
import morfologik.fsa.builders.FSASerializer;

/**
 * The serialization and encoding format to use for compressing the
 * automaton.
 */
public enum SerializationFormat {
  FSA5 {
    @Override
    FSASerializer getSerializer() {
      return new FSA5Serializer();
    }
  },

  CFSA2 {
    @Override
    CFSA2Serializer getSerializer() {
      return new CFSA2Serializer();
    }      
  };

  abstract FSASerializer getSerializer();
}