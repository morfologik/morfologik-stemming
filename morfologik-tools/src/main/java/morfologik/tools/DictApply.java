package morfologik.tools;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.WordData;

/**
 * Applies a morphological dictionary automaton to the input. 
 */
@Parameters(
    commandNames = "dict_apply",
    commandDescription = "Applies a dictionary to an input. Each line is considered an input term.")
public class DictApply extends CliTool {
  private final static String ARG_ENCODING = "--input-charset";
  
  @Parameter(
      names = {"-i", "--input"},
      required = false,
      description = "The input file, each entry in a single line. If not provided, stdin is used.",
      validateValueWith = ValidateFileExists.class)
  private Path input;

  @Parameter(
      names = {"-d", "--dictionary"},
      description = "The dictionary (*.dict and a sibling *.info metadata) to apply.", 
      required = true,
      validateValueWith = ValidateFileExists.class)
  private Path dictionary;

  @Parameter(
      names = {ARG_ENCODING},
      required = false,
      description = "Character encoding of the input (platform's default).")
  private String inputEncoding;

  @Parameter(
      names = {"--skip-tags"},
      required = false,
      description = "Skip tags in the output, only print base forms if found.")
  private boolean skipTags = false;

  private abstract class LineSupplier implements Closeable {
    public abstract String nextLine() throws IOException;

    @Override
    public void close() throws IOException {
      // No-op by default.
    }
  }

  private class ReaderLineSupplier extends LineSupplier {
    private final BufferedReader lineReader;

    public ReaderLineSupplier(BufferedReader reader) {
      this.lineReader = reader;
    }

    @Override
    public String nextLine() throws IOException {
      return lineReader.readLine();
    }
    
    @Override
    public void close() throws IOException {
      lineReader.close();
    }
  }

  DictApply() {
  }
  
  public DictApply(Path dictionary,
                   Path input,
                   String inputEncoding) {
    this.input = checkNotNull(input);
    this.dictionary = checkNotNull(dictionary);
  }

  @Override
  public ExitStatus call() throws Exception {
    ExitStatus exitStatus = validateArguments();
    if (exitStatus != null) {
      return exitStatus;
    }
    
    final DictionaryLookup lookup = new DictionaryLookup(Dictionary.read(this.dictionary));
    try (final LineSupplier input = determineInput()) {
      String line;
      while ((line = input.nextLine()) != null) {
        if (line.length() == 0) {
          continue;
        }

        List<WordData> wordData = lookup.lookup(line);
        if (wordData.isEmpty()) {
          System.out.println(line + " => [not found]");
        } else {
          for (WordData wd : wordData) {
            CharSequence stem = wd.getStem();
            CharSequence tag = wd.getTag();
            System.out.println(line + " => " +
                ((skipTags || tag == null) ? stem
                                           : stem + " " + tag));
          }
        }
      }
    }

    return ExitStatus.SUCCESS;
  }

  private LineSupplier determineInput() throws IOException {
    if (this.input != null) {
      return new ReaderLineSupplier(Files.newBufferedReader(this.input, Charset.forName(inputEncoding)));
    }

    final Console c = System.console();
    if (c != null) {
      System.err.println("NOTE: Using Console for input, character encoding is unknown but should be all right.");
      return new LineSupplier() {
        @Override
        public String nextLine() throws IOException {
          return c.readLine();
        }
      };
    }

    Charset charset = this.inputEncoding != null ? Charset.forName(this.inputEncoding)
                                                 : Charset.defaultCharset();
    System.err.println("NOTE: Using stdin for input, character encoding set to: " + charset.name() +
        " (use " + ARG_ENCODING + " to override).");
    return new ReaderLineSupplier(
        new BufferedReader(
            new InputStreamReader(
                new BufferedInputStream(System.in), charset)));
  }

  private ExitStatus validateArguments() {
    if (this.input != null) {
      if (this.inputEncoding == null) {
        System.err.println("Input encoding is required if file input is used.");
        return ExitStatus.ERROR_INVALID_ARGUMENTS;
      }
    } else {
      if (System.console() != null && this.inputEncoding != null) {
        System.err.println("Input encoding is only valid with file input or stdin redirection.");
        return ExitStatus.ERROR_INVALID_ARGUMENTS;
      }
    }

    return null;
  }

  public static void main(String[] args) {
    main(args, new DictApply());
  }
}
