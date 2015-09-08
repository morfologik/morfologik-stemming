package morfologik.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import morfologik.fsa.FSA;
import morfologik.fsa.builders.CFSA2Serializer;
import morfologik.fsa.builders.FSA5Serializer;
import morfologik.fsa.builders.FSABuilder;
import morfologik.fsa.builders.FSASerializer;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Build finite state automaton out of text input.
 */
@Parameters(
    commandNames = "fsa_build",
    commandDescription = "Builds finite state automaton from \\n-delimited input.")
public class FSABuild extends CliTool {
  /**
   * The serialization and encoding format to use for compressing the
   * automaton.
   */
  public enum Format {
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

  private static interface LineConsumer {
    byte[] process(byte[] buffer, int pos);
  }

  @Parameter(
      names = {"-i", "--input"},
      description = "The input sequences (one sequence per \\n-delimited line).", 
      required = true)
  private Path input;  

  @Parameter(
      names = {"-o", "--output"},
      description = "The output automaton file.", 
      required = true)
  private Path output;  

  @Parameter(
      names = {"-f", "--format"},
      description = "Automaton serialization format.")
  private Format format = Format.CFSA2;  

  @Parameter(
      names = {"--ignore-bom"},
      arity = 0,
      description = "Ignore leading BOM bytes (UTF-8).")  
  private boolean ignoreBom;

  @Parameter(
      names = {"--ignore-cr"},
      arity = 0,
      description = "Ignore CR bytes in input sequences (\\r).")  
  private boolean ignoreCr;

  @Parameter(
      names = {"--ignore-empty"},
      arity = 0,
      description = "Ignore empty lines in the input.")  
  private boolean ignoreEmpty;

  FSABuild() {
  }
  
  public FSABuild(Path input,
                  Path output,
                  Format format,
                  boolean ignoreBom,
                  boolean ignoreCr,
                  boolean ignoreEmpty) {
    this.input = checkNotNull(input);
    this.output = checkNotNull(output);
    this.ignoreBom = ignoreBom;
    this.ignoreCr = ignoreCr;
    this.ignoreEmpty = ignoreEmpty;
  }

  @Override
  public ExitStatus call() throws Exception {
    final ArrayList<byte[]> sequences = new ArrayList<>();
    try (InputStream is = new BufferedInputStream(Files.newInputStream(input))) {
      forAllLines(is, new LineConsumer() {
        @Override
        public byte[] process(byte[] buffer, int pos) {
          sequences.add(Arrays.copyOf(buffer, pos));
          return buffer;
        }
      });
    }

    // A little bit of sanity checking.
    if (sequences.size() >= 1) {
      if (!ignoreBom && hasUtf8Bom(sequences.get(0))) {
        System.err.println("ERROR: the input starts with UTF-8 BOM bytes which is" +
            " most likely not what you want. Use header-less UTF-8 file or override with --ignore-bom.");
        return ExitStatus.ERROR_OTHER;
      }

      if (!ignoreCr && hasCr(sequences)) {
        System.err.println("ERROR: the input contains \\r byte (CR) which would be encoded as part of the"
            + " automaton. If this is desired, use --ignore-cr.");
        return ExitStatus.ERROR_OTHER;
      }
      
      if (!ignoreEmpty && hasEmpty(sequences)) {
        System.err.println("ERROR: the input contains empty sequences."
            + " If these can be ignored, use --ignore-empty.");
        return ExitStatus.ERROR_OTHER;
      }      
    }

    Collections.sort(sequences, FSABuilder.LEXICAL_ORDERING);

    FSABuilder fsaBuilder = new FSABuilder();
    for (byte [] seq : sequences) {
      if (seq.length > 0) {
        fsaBuilder.add(seq, 0, seq.length);
      }
    }
    sequences.clear();
    FSA fsa = fsaBuilder.complete();

    FSASerializer serializer = format.getSerializer();
    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(output))) {
      serializer.serialize(fsa, os);
    }

    return ExitStatus.SUCCESS;
  }

  private boolean hasEmpty(ArrayList<byte[]> sequences) {
    for (byte [] seq : sequences) {
      if (seq.length == 0) {
        return true;
      }
    }
    return false;
  }

  private boolean hasCr(ArrayList<byte[]> sequences) {
    for (byte [] seq : sequences) {
      for (int o = seq.length; --o >= 0;) {
        if (seq[o] == '\r') {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean hasUtf8Bom(byte [] sequence) {
    return sequence.length >= 3 &&
           (sequence[0] & 0xff) == 0xef &&
           (sequence[1] & 0xff) == 0xbb &&
           (sequence[2] & 0xff) == 0xbf;
  }

  /**
   * Read all \\n-separated sequences.
   */
  private int forAllLines(InputStream is, LineConsumer lineConsumer) throws IOException {
    int lines = 0;
    byte[] buffer = new byte[0];
    int b, pos = 0;
    while ((b = is.read()) != -1) {
      if (b == '\n') {
        buffer = lineConsumer.process(buffer, pos);
        pos = 0;
        lines++;
      } else {
        if (pos >= buffer.length) {
          buffer = java.util.Arrays.copyOf(buffer, buffer.length + 10);
        }
        buffer[pos++] = (byte) b;
      }
    }

    if (pos > 0) {
      lineConsumer.process(buffer, pos);
      lines++;
    }
    return lines;
  }

  public static void main(String[] args) {
    main(args, new FSABuild());
  }
}
