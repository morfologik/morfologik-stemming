package morfologik.tools;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

import morfologik.fsa.FSA5;
import morfologik.fsa.FSA5Serializer;
import morfologik.fsa.FSABuilder;
import morfologik.fsa.State;
import morfologik.util.Arrays;

import org.apache.commons.cli.*;

/**
 * Convert from plain text input to {@link FSA5}.
 */
public final class FSABuild extends Tool {
	/**
	 * Command line entry point after parsing arguments.
	 */
	protected void go(CommandLine line) throws Exception {
		String[] args = line.getArgs();
		if (args.length != 0) {
			printUsage();
			return;
		}

		final long world = System.currentTimeMillis();
		try {
			log("Reading input...");
			final ArrayList<byte[]> input = readInput(initializeInput(line));

			log("Sorting...");
			Collections.sort(input, FSABuilder.LEXICAL_ORDERING);
			log("Input consists of: " + input.size() + " sequences.");

			log("Building FSA...");
			final long start = System.currentTimeMillis();
			State build = FSABuilder.build(input);
			final long end = System.currentTimeMillis();
			log("FSA built in: " + String.format("%.2f", (end - start) / 1000.0) + " sec.");

			// Save the result.
			log("Saving FSA...");
			FSA5Serializer serializer = new FSA5Serializer();
			initializeSerializer(serializer, line);
			serializer.serialize(build, initializeOutput(line)).close();

			log("Done in: " + String.format("%.2f", 
					(System.currentTimeMillis() - world) / 1000.0) + " sec.");
		} catch (OutOfMemoryError e) {
			log("Out of memory. Pass -Xmx1024m argument (or more) to java.");
		}
	}

	/**
	 * Initialize automaton serializer.
	 */
	private void initializeSerializer(FSA5Serializer serializer,
            CommandLine line) {
		String opt = SharedOptions.fillerCharacterOption.getOpt();
		if (line.hasOption(opt)) {
			String chr = line.getOptionValue(opt);
			checkSingleByte(chr);
			serializer.fillerByte = chr.getBytes()[0];
		}
		
		opt = SharedOptions.annotationSeparatorCharacterOption.getOpt();
		if (line.hasOption(opt)) {
			String chr = line.getOptionValue(opt);
			checkSingleByte(chr);
			serializer.annotationByte = chr.getBytes()[0];
		}
    }

	/**
	 * Check if the argument is a single byte after conversion using platform-default
	 * encoding. 
	 */
	public static void checkSingleByte(String chr) {
		if (chr.getBytes().length == 1)
			return;

		throw new IllegalArgumentException("Filler and annotation characters must be single" +
				"-byte values, " + chr + " has " + chr.getBytes().length + " bytes."); 
    }

	/**
	 * Log progress to the console.
	 */
	private void log(String msg) {
		System.err.println(msg);
	}

	/**
	 * Read all the input lines.
	 */
	private ArrayList<byte[]> readInput(InputStream is) throws IOException {
		final ArrayList<byte[]> input = new ArrayList<byte[]>();

		boolean warned = false;
		byte[] buffer = new byte[0];
		int line = 0, b, pos = 0;
		while ((b = is.read()) != -1) {
			if (b == '\r' && !warned) {
				log("Warning: input contains carriage returns?");
				warned = true;
			}

			if (b == '\n') {
				processLine(input, buffer, pos);
				pos = 0;
				if ((line++ % 10000) == 0) {
					log("Lines read: " + (line - 1));
				}
			} else {
				if (pos >= buffer.length) {
					buffer = Arrays.copyOf(buffer, buffer.length + 1);
				}
				buffer[pos++] = (byte) b;
			}
		}
		processLine(input, buffer, pos);
		return input;
	}

	/**
	 * Process a single line.
	 */
	private void processLine(ArrayList<byte[]> input, byte[] buffer, int pos) {
		if (pos == 0)
			return;
		input.add(Arrays.copyOf(buffer, pos));
	}

	@Override
	protected void printUsage() {
		final HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(this.getClass().getName() + " in.fsa out.cfsa",
		        options, true);
	}

	@Override
	protected void initializeOptions(Options options) {
		options.addOption(SharedOptions.inputFileOption);
		options.addOption(SharedOptions.outputFileOption);
		
		options.addOption(SharedOptions.fillerCharacterOption);
		options.addOption(SharedOptions.annotationSeparatorCharacterOption);
	}

	/**
     * 
     */
	private static OutputStream initializeOutput(CommandLine line)
	        throws IOException, ParseException {
		final OutputStream output;
		final String opt = SharedOptions.outputFileOption.getOpt();
		if (line.hasOption(opt)) {
			// Use output file.
			output = new FileOutputStream((File) line.getParsedOptionValue(opt));
		} else {
			// Use standard output.
			output = System.out;
		}
		return new BufferedOutputStream(output);
	}

	/**
     * 
     */
	private static InputStream initializeInput(CommandLine line)
	        throws IOException, ParseException {
		final InputStream input;
		final String opt = SharedOptions.inputFileOption.getOpt();

		if (line.hasOption(opt)) {
			// Use input file.
			input = new FileInputStream((File) line.getParsedOptionValue(opt));
		} else {
			// Use standard input.
			input = System.in;
		}
		return new BufferedInputStream(input);
	}

	/**
	 * Command line entry point.
	 */
	public static void main(String[] args) throws Exception {
		final FSABuild tool = new FSABuild();
		tool.go(args);
	}
}