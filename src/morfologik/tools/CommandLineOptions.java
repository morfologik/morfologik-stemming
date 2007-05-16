package morfologik.tools;

import java.io.File;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

/**
 * Common command line options (for consistency across tools).
 * 
 * @author Dawid Weiss
 */
public final class CommandLineOptions {
    public final static Option fsaDictionaryFileOption = 
        OptionBuilder
            .hasArg().withArgName("file")
            .withDescription("Path to FSA dictionary file.")
            .withLongOpt("dictionary")
            .withType(File.class)
            .isRequired(true)
            .create("d");

    public final static Option characterEncodingOption =
        OptionBuilder
            .hasArg().withArgName("codepage")
            .withDescription("Character encoding in the dictionary.")
            .withLongOpt("encoding")
            .isRequired(true)
            .create("e");

    public final static Option useApiOption =
        OptionBuilder
            .withDescription("Use node traversal API instead of linear dump.")
            .withLongOpt("use-api")
            .isRequired(false)
            .create("a");

    public final static Option inputEncodingOption = 
        OptionBuilder
            .hasArg().withArgName("codepage")
            .withDescription("Input stream encoding.")
            .withLongOpt("input-encoding")
            .isRequired(false)
            .create("ie");

    public final static Option outputEncodingOption = 
        OptionBuilder
            .hasArg().withArgName("codepage")
            .withDescription("Output stream encoding.")
            .withLongOpt("output-encoding")
            .isRequired(false)
            .create("oe");

    public final static Option inputFileOption = 
        OptionBuilder
            .hasArg().withArgName("file")
            .withDescription("Input file. If missing, standard input is used.")
            .withLongOpt("input")
            .withType(File.class)
            .isRequired(false)
            .create("i");

    public final static Option outputFileOption = 
        OptionBuilder
            .hasArg().withArgName("file")
            .withDescription("Output file. If missing, standard output is used.")
            .withLongOpt("output")
            .withType(File.class)
            .isRequired(false)
            .create("o");

    /**
     * No instances. Use static fields.
     */
    private CommandLineOptions() {
        // empty
    }
}
