package morfologik.tools;

import java.io.*;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import morfologik.stemming.*;

import org.apache.commons.cli.*;

/**
 * This utility parses input text, tokenizes it on whitespace and stems input
 * words, writing them to the output in column-based format:
 * 
 * <pre>
 * word   stem   form
 * word   stem   form
 * </pre>
 * 
 * Words for which no stems or forms are available have empty values in each
 * respective column. Columns are tab-delimited.
 */
class PolishStemmingTool extends Tool {
	/**
     * 
     */
	protected void go(CommandLine line) throws Exception {
		// Determine input/ output encoding.
		final String inputEncoding = getEncodingOption(line,
		        SharedOptions.inputEncodingOption.getOpt());

		final String outputEncoding = getEncodingOption(line,
		        SharedOptions.outputEncodingOption.getOpt());

		System.out.println("Input encoding: " + inputEncoding);
		System.out.println("Output encoding: " + outputEncoding);

		// Determine input and output streams.
		final Reader input = initializeInput(line, inputEncoding);
		final Writer output = initializeOutput(line, outputEncoding);

		final long start = System.currentTimeMillis();
		try {
			final long count = process(input, output);

			output.flush();

			final long millis = System.currentTimeMillis() - start;
			final double time = millis / 1000.0;
			final double wordsPerSec = time > 0 ? (count / time)
			        : Double.POSITIVE_INFINITY;
			System.out
			        .println(new MessageFormat(
			                "Processed {0} words in {1,number,#.###} seconds ({2,number,#} words per second).",
			                Locale.ENGLISH).format(new Object[] {
			                new Long(count), new Double(millis / 1000.0),
			                new Double(wordsPerSec) }));
		} finally {
			input.close();
			output.close();
		}

	}

	/**
	 * Process input stream, writing to output stream.
	 * 
	 * @return Returns the number of processed words.
	 */
	protected long process(Reader input, Writer output) throws IOException {
		final IStemmer stemmer = new PolishStemmer();
		final StreamTokenizer st = new StreamTokenizer(input);
		st.eolIsSignificant(false);
		st.wordChars('+', '+');

		long count = 0;
		int token;
		while ((token = st.nextToken()) != StreamTokenizer.TT_EOF) {
			if (token == StreamTokenizer.TT_WORD) {
				final String word = st.sval;

				count++;
				final List<WordData> stems = stemmer.lookup(word);
				if (stems.size() == 0) {
					output.write(word);
					output.write("\t-\t-\n");
				} else {
					for (WordData wd : stems) {
						output.write(word);
						output.write("\t");
						output.write(asString(wd.getStem()));
						output.write("\t");
						output.write(asString(wd.getTag()));
						output.write("\n");
					}
				}
			}
		}

		return count;
	}

	private String asString(CharSequence stem) {
		if (stem == null)
			return "-";
		return stem.toString();
	}

	/**
	 * Command line options for the tool.
	 */
	protected void initializeOptions(Options options) {
		options.addOption(SharedOptions.inputFileOption);
		options.addOption(SharedOptions.inputEncodingOption);
		options.addOption(SharedOptions.outputFileOption);
		options.addOption(SharedOptions.outputEncodingOption);
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
	private Reader initializeInput(CommandLine line, String inputEncoding)
	        throws IOException, ParseException {
		final Reader input;
		final String opt = SharedOptions.inputFileOption.getOpt();

		if (line.hasOption(opt)) {
			// Use input file.
			input = new InputStreamReader(
			        new BufferedInputStream(new FileInputStream((File) line
			                .getParsedOptionValue(opt))), inputEncoding);
		} else {
			// Use standard input.
			input = new InputStreamReader(System.in, inputEncoding);
		}
		return input;
	}

	/**
     *  
     */
	private String getEncodingOption(CommandLine line, String opt) {
		String encoding = System.getProperty("file.encoding", "iso-8859-1");
		if (line.hasOption(opt)) {
			encoding = line.getOptionValue(opt);
		}
		return encoding;
	}

	/*
	 * Check if the dictionary is available.
	 */
	@Override
	protected boolean isAvailable() {
		boolean available = true;
		try {
			new PolishStemmer();
		} catch (Throwable t) {
			available = false;
		}
		return available;
	}

	/**
	 * Command line entry point.
	 */
	public static void main(String[] args) throws Exception {
		final PolishStemmingTool tool = new PolishStemmingTool();
		tool.go(args);
	}
}