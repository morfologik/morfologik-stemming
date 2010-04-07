package morfologik.tools;

import java.io.*;

import morfologik.fsa.morph.*;

import org.apache.commons.cli.*;

/**
 * This utility converts the dictionary in a text (tabbed) format into 
 * the format accepted by the fsa building tools. It is meant to replace
 * the Perl and AWK scripts from the original FSA package. 
 */
class MorphEncodingTool extends Tool {
	
	boolean prefixes = false;
	boolean infixes = false;
	/**
     * @author Marcin Milkowski
     */
	protected void go(CommandLine line) throws Exception {
		// Determine input/ output encoding.

		// Determine input and output streams.
		final BufferedReader input = initializeInput(line, "iso-8859-1");
		final Writer output = initializeOutput(line, "iso-8859-1");

		infixes = line.hasOption(SharedOptions.infixEncoding.getOpt());
		
		if (!infixes) {
			prefixes = line.hasOption(SharedOptions.prefixEncoding.getOpt());
		}
		
		try {
			process(input, output);
			output.flush();
			
		} finally {
			input.close();
			output.close();
		}

	}

	/**
	 * Process input stream, writing to output stream.
	 *  
	 */
	protected void process(BufferedReader input, Writer output) throws IOException {
		long lnumber = 0;
		try {
			String line = null; // not declared within while loop			
			while ((line = input.readLine()) != null) {
				lnumber++;
				String[] words = line.split("\t");
				if (words.length < 3) {
					throw new IllegalArgumentException("The input file has less than 3 fields in line: " + lnumber);
				}
				if (infixes) {
					output.write(FSAMorphCoder.infixEncode(words[0], words[1], words[2]));
					
				} else if (prefixes) {
					output.write(FSAMorphCoder.prefixEncode(words[0], words[1], words[2]));
				} else {
					output.write(FSAMorphCoder.standardEncode(words[0], words[1], words[2]));
				}
				
				output.write("\n");
			}
		} finally {
			input.close();
		}
	}
	
	/**
	 * Command line options for the tool.
	 */
	protected void initializeOptions(Options options) {
		options.addOption(SharedOptions.inputFileOption);		
		options.addOption(SharedOptions.outputFileOption);
		options.addOption(SharedOptions.standardEncoding);
		options.addOption(SharedOptions.prefixEncoding);
		options.addOption(SharedOptions.infixEncoding);		
	}

	/**
     * 
     */
	private Writer initializeOutput(CommandLine line, String outputEncoding)
	        throws IOException, ParseException {
		final Writer output;
		final String opt = SharedOptions.outputFileOption.getOpt();
		if (line.hasOption(opt)) {
			// Use output file.
			output = new OutputStreamWriter(
			        new BufferedOutputStream(new FileOutputStream((File) line
			                .getParsedOptionValue(opt))), outputEncoding);
		} else {
			// Use standard output.
			output = new OutputStreamWriter(System.out, outputEncoding);
		}
		return output;
	}

	/**
     * 
     */
	private BufferedReader initializeInput(CommandLine line, String inputEncoding)
	        throws IOException, ParseException {
		final BufferedReader input;
		final String opt = SharedOptions.inputFileOption.getOpt();

		if (line.hasOption(opt)) {
			// Use input file.
			input = new BufferedReader( 
				new InputStreamReader(
			        new BufferedInputStream(new FileInputStream((File) line
			                .getParsedOptionValue(opt))), inputEncoding));
		} else {
			// Use standard input.
			input = new BufferedReader(new InputStreamReader(System.in, inputEncoding));
		}
		return input;
	}

	
	/**
	 * Command line entry point.
	 */
	public static void main(String[] args) throws Exception {
		final MorphEncodingTool tool = new MorphEncodingTool();
		tool.go(args);
	}
}