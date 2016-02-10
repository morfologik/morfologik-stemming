package morfologik.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import morfologik.fsa.FSA;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Dump all byte sequences encoded in a finite state automaton.
 */
@Parameters(
    commandNames = "fsa_decompile",
    commandDescription = "Dumps all sequences encoded in an automaton.")
public class FSADecompile extends CliTool {
  @Parameter(
      names = {"-i", "--input"},
      description = "The input automaton.", 
      required = true,
      validateValueWith = ValidateFileExists.class)
  private Path input;  

  @Parameter(
      names = {"-o", "--output"},
      description = "The output file for byte sequences.", 
      required = true,
      validateValueWith = ValidateParentDirExists.class)
  private Path output;  

  FSADecompile() {
  }
  
  public FSADecompile(Path input,
                 Path output) {
    this.input = checkNotNull(input);
    this.output = checkNotNull(output);
  }

  @Override
  public ExitStatus call() throws Exception {
    final FSA fsa;
    try (InputStream is = new BufferedInputStream(Files.newInputStream(input))) {
      fsa = FSA.read(is);
    }

    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(output))) {
      for (ByteBuffer bb : fsa) {
        int o = bb.arrayOffset();
        os.write(bb.array(), o + bb.position(), o + bb.remaining());
        os.write('\n');
      }
    }

    return ExitStatus.SUCCESS;
  }

  public static void main(String[] args) {
    main(args, new FSADecompile());
  }
}
