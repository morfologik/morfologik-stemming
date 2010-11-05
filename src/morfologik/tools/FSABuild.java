package morfologik.tools;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import morfologik.fsa.FSA5;
import morfologik.fsa.FSA5Serializer;
import morfologik.fsa.FSABuilder;
import morfologik.fsa.FSAInfo;
import morfologik.fsa.State;
import morfologik.fsa.StateUtils;
import morfologik.util.Arrays;

import org.apache.commons.cli.*;

/**
 * Convert from plain text input to {@link FSA5}.
 */
public final class FSABuild extends Tool {
    /**
     * Be more verbose about progress.
     */
    private boolean printProgress;
    
    /**
     * Last part's start time.
     */
    private long partStart;

    /**
     * Start of the process.
     */
    private long world;
    
    /**
     * Serializer used for emitting the FSA.
     */
    private FSA5Serializer serializer = new FSA5Serializer();
    
	/**
	 * Command line entry point after parsing arguments.
	 */
	protected void go(CommandLine line) throws Exception {
		String[] args = line.getArgs();
		if (args.length != 0) {
			printUsage();
			return;
		}
		
		// Parse the input options.
        parseOptions(line);

		world = System.currentTimeMillis();
		try {
			if (!printProgress) {
			    startPart("Reading input");
			} else {
			    this.partStart = System.currentTimeMillis();
			}
			final ArrayList<byte[]> input = readInput(initializeInput(line));
			if (printProgress) {
			    long partStart = this.partStart;
			    startPart("Reading input");
			    this.partStart = partStart;
			}
		    endPart();
            logInt("Input sequences", input.size());

			startPart("Sorting");
			Collections.sort(input, FSABuilder.LEXICAL_ORDERING);
			endPart();

			startPart("Building FSA");
			State root = FSABuilder.build(input);
			endPart();

			startPart("Statistics");
			FSAInfo info = StateUtils.getInfo(root);
	        endPart();

            logInt("Nodes", info.nodeCount);
            logInt("Arcs", info.arcsCount);

			// Save the result.
            startPart("Serializing FSA");
			serializer.serialize(root, initializeOutput(line)).close();
			endPart();
		} catch (OutOfMemoryError e) {
			log("Out of memory. Pass -Xmx1024m argument (or more) to java.");
		}
	}

    /**
	 * Parse input options.
	 */
	private void parseOptions(CommandLine line) {
		String opt = SharedOptions.fillerCharacterOption.getLongOpt();
		if (line.hasOption(opt)) {
			String chr = line.getOptionValue(opt);
			checkSingleByte(chr);
			serializer.fillerByte = chr.getBytes()[0];
		}
		
		opt = SharedOptions.annotationSeparatorCharacterOption.getLongOpt();
		if (line.hasOption(opt)) {
			String chr = line.getOptionValue(opt);
			checkSingleByte(chr);
			serializer.annotationByte = chr.getBytes()[0];
		}
		
        opt = SharedOptions.withNumbersOption.getOpt();
        if (line.hasOption(opt)) {
            serializer.withNumbers();
        }
        
        opt = SharedOptions.progressOption.getLongOpt();
        if (line.hasOption(opt)) {
            printProgress = true;
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
		System.err.flush();
	}

	/**
     * Log message header and save current time.
     */
    private void startPart(String header) {
        System.err.print(String.format(Locale.ENGLISH, "%-20s", header + "..."));
        System.err.flush();
        partStart = System.currentTimeMillis();
    }

    /**
     * 
     */
    private void endPart() {
        long now = System.currentTimeMillis();
        System.err.println(
                String.format(Locale.ENGLISH, "%13.2f sec.  [%6.2f sec.]", 
                (now - partStart) / 1000.0,
                (now - world) / 1000.0));
    }

    /**
     * Log an integer statistic.
     */
    private void logInt(String header, int v) {
        System.err.println(String.format(Locale.ENGLISH, "%-20s  %,11d", header, v));
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
				if (printProgress && (line++ % 10000) == 0) {
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
		formatter.printHelp(this.getClass().getName(), options, true);
	}

	@Override
	protected void initializeOptions(Options options) {
		options.addOption(SharedOptions.inputFileOption);
		options.addOption(SharedOptions.outputFileOption);
		
		options.addOption(SharedOptions.fillerCharacterOption);
		options.addOption(SharedOptions.annotationSeparatorCharacterOption);

		options.addOption(SharedOptions.withNumbersOption);
		options.addOption(SharedOptions.progressOption);
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