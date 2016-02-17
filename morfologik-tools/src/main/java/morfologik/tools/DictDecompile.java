package morfologik.tools;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.WordData;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Decompiles morphological dictionary automaton back to source state. 
 */
@Parameters(
    commandNames = "dict_decompile",
    commandDescription = "Decompiles morphological dictionary automaton back to source state.")
public class DictDecompile extends CliTool {
  @Parameter(
      names = {"-i", "--input"},
      description = "The input dictionary (*.dict and a sibling *.info metadata).", 
      required = true,
      validateValueWith = ValidateFileExists.class)
  private Path input;

  @Parameter(
      names = {"-o", "--output"},
      description = "The output file for dictionary data.")
  private Path output;  

  @Parameter(
      names = ARG_OVERWRITE,
      description = "Overwrite the output file if it exists.")
  private boolean overwrite;  

  @Parameter(
      names = ARG_VALIDATE,
      arity = 1,
      description = "Validate decoded output to make sure it can be re-encoded.")
  private boolean validate = true;  

  DictDecompile() {
  }
  
  public DictDecompile(Path input,
                       Path output,
                       boolean overwrite,
                       boolean validate) {
    this.input = checkNotNull(input);
    this.output = output;
    this.overwrite = overwrite;
    this.validate = validate;
  }

  @Override
  public ExitStatus call() throws Exception {
    final Dictionary dictionary = Dictionary.read(input);
    final DictionaryLookup lookup = new DictionaryLookup(dictionary);

    if (output == null) {
      output = input.resolveSibling(input.getFileName().toString().replaceAll("\\.dict$", "") + ".input");
      if (Files.exists(output) && !overwrite) {
        System.err.println("ERROR: the default output file location already exists. Use --overwrite or remove"
            + " the file manually: " + output.toString());
        return ExitStatus.ERROR_CONFIRMATION_REQUIRED;
      }
    }

    final byte separator = dictionary.metadata.getSeparator();
    ByteBuffer stem = ByteBuffer.allocate(0);
    ByteBuffer word = ByteBuffer.allocate(0);
    ByteBuffer tag  = ByteBuffer.allocate(0);
    try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(output))) {
      boolean hasTags = false;
      for (WordData wd : lookup) {
        tag = wd.getTagBytes(tag);
        if (tag.hasRemaining()) {
          hasTags = true;
          break;
        }
      }

      for (WordData wd : lookup) {
        stem = wd.getStemBytes(stem);
        word = wd.getWordBytes(word);
        tag = wd.getTagBytes(tag);

        write(os, stem);
        os.write(separator);
        write(os, word);
        if (hasTags) {
          os.write(separator);
          write(os, tag);
        }
        os.write('\n');

        if (validate && (ensureNoSeparator(stem, separator) ||
                         ensureNoSeparator(word, separator))) {
          System.err.println("ERROR: The stem or word of a dictionary entry contains separator "
              + " byte " + FSAInfo.byteAsChar(separator) + ", this will prevent proper re-encoding."
              + " Add '--validate false' to override. Offending entry: "
              + wd.getStem() + ", "
              + wd.getWord());
          return ExitStatus.ERROR_OTHER;
        }
      }
    }

    return ExitStatus.SUCCESS;
  }

  private void write(OutputStream os, ByteBuffer bb) throws IOException {
    os.write(bb.array(), bb.arrayOffset() + bb.position(), bb.remaining());
  }

  private boolean ensureNoSeparator(ByteBuffer bb, byte marker) {
    byte [] buf = bb.array();
    for (int o = bb.arrayOffset() + bb.position(), i = bb.remaining(); i > 0; i--) {
      if (buf[o] == marker) {
        return true;
      }
    }
    return false;
  }

  public static void main(String[] args) {
    main(args, new DictDecompile());
  }
}
