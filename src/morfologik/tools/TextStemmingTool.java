package morfologik.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Locale;

import morfologik.stemmers.IStemmer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * <p>This utility parses input text, tokenizes it on whitespace
 * and stems input words, writing them to the output in column-based
 * format:
 * 
 * <pre>
 * word   stem   form
 * word   stem   form
 * </pre>
 *
 * <p>Words for which no stems or forms are available have empty
 * values in each respective column.
 * 
 * <p>Columns are tab-delimited.
 *
 * @author Dawid Weiss
 */
class TextStemmingTool extends BaseCommandLineTool {
    /**
     * The stemmer to use.
     */
    private final IStemmer stemmer;

    /**
     * 
     */
    public TextStemmingTool(IStemmer stemmer) {
        this.stemmer = stemmer;
    }
    
    /**
     * 
     */
    protected void go(CommandLine line) throws Exception {
        // Determine input/ output encoding.
        final String inputEncoding = getEncodingOption(
                line, CommandLineOptions.inputEncodingOption.getOpt());

        final String outputEncoding = getEncodingOption(
                line, CommandLineOptions.outputEncodingOption.getOpt());

        logger.info("Input encoding: " + inputEncoding);
        logger.info("Output encoding: " + outputEncoding);

        // Determine input and output streams.
        final Reader input = initializeInput(line, inputEncoding);
        final Writer output = initializeOutput(line, outputEncoding);

        final long start = System.currentTimeMillis();
        try {
            final long count = process(input, output);

            output.flush();

            final long millis = System.currentTimeMillis() - start;
            final double time = millis / 1000.0;
            final double wordsPerSec = time > 0 ? (count / time) : Double.POSITIVE_INFINITY;
            logger.info(
                    new MessageFormat("Processed {0} words in {1,number,#.###} seconds ({2,number,#} words per second).", Locale.ENGLISH)
                        .format(new Object[] {
                                new Long(count), 
                                new Double(millis / 1000.0),
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
    protected long process(Reader input, Writer output) 
        throws IOException
    {
        final StreamTokenizer st = new StreamTokenizer(input);
        st.eolIsSignificant(false);
        st.wordChars('+', '+');

        long count = 0;
        int token;
        while ((token = st.nextToken()) != StreamTokenizer.TT_EOF) {
            if (token == StreamTokenizer.TT_WORD) {
                final String word = st.sval;

                count++;
                final String[] stems = stemmer.stemAndForm(word);
                if (stems == null || stems.length == 0) {
                    output.write(word);
                    output.write("\t-\t-\n");
                } else {
                    for (int i = 0; i < stems.length; i += 2) {
                        output.write(word);
                        output.write("\t");
                        output.write(stems[i] != null ? stems[i] : "-");
                        output.write("\t");
                        output.write(stems[i + 1] != null ? stems[i + 1] : "-");
                        output.write("\n");
                    }
                }
            }
        }
        
        return count;
    }

    /**
     * Command line options for the tool.
     */
    protected void initializeOptions(Options options) {
        options.addOption(CommandLineOptions.inputFileOption);
        options.addOption(CommandLineOptions.inputEncodingOption);
        options.addOption(CommandLineOptions.outputFileOption);
        options.addOption(CommandLineOptions.outputEncodingOption);
    }

    /**
     * 
     */
    private Writer initializeOutput(CommandLine line, String outputEncoding)
        throws IOException
    {
        final Writer output;
        final String opt = CommandLineOptions.outputFileOption.getOpt();
        if (line.hasOption(opt)) {
            // Use output file.
            output = new OutputStreamWriter(
                    new BufferedOutputStream(
                            new FileOutputStream((File) line.getOptionObject(opt))),
                    outputEncoding);
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
        throws IOException 
    {
        final Reader input;
        final String opt = CommandLineOptions.inputFileOption.getOpt();

        if (line.hasOption(opt)) {
            // Use input file.
            input = new InputStreamReader(
                    new BufferedInputStream(
                            new FileInputStream((File) line.getOptionObject(opt))),
                    inputEncoding);
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
}