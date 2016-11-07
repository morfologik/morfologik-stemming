package morfologik.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import morfologik.fsa.FSA;
import morfologik.fsa.builders.FSABuilder;
import morfologik.fsa.builders.FSASerializer;
import morfologik.stemming.BufferUtils;
import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.DictionaryMetadata;
import morfologik.stemming.ISequenceEncoder;
import morfologik.stemming.WordData;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 * Decompiles morphological dictionary automaton back to source state. 
 */
@Parameters(
    commandNames = "dict_compile",
    commandDescription = "Compiles a morphological dictionary automaton.")
public class DictCompile extends CliTool {
  @Parameter(
      names = {"-i", "--input"},
      description = "The input file (base,inflected,tag). An associated metadata (*.info) file must exist.", 
      required = true,
      validateValueWith = ValidateFileExists.class)
  private Path input;

  @Parameter(
      names = ARG_VALIDATE,
      arity = 1,
      description = "Validate input to make sure it makes sense.")
  private boolean validate = true;  

  @Parameter(
      names = {"-f", "--format"},
      description = "Automaton serialization format.")
  private SerializationFormat format = SerializationFormat.FSA5;  

  @Parameter(
      names = ARG_OVERWRITE,
      description = "Overwrite the output file if it exists.")
  private boolean overwrite;  

  @ParametersDelegate
  private final BinaryInput binaryInput;

  DictCompile() {
    binaryInput = new BinaryInput();
  }
  
  public DictCompile(Path input,
                     boolean overwrite,
                     boolean validate,
                     boolean acceptBom,
                     boolean acceptCr,
                     boolean ignoreEmpty) {
    this.input = checkNotNull(input);
    this.overwrite = overwrite;
    this.validate = validate;
    this.binaryInput = new BinaryInput(acceptBom, acceptCr, ignoreEmpty);
  }

  @Override
  public ExitStatus call() throws Exception {
    final Path metadataPath = DictionaryMetadata.getExpectedMetadataLocation(input);

    if (!Files.isRegularFile(metadataPath)) {
      System.err.println("Dictionary metadata file for the input does not exist: " + metadataPath);
      System.err.println("The metadata file (with at least the column separator and byte encoding) "
          + "is required. Check out the examples."); 
      return ExitStatus.ERROR_OTHER;
    }

    final Path output = metadataPath.resolveSibling(
        metadataPath.getFileName().toString().replaceAll(
            "\\." + DictionaryMetadata.METADATA_FILE_EXTENSION + "$", ".dict"));

    if (!overwrite && Files.exists(output)) {
      throw new ExitStatusException(ExitStatus.ERROR_CONFIRMATION_REQUIRED, 
          "Output dictionary file already exists: %s, use %s to override.", output, ARG_OVERWRITE);
    }
    
    final DictionaryMetadata metadata;
    try (InputStream is = new BufferedInputStream(Files.newInputStream(metadataPath))) {
      metadata = DictionaryMetadata.read(is);
    }

    final List<byte[]> sequences = binaryInput.readBinarySequences(input, (byte) '\n');

    final CharsetDecoder charsetDecoder = metadata.getDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT);

    final byte separator = metadata.getSeparator();
    final ISequenceEncoder sequenceEncoder = metadata.getSequenceEncoderType().get();

    if (!sequences.isEmpty()) {
      Iterator<byte[]> i = sequences.iterator();
      byte [] row = i.next();
      final int separatorCount = countOf(separator, row);

      if (separatorCount < 1 || separatorCount > 2) {
        throw new ExitStatusException(ExitStatus.ERROR_OTHER, 
            "Invalid input. Each row must consist of [base,inflected,tag?] columns, where ',' is a "
            + "separator character (declared as: %s). This row contains %d separator characters: %s",
              Character.isJavaIdentifierPart(metadata.getSeparatorAsChar()) 
                ? "'" + Character.toString(metadata.getSeparatorAsChar()) + "'"
                : "0x" + Integer.toHexString((int) separator & 0xff),
              separatorCount,
              new String(row, charsetDecoder.charset()));
      }

      while (i.hasNext()) {
        row = i.next();
        int count = countOf(separator, row);
        if (count != separatorCount) {
          throw new ExitStatusException(ExitStatus.ERROR_OTHER,
              "The number of separators (%d) is inconsistent with previous lines: %s",
              count,
              new String(row, charsetDecoder.charset()));
        }
      }
    }

    ByteBuffer encoded = ByteBuffer.allocate(0);
    ByteBuffer source = ByteBuffer.allocate(0);
    ByteBuffer target = ByteBuffer.allocate(0);
    ByteBuffer tag = ByteBuffer.allocate(0);
    ByteBuffer assembled = ByteBuffer.allocate(0);
    for (int i = 0, max = sequences.size(); i < max; i++) {
      byte[] row = sequences.get(i);
      int sep1 = indexOf(separator, row, 0);
      int sep2 = indexOf(separator, row, sep1 + 1);
      if (sep2 < 0) {
        sep2 = row.length;
      }

      source = BufferUtils.clearAndEnsureCapacity(source, sep1);
      source.put(row, 0, sep1);
      source.flip();

      final int len = sep2 - (sep1 + 1);
      target = BufferUtils.clearAndEnsureCapacity(target, len);
      target.put(row, sep1 + 1, len);
      target.flip();

      final int len2 = row.length - (sep2 + 1);
      tag = BufferUtils.clearAndEnsureCapacity(tag, len2);
      if (len2 > 0) {
        tag.put(row, sep2 + 1, len2);
      }
      tag.flip();

      encoded = sequenceEncoder.encode(encoded, target, source);

      assembled = BufferUtils.clearAndEnsureCapacity(assembled, 
          target.remaining() + 1 +
          encoded.remaining() + 1 + 
          tag.remaining());

      assembled.put(target);
      assembled.put(separator);
      assembled.put(encoded);
      if (tag.hasRemaining()) {
        assembled.put(separator);
        assembled.put(tag);
      }
      assembled.flip();

      sequences.set(i, BufferUtils.toArray(assembled));
    }

    Collections.sort(sequences, FSABuilder.LEXICAL_ORDERING);
    FSA fsa = FSABuilder.build(sequences);

    FSASerializer serializer = format.getSerializer();
    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(output))) {
      serializer.serialize(fsa, os);
    }

    // If validating, try to scan the input
    if (validate) {
      for (WordData wd : new DictionaryLookup(new Dictionary(fsa, metadata))) {
        // Do nothing, just scan and make sure no exceptions are thrown.
      }
    }
    
    return ExitStatus.SUCCESS;
  }

  private static int countOf(byte separator, byte[] row) {
    int cnt = 0;
    for (int i = row.length; --i >= 0;) {
      if (row[i] == separator) {
        cnt++;
      }
    }
    return cnt;
  }

  private static int indexOf(byte separator, byte[] row, int fromIndex) {
    while (fromIndex < row.length) {
      if (row[fromIndex] == separator) {
        return fromIndex;
      }
      fromIndex++;
    }
    return -1;
  }

  public static void main(String[] args) {
    main(args, new DictCompile());
  }
}
