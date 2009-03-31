package morfologik.tools;

import java.io.File;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

/**
 * Options shared between tools.
 */
@SuppressWarnings("static-access")
final class SharedOptions {
    public final static Option fsaDictionaryFileOption = 
        OptionBuilder
            .hasArg().withArgName("file")
            .withDescription("Path to the FSA dictionary.")
            .withLongOpt("dictionary")
            .withType(File.class)
            .isRequired(true)
            .create("d");

    public final static Option decode =
        OptionBuilder
            .withDescription("Decode prefix/ infix/ suffix forms (if available).")
            .withLongOpt("decode")
            .isRequired(false)
            .create("x");

    public final static Option dataOnly =
        OptionBuilder
            .withDescription("Dump only raw FSA data.")
            .withLongOpt("raw-data")
            .isRequired(false)
            .create("r");

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
    private SharedOptions() {
        // empty
    }
}
