package morfologik.tools;

import java.io.*;
import java.util.*;

import morfologik.fsa.*;

import org.apache.commons.cli.*;

import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.cursors.IntIntCursor;

/**
 * Convert from plain text input to a serialized FSA in any of the
 * available {@link Format}s.
 */
public final class FSABuildTool extends Tool {
    /**
     * One megabyte.
     */
    private final static int MB = 1024 * 1024;
    
    /**
     * The serialization format to use for the binary output.
     */
    public enum Format {
        FSA5, 
        CFSA2;

        public FSASerializer getSerializer() {
            switch (this) {
                case FSA5:
                    return new FSA5Serializer();

                case CFSA2:
                    return new CFSA2Serializer();

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
     * Print additional statistics about the output automaton.
     */
    private boolean statistics;

    /**
     * The actual construction of the FSA.
     */
    private FSABuilder builder = new FSABuilder();
    
    /**
     * Start time.
     */
    private long start = System.currentTimeMillis();

    private IMessageLogger logger;

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

        logger = new WriterMessageLogger(new PrintWriter(System.err));
        this.serializer.withLogger(logger);

		try {
		    InputStream inputStream = initializeInput(line);

            if (inputSorted) {
                logger.log("Assuming input is already sorted");
            }

		    final FSA fsa;
		    if (inputSorted) {
                fsa = processSortedInput(inputStream);
		    } else {
		        fsa = processUnsortedInput(inputStream);
		    }
	        if (crWarning) logger.log("Warning: input contained carriage returns?");

            if (statistics) {
                logger.startPart("Statistics");
                FSAInfo info = new FSAInfo(fsa);
                TreeMap<Integer, Integer> fanout = FSAUtils.calculateFanOuts(fsa, fsa.getRootNode());
                logger.endPart();

                final IntIntOpenHashMap numbers = new IntIntOpenHashMap();
                fsa.visitInPostOrder(new StateVisitor() {
                    public boolean accept(int state) {
                        int thisNodeNumber = 0;
                        for (int arc = fsa.getFirstArc(state); arc != 0; arc = fsa.getNextArc(arc)) {
                            thisNodeNumber +=
                                (fsa.isArcFinal(arc) ? 1 : 0) +
                                (fsa.isArcTerminal(arc) ? 0 : numbers.get(fsa.getEndNode(arc)));
                        }
                        numbers.put(state, thisNodeNumber);
                        return true;
                    }
                });

                int singleRLC = 0;
                for (IntIntCursor c : numbers) {
                    if (c.value == 1) singleRLC++;
                }
                
                logger.log("Nodes", info.nodeCount);
                logger.log("Arcs", info.arcsCount);
                logger.log("Tail nodes", singleRLC);

                logger.log("States with the given # of outgoing arcs:");
                for (Map.Entry<Integer, Integer> e : fanout.entrySet()) {
                    logger.log("  #" + e.getKey(), e.getValue());
                }

                logger.log("FSA builder properties:");
                for (Map.Entry<FSABuilder.InfoEntry, Object> e : builder.getInfo().entrySet()) {
                    logger.log(e.getKey().toString(), e.getValue());
                }
            }

			// Save the result.
            logger.startPart("Serializing " + format);
			serializer.serialize(fsa, initializeOutput(line)).close();
			logger.endPart();
		} catch (OutOfMemoryError e) {
		    logger.log("Error: Out of memory. Pass -Xmx1024m argument (or more) to java.");
		}
	}

	/**
	 * Process unsorted input (sort and construct FSA).
	 */
    private FSA processUnsortedInput(InputStream inputStream)
            throws IOException {
        final FSA root;
        logger.startPart("Reading input");
        final ArrayList<byte[]> input = readInput(inputStream);
        logger.endPart();

        logger.log("Input sequences", input.size());

        logger.startPart("Sorting");
        Collections.sort(input, FSABuilder.LEXICAL_ORDERING);
        logger.endPart();

        logger.startPart("Building FSA");
        for (byte [] bb : input)
            builder.add(bb, 0, bb.length);
        root = builder.complete();
        logger.endPart();
        return root;
    }

    /**
     * 
     */
    private FSA processSortedInput(InputStream inputStream)
            throws IOException {

        int lines = forAllLines(inputStream, new LineConsumer() {
            private byte [] current;
            private byte [] previous = null;
            private int previousLen;
            private int line;

            public byte[] process(byte[] current, int currentLen) {
                line++;

                // Verify the order.
                if (previous != null) {
                    if (FSABuilder.compare(previous, 0, previousLen, current, 0, currentLen) > 0) {
                        logger.log("\n\nERROR: The input is not sorted: \n" + 
                                dumpLine(previous, previousLen) + "\n" +
                                dumpLine(current, currentLen));
                        throw new TerminateProgramException("Input is not sorted.");
                    }
                }

                // Add to the automaton.
                builder.add(current, 0, currentLen);

                // Swap buffers.
                this.current = previous != null ? previous : new byte [current.length];
                this.previous = current;
                this.previousLen = currentLen;

                return this.current;
            }
        });

        logger.startPart("Building FSA");
        FSA fsa = builder.complete();
        logger.endPart();
        logger.log("Input sequences", lines);

        return fsa;
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
		if (line.hasOption(opt) && requiredCapability(opt, FSAFlags.SEPARATORS)) {
			String chr = line.getOptionValue(opt);
			checkSingleByte(chr);
			serializer.withFiller(chr.getBytes()[0]);
		}
		
		opt = SharedOptions.annotationSeparatorCharacterOption.getLongOpt();
		if (line.hasOption(opt) && requiredCapability(opt, FSAFlags.SEPARATORS)) {
			String chr = line.getOptionValue(opt);
			checkSingleByte(chr);
			serializer.withAnnotationSeparator(chr.getBytes()[0]);
		}

        opt = SharedOptions.withNumbersOption.getOpt();
        if (line.hasOption(opt) && requiredCapability(opt, FSAFlags.NUMBERS)) {
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

        opt = SharedOptions.statistics.getLongOpt();
        if (line.hasOption(opt)) {
            statistics = true;
        }
    }
	
	private boolean requiredCapability(String opt, FSAFlags flag) {
	    if (!serializer.getFlags().contains(flag)) {
	        throw new RuntimeException("This serializer does not support option: " + opt);
	    }
	    return true;
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
	 * Read all the input lines, unsorted.
	 */
	private ArrayList<byte[]> readInput(InputStream is) throws IOException {
	    final ArrayList<byte[]> result = new ArrayList<byte[]>();
	    forAllLines(is, new LineConsumer() {
	        public byte[] process(byte[] buffer, int pos) {
	            result.add(java.util.Arrays.copyOf(buffer, pos));
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

				if (printProgress && line++ > 0 && (line % 1000000) == 0) {
				    logger.log(String.format(Locale.ENGLISH, "%6.2fs, sequences: %d", elapsedTime(), line));
				}
			} else {
				if (pos >= buffer.length) {
					buffer = java.util.Arrays.copyOf(buffer, buffer.length + 10);
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

	private double elapsedTime() {
        return (System.currentTimeMillis() - start) / 1000.0d;
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

		options.addOption(SharedOptions.statistics);
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
	private InputStream initializeInput(CommandLine line)
	        throws IOException, ParseException {
		final InputStream input;
		final String opt = SharedOptions.inputFileOption.getOpt();

		if (line.hasOption(opt)) {
			// Use input file.
			File inputFile = (File) line.getParsedOptionValue(opt);
			if (!inputSorted && inputFile.length() > 20 * MB) {
			    logger.log("WARN: The input file is quite large, avoid\n" +
			        "      in-memory sorting by piping pre-sorted\n" +
			        "      input directly to fsa_build. Linux:\n" +
			        "      export LC_ALL=C && \\\n" +
			        "         sort input | \\\n" +
			        "         java -jar morfologik.jar fsa_build --sorted -o dict.fsa");
			}

            input = new FileInputStream(inputFile);
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