package morfologik.tools;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import morfologik.fsa.CFSA;
import morfologik.fsa.CFSA2;
import morfologik.fsa.FSA;
import morfologik.fsa.FSA5;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Print extra information about a compiled automaton file.
 */
@Parameters(
    commandNames = "fsa_info",
    commandDescription = "Print extra information about a compiled automaton file.")
public class FSAInfo extends CliTool {
  @Parameter(
      names = {"-i", "--input"},
      description = "The input automaton.", 
      required = true,
      validateValueWith = ValidateFileExists.class)
  private Path input;

  FSAInfo() {
  }

  public FSAInfo(Path input) {
    this.input = checkNotNull(input);
  }

  @Override
  public ExitStatus call() throws Exception {
    final FSA fsa;
    try (InputStream is = new BufferedInputStream(Files.newInputStream(input))) {
      fsa = FSA.read(is);
    }

    printf("%-25s : %s", "FSA implementation", fsa.getClass().getName());
    printf("%-25s : %s", "Compiled with flags", fsa.getFlags().toString());

    final morfologik.fsa.builders.FSAInfo info = new morfologik.fsa.builders.FSAInfo(fsa);
    printf("%-25s : %,d", "Number of arcs (merged)", info.arcsCount);
    printf("%-25s : %,d", "Number of arcs (total)", info.arcsCountTotal);
    printf("%-25s : %,d", "Number of nodes", info.nodeCount);
    printf("%-25s : %,d", "Number of final states", info.finalStatesCount);
    printf("");

    if (fsa instanceof FSA5) {
      FSA5 fsa5 = (FSA5) fsa;
      printf("%-25s : %d", "Goto length (GTL)", fsa5.gtl);
      printf("%-25s : %d", "Node extra data", fsa5.nodeDataLength);
      printf("%-25s : %s", "Annotation separator", byteAsChar(fsa5.annotation));
      printf("%-25s : %s", "Filler character", byteAsChar(fsa5.filler));
    }

    if (fsa instanceof CFSA) {
      CFSA cfsa = (CFSA) fsa;
      printf("%-25s : %d", "Goto length (GTL)", cfsa.gtl);
      printf("%-25s : %d", "Node extra data", cfsa.nodeDataLength);
    }

    if (fsa instanceof CFSA2) {
      CFSA2 cfsa2 = (CFSA2) fsa;

      byte [] labelMapping = cfsa2.labelMapping;
      if (labelMapping != null && labelMapping.length > 0) {
        printf("%-25s :", "Label mapping");
        for (int i = 0; i < labelMapping.length; i++) {
          printf("%-25s   %2d -> %s", "", i, byteAsChar(labelMapping[i]));
        }
      }
    }

    return ExitStatus.SUCCESS;
  }

  /**
   * Convert a byte to an informative string.
   */
  static String byteAsChar(byte v) {
    int chr = v & 0xff;
    return String.format(Locale.ROOT, 
        "%s (0x%02x)",
        (Character.isWhitespace(chr) || chr > 127) ? "[non-printable]" : Character.toString((char) chr),
        v & 0xFF);
  }

  public static void main(String[] args) {
    main(args, new FSAInfo());
  }
}
