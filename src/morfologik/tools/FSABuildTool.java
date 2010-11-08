package morfologik.tools;

import java.io.*;
import java.util.*;

import morfologik.fsa.*;
import morfologik.util.Arrays;

import org.apache.commons.cli.*;

/**
 * Convert from plain text input to {@link FSA5} or {@link CFSA}.
 */
public final class FSABuildTool extends Tool {
    /**
     * The serialization format to use for the binary output.
     */
    public enum Format {
        FSA5,
        CFSA;

        public FSASerializer getSerializer() {
            switch (this) {
                case FSA5:
                    return new FSA5Serializer();
                case CFSA:
                    return new CFSASerializer();

                default:
                    throw new RuntimeException();
            }
        }
    }

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
    private FSASerializer serializer;

    /**
     * Output format name.
     */
    private Format format;
    
    /**
     * Warn about CR characters in the input (usually not what you want).
     */
    private boolean crWarning = false;
    
    /**
     * If <code>true</code>, the input is not buffered and sorted in-memory, but
     * must be sorted externally (using the "C" convention: unsigned byte values).
     */
    private boolean inputSorted;

    /**
     * Gets fed with the lines read from the input.
     */
    private static interface LineConsumer {
        /**
         * Process the buffer, return the same buffer or a new buffer (for
         * swapping).
         */
        byte[] process(byte[] buffer, int pos);
    }

    /**
     * To help break out of the anonymous delegate on error.
     */
    @SuppressWarnings("serial")
    private static class TerminateProgramException extends RuntimeException {
        public TerminateProgramException(String msg) {
            super(msg);
        }

        public synchronized Throwable fillInStackTrace() {
            return null;
        }
    }
    
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
		    InputStream inputStream = initializeInput(line);

            if (!printProgress) {
                if (inputSorted) {
                    log("Assuming input is already sorted");
                    startPart("Building FSA");
                } else {
                    startPart("Reading input");
                }
            } else {
                this.partStart = System.currentTimeMillis();
            }

		    final State root;
		    if (inputSorted) {
                root = processSortedInput(inputStream);
		    } else {
	            root = processUnsortedInput(inputStream);
		    }

			startPart("Statistics");
			FSAInfo info = StateUtils.getInfo(root);
	        endPart();

            logInt("Nodes", info.nodeCount);
            logInt("Arcs", info.arcsCount);

			// Save the result.
            startPart("Serializing " + format);
			serializer.serialize(root, initializeOutput(line)).close();
			endPart();
		} catch (OutOfMemoryError e) {
			log("Out of memory. Pass -Xmx1024m argument (or more) to java.");
		}
	}

	/**
	 * 
	 */
    private State processUnsortedInput(InputStream inputStream)
            throws IOException {
        final State root;
        final ArrayList<byte[]> input = readInput(inputStream);
        if (printProgress) {
            long partStart = this.partStart;
            startPart("Reading input");
            this.partStart = partStart;
        }
        endPart();

        if (crWarning) log("Warning: input contains carriage returns?");
        logInt("Input sequences", input.size());

        startPart("Sorting");
        Collections.sort(input, FSABuilder.LEXICAL_ORDERING);
        endPart();

        startPart("Building FSA");
        root = FSABuilder.build(input);
        endPart();
        return root;
    }

    /**
     * 
     */
    private State processSortedInput(InputStream inputStream)
            throws IOException {
        final State root;
        final FSABuilder builder = new FSABuilder();
        int lines = forAllLines(inputStream, new LineConsumer() {
            private byte [] current;
            private byte [] previous = null;
            private int previousLen;
            private int line;

            public byte[] process(byte[] current, int currentLen) {
                line++;

                // Verify the order.
                if (previous != null) {
                    if (FSABuilder.compare(previous, previousLen, current, currentLen) > 0) {
                        log("\n\nERROR: The input is not sorted: ");
                        log(dumpLine(previous, previousLen));
                        log(dumpLine(current, currentLen));
                        throw new TerminateProgramException("Input is not sorted.");
                    }
                }

                // Add to the automaton.
                builder.add(current, currentLen);
                
                // Swap buffers.
                this.current = previous != null ? previous : new byte [current.length];
                this.previous = current;
                this.previousLen = currentLen;

                return this.current;
            }
        });
        logInt("Input sequences", lines);

        root = builder.complete();

        if (printProgress) {
            long partStart = this.partStart;
            startPart("Building FSA");
            this.partStart = partStart;
        }
        endPart();
        return root;
    }

	/**
	 * Dump input line, byte-by-byte. 
	 */
    protected String dumpLine(byte[] line, int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0) builder.append(" ");
            builder.append(String.format("%02x", line[i]));
        }
        builder.append(" | ");
        for (int i = 0; i < length; i++) {
            if (Character.isLetterOrDigit(line[i]))
                builder.append((char) line[i]);
            else
                builder.append(".");
        }
        return builder.toString();
    }

    /**
	 * Parse input options.
	 */
	private void parseOptions(CommandLine line) {
	    String opt;

	    opt = SharedOptions.outputFormatOption.getOpt();
	    if (line.hasOption(opt)) {
            String formatValue = line.getOptionValue(opt);
            try {
                format = Format.valueOf(formatValue.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new TerminateProgramException("Not a valid format: " 
                        + formatValue);
            }
	    } else {
	        format = Format.FSA5;
	    }
        serializer = format.getSerializer();

		opt = SharedOptions.fillerCharacterOption.getLongOpt();
		if (line.hasOption(opt)) {
			String chr = line.getOptionValue(opt);
			checkSingleByte(chr);
			serializer.withFiller(chr.getBytes()[0]);
		}
		
		opt = SharedOptions.annotationSeparatorCharacterOption.getLongOpt();
		if (line.hasOption(opt)) {
			String chr = line.getOptionValue(opt);
			checkSingleByte(chr);
			serializer.withAnnotationSeparator(chr.getBytes()[0]);
		}

        opt = SharedOptions.withNumbersOption.getOpt();
        if (line.hasOption(opt)) {
            serializer.withNumbers();
        }
        
        opt = SharedOptions.progressOption.getLongOpt();
        if (line.hasOption(opt)) {
            printProgress = true;
        }
        
        opt = SharedOptions.inputSortedOption.getLongOpt();
        if (line.hasOption(opt)) {
            inputSorted = true;
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
	 * Read all the input lines, unsorted.
	 */
	private ArrayList<byte[]> readInput(InputStream is) throws IOException {
	    final ArrayList<byte[]> result = new ArrayList<byte[]>();
	    forAllLines(is, new LineConsumer() {
	        public byte[] process(byte[] buffer, int pos) {
	            result.add(Arrays.copyOf(buffer, pos));
	            return buffer;
	        }
        });
	    return result;
	}

	/**
	 * Apply line consumer to all non-empty lines.
	 */
	private int forAllLines(InputStream is, LineConsumer lineConsumer) throws IOException {
	    int lines = 0;
		byte[] buffer = new byte[0];
		int line = 0, b, pos = 0;
		while ((b = is.read()) != -1) {
			if (b == '\r' && !crWarning) {
				crWarning = true;
			}

			if (b == '\n') {
			    if (pos > 0) {
			        buffer = lineConsumer.process(buffer, pos);
	                pos = 0;
	                lines++;
			    }

				if (printProgress && (line++ % 10000) == 0) {
					log("Lines read: " + (line - 1));
				}
			} else {
				if (pos >= buffer.length) {
					buffer = Arrays.copyOf(buffer, buffer.length + 10);
				}
				buffer[pos++] = (byte) b;
			}
		}

		if (pos > 0) {
		    lineConsumer.process(buffer, pos);
		    lines++;
		}

		return lines;
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

		options.addOption(SharedOptions.outputFormatOption);
		
		options.addOption(SharedOptions.fillerCharacterOption);
		options.addOption(SharedOptions.annotationSeparatorCharacterOption);

		options.addOption(SharedOptions.withNumbersOption);
		options.addOption(SharedOptions.progressOption);
		
		options.addOption(SharedOptions.inputSortedOption);
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
		final FSABuildTool tool = new FSABuildTool();
		tool.go(args);
	}
}