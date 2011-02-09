package morfologik.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import morfologik.fsa.FSA5;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * This utility converts the dictionary in a text (tabbed) format into 
 * the format accepted by the fsa building tools. It is meant to replace
 * the Perl and AWK scripts from the original FSA package. 
 */
class MorphEncodingTool extends Tool {
	
	private boolean prefixes = false;
	private boolean infixes = false;
	private boolean noWarn = false;

	private MorphEncoder encoder;

	/**
     * 
     */
	protected void go(final CommandLine line) throws Exception {
		
		noWarn = line.hasOption(SharedOptions.noWarnIfTwoFields.getOpt());
		
		infixes = line.hasOption(SharedOptions.infixEncoding.getOpt());
		
		if (!infixes) {
			prefixes = line.hasOption(SharedOptions.prefixEncoding.getOpt());
		}

		char separator = FSA5.DEFAULT_ANNOTATION;
		if (line.hasOption(SharedOptions.annotationSeparatorCharacterOption.getLongOpt())) {
			String sep = line.getOptionValue(SharedOptions.annotationSeparatorCharacterOption.getLongOpt());

			if (sep.length() == 1) {
				separator = sep.charAt(0);
			}

			FSABuildTool.checkSingleByte(Character.toString(separator));
		}
		encoder = new MorphEncoder((byte) separator);

		// Determine input and output streams.
		final DataInputStream input = initializeInput(line);
		final DataOutputStream output = initializeOutput(line);
		
		try {
			process(input, output);
			output.flush();
			
		} finally {
			input.close();
			output.close();
		}

	}

	/**
	 * Split fields
	 * @param line 
	 * 			byte input buffer
	 * @param pos
	 * 			current offset in the file
	 * @return
	 * 			an array of three byte arrays; if there are less
	 * 			than three fields, one of byte arrays is null. If the
	 * 			line contains more than three fields, they are ignored. 
	 */
	private static byte[][] splitFields(final byte[] line, final int pos) {
		byte[][] outputArray = new byte[3][];
		int i = 0;
		int prevPos = 0;
		int arrayInd = 0;
		while (i < pos) {			
			if (line[i] == (byte)'\t') { //tab
				outputArray[arrayInd] = new byte[i - prevPos];
				System.arraycopy(line, prevPos, outputArray[arrayInd], 0, i - prevPos);
				prevPos = i + 1;
				arrayInd++;
			}
			i++;
		}
		return outputArray;
	}
	
	/**
	 * Process input stream, writing to output stream.
	 *  
	 */
	protected void process(final DataInputStream input, final DataOutputStream output)
	        throws IOException {
		long lnumber = 0;
		try {
			int bufPos = 0;
			byte[] buf = new byte[0xfffff]; // assumed that line is shorter than
											// 64K chars
			int dataByte = -1; // not declared within while loop
			byte[][] words;
			while ((dataByte = input.read()) != -1) {				
				if (dataByte == (byte) '\n') {
					lnumber++;
					buf[bufPos++] = 9;
					words = splitFields(buf, bufPos);					
					for (int i = 0; i < words.length; i++) {
						if (i < 1 && words[i] == null) {
							throw new IllegalArgumentException(
							        "The input file has less than 2 fields in line: "
							                + lnumber);
						} 
						if (words[i] == null && !noWarn) {	
							System.err.println("Line number: " + lnumber + " has less than three fields.");
						}
					}

					if (infixes) {
						output.write(encoder.infixEncode(words[0], words[1], words[2]));
					} else if (prefixes) {
						output.write(encoder.prefixEncode(words[0], words[1], words[2]));
					} else {
						output.write(encoder.standardEncode(words[0], words[1], words[2]));
					}

					output.writeByte('\n'); // Unix line end only.
					bufPos = 0;
				} else {
					if (dataByte != (byte) '\r') { 
						buf[bufPos++] = (byte) dataByte;
					}
				}
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
		options.addOption(SharedOptions.noWarnIfTwoFields);
		options.addOption(SharedOptions.annotationSeparatorCharacterOption);
	}

	/**
     * 
     */
	private static DataOutputStream initializeOutput(CommandLine line)
	        throws IOException, ParseException {
		final DataOutputStream output;		
		final String opt = SharedOptions.outputFileOption.getOpt();
		if (line.hasOption(opt)) {
			// Use output file.
			output = new DataOutputStream(
					new BufferedOutputStream(
					new FileOutputStream((File) line
			                .getParsedOptionValue(opt))));
		} else {
			// Use standard output.
			output = new DataOutputStream(
					new BufferedOutputStream(
					System.out));
		}
		return output;
	}

	/**
     * 
     */
	private static DataInputStream initializeInput(CommandLine line)
	        throws IOException, ParseException {
		final DataInputStream input;
		final String opt = SharedOptions.inputFileOption.getOpt();
		if (line.hasOption(opt)) {
			// Use input file.
			input = new DataInputStream ( 
					new BufferedInputStream(
					new FileInputStream((File) line
			                .getParsedOptionValue(opt))));
		} else {
			// Use standard input.
			input = new DataInputStream(
					new BufferedInputStream(
					System.in));
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