package morfologik.tools;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import morfologik.fsa.FSA;
import morfologik.fsa.builders.FSABuilder;
import morfologik.fsa.builders.FSASerializer;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

/**
 * Build finite state automaton out of text input.
 */
@Parameters(
    commandNames = {"fsa_compile"},
    commandDescription = "Builds finite state automaton from \\n-delimited input.")
public class FSACompile extends CliTool {
  @Parameter(
      names = {"-i", "--input"},
      description = "The input sequences (one sequence per \\n-delimited line).", 
      required = true,
      validateValueWith = ValidateFileExists.class)
  private Path input;  

  @Parameter(
      names = {"-o", "--output"},
      description = "The output automaton file.", 
      required = true,
      validateValueWith = ValidateParentDirExists.class)
  private Path output;  

  @Parameter(
      names = {"-f", "--format"},
      description = "Automaton serialization format.")
  private SerializationFormat format = SerializationFormat.FSA5;  

  @ParametersDelegate
  private final BinaryInput binaryInput;

  FSACompile() {
    binaryInput = new BinaryInput();
  }

  public FSACompile(Path input,
                  Path output,
                  SerializationFormat format,
                  boolean acceptBom,
                  boolean acceptCr,
                  boolean ignoreEmpty) {
    this.input = checkNotNull(input);
    this.output = checkNotNull(output);
    this.binaryInput = new BinaryInput(acceptBom, acceptCr, ignoreEmpty);
  }

  @Override
  public ExitStatus call() throws Exception {
    final List<byte[]> sequences = binaryInput.readBinarySequences(input, (byte) '\n');

    Collections.sort(sequences, FSABuilder.LEXICAL_ORDERING);
    FSA fsa = FSABuilder.build(sequences);

    FSASerializer serializer = format.getSerializer();
    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(output))) {
      serializer.serialize(fsa, os);
    }

    return ExitStatus.SUCCESS;
  }

  public static void main(String[] args) {
    main(args, new FSACompile());
  }
}
