package morfologik.tools;

import java.io.File;
import java.util.Arrays;

import morfologik.stemming.EncoderType;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

/**
 * Options shared between tools.
 */
@SuppressWarnings("static-access")
final class SharedOptions {
    public final static Option fsaDictionaryFileOption = OptionBuilder
        .hasArg()
        .withArgName("file")
        .withDescription("Path to the FSA dictionary.")
        .withLongOpt("dictionary")
        .withType(File.class)
        .isRequired(true)
        .create("d");

	public final static Option decode = OptionBuilder
	    .withDescription("Decode prefix/ infix/ suffix forms (if available).")
	    .withLongOpt("decode")
	    .isRequired(false)
	    .create("x");

	public final static Option dataOnly = OptionBuilder
	    .withDescription("Dump only raw FSA data.")
	    .withLongOpt("raw-data")
	    .isRequired(false)
	    .create("r");

	public final static Option dot = OptionBuilder
	    .withDescription("Dump the automaton as graphviz DOT file.")
	    .withLongOpt("dot")
	    .isRequired(false)
	    .create();

	public final static Option inputEncodingOption = OptionBuilder
	    .hasArg()
	    .withArgName("codepage")
	    .withDescription("Input stream encoding.")
	    .withLongOpt("input-encoding")
	    .isRequired(false)
	    .create("ie");

	public final static Option outputEncodingOption = OptionBuilder
	    .hasArg()
	    .withArgName("codepage")
	    .withDescription("Output stream encoding.")
	    .withLongOpt("output-encoding")
	    .isRequired(false)
	    .create("oe");

	public final static Option inputFileOption = OptionBuilder
	    .hasArg()
	    .withArgName("file")
	    .withDescription("Input file. If missing, standard input is used.")
	    .withLongOpt("input")
	    .withType(File.class)
	    .isRequired(false)
	    .create("i");

	public final static Option outputFileOption = OptionBuilder
	    .hasArg()
	    .withArgName("file")
	    .withDescription("Output file. If missing, standard output is used.")
	    .withLongOpt("output")
	    .withType(File.class)
	    .isRequired(false)
	    .create("o");

    public final static Option outputFormatOption = OptionBuilder
        .hasArg()
        .withArgName("format")
        .withDescription("Name of the binary output format. Allowed values: " + Arrays.toString(FSABuildTool.Format.values()))
        .withLongOpt("format")
        .isRequired(false)
        .create("f");

	public final static Option fillerCharacterOption = OptionBuilder
		.hasArg()
    	.withArgName("char")
    	.withDescription("Custom filler character")
    	.isRequired(false)
    	.withLongOpt("filler")
    	.create();

	public final static Option annotationSeparatorCharacterOption = OptionBuilder
		.hasArg()
		.withArgName("char")
		.withDescription("Custom annotation separator character")
		.isRequired(false)
		.withLongOpt("annotation")
		.create();

	public final static Option withNumbersOption = OptionBuilder
	    .withDescription("Include numbers required for perfect hashing (larger automaton)")
	    .isRequired(false)
	    .withLongOpt("with-numbers")
	    .create("n");

    public final static Option progressOption = OptionBuilder
        .withDescription("Print more verbose progress information")
        .isRequired(false)
        .withLongOpt("progress")
        .create();

    public final static Option inputSortedOption = OptionBuilder
        .withDescription("Assume the input is already sorted using C-sort (builds FSA directly, no in-memory sorting)")
        .isRequired(false)
        .withLongOpt("sorted")
        .create();

	public final static Option encoder = OptionBuilder
	    .withDescription("Encoder used for compressing inflected forms. Any of: "
	        + Arrays.toString(EncoderType.values()))
	    .withLongOpt("encoder")
        .hasArg(true)
	    .withArgName("name")
	    .isRequired(false)
	    .create("e");

	public final static Option noWarnIfTwoFields = OptionBuilder
	    .withDescription("Suppress warning for lines with only two fields (for stemming dictionaries)")
		.withLongOpt("nowarn")
		.isRequired(false)
		.create("nw");

    public final static Option statistics = OptionBuilder
        .withDescription("Print extra statistics.")
        .isRequired(false)
        .withLongOpt("stats")
        .create();

    public final static Option help = OptionBuilder
        .withDescription("Help on available options.")
        .withLongOpt("help")
        .isRequired(false)
        .create();

	/**
	 * No instances. Use static fields.
	 */
	private SharedOptions() {
		// empty
	}
}
