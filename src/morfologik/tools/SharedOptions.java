package morfologik.tools;

import java.io.File;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

/**
 * Options shared between tools.
 */
@SuppressWarnings("static-access")
final class SharedOptions {
	public final static Option fsaDictionaryFileOption = OptionBuilder.hasArg()
	        .withArgName("file").withDescription("Path to the FSA dictionary.")
	        .withLongOpt("dictionary").withType(File.class).isRequired(true)
	        .create("d");

	public final static Option decode = OptionBuilder.withDescription(
	        "Decode prefix/ infix/ suffix forms (if available).").withLongOpt(
	        "decode").isRequired(false).create("x");

	public final static Option dataOnly = OptionBuilder.withDescription(
	        "Dump only raw FSA data.").withLongOpt("raw-data")
	        .isRequired(false).create("r");

	public final static Option dot = OptionBuilder.withDescription(
	        "Dump the automaton as graphviz DOT file.").withLongOpt("dot")
	        .isRequired(false).create();

	public final static Option inputEncodingOption = OptionBuilder.hasArg()
	        .withArgName("codepage").withDescription("Input stream encoding.")
	        .withLongOpt("input-encoding").isRequired(false).create("ie");

	public final static Option outputEncodingOption = OptionBuilder.hasArg()
	        .withArgName("codepage").withDescription("Output stream encoding.")
	        .withLongOpt("output-encoding").isRequired(false).create("oe");

	public final static Option inputFileOption = OptionBuilder.hasArg()
	        .withArgName("file").withDescription(
	                "Input file. If missing, standard input is used.")
	        .withLongOpt("input").withType(File.class).isRequired(false)
	        .create("i");

	public final static Option outputFileOption = OptionBuilder.hasArg()
	        .withArgName("file").withDescription(
	                "Output file. If missing, standard output is used.")
	        .withLongOpt("output").withType(File.class).isRequired(false)
	        .create("o");

	public final static Option fillerCharacterOption = OptionBuilder.hasArg()
    	.withArgName("char").withDescription("Custom filler character")
    	.isRequired(false).withLongOpt("filler-char")
    		.create("filler");

	public final static Option annotationSeparatorCharacterOption = OptionBuilder.hasArg()
		.withArgName("char").withDescription("Custom annotation separator character")
		.isRequired(false).withLongOpt("annotation-char")
		.create("annotation");

	public final static Option standardEncoding = OptionBuilder
	        .withDescription("Encode suffix forms in a standard way")
	        .withLongOpt("suffix").isRequired(false).create("suf");

	public final static Option prefixEncoding = OptionBuilder.withDescription(
	        "Encode suffix forms in a prefix way").withLongOpt("prefix")
	        .isRequired(false).create("pre");

	public final static Option infixEncoding = OptionBuilder.withDescription(
	        "Encode suffix forms in an infix way").withLongOpt("infix")	        
	        .isRequired(false).create("inf");

	public final static Option noWarnIfTwoFields = OptionBuilder.withDescription(
    "Suppress warning for lines with only two fields (for stemming dictionaries)").withLongOpt("nowarn")
    .isRequired(false).create("nw");
	
	
	public final static Option fieldSeparator = OptionBuilder.hasArg()
	.withArgName("char").withDescription(
	"Dictionary separator character").withLongOpt("separator")
	.isRequired(false).create("sep"); 
	
	
	/**
	 * No instances. Use static fields.
	 */
	private SharedOptions() {
		// empty
	}
}
