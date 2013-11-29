package morfologik.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import morfologik.fsa.FSA5;
import morfologik.stemming.EncoderType;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringEscapeUtils;


/**
 * This utility converts the dictionary in a text (tabbed) format into 
 * the format accepted by the fsa building tools. It is meant to replace
 * the Perl and AWK scripts from the original FSA package. 
 */
class MorphEncodingTool extends Tool {
    private static Charset US_ASCII = Charset.forName("US-ASCII"); 
	private boolean noWarn = false;
	private SequenceAssembler encoder;
    private byte separatorByte;
    private char separator; 

	/**
     * 
     */
	protected void go(final CommandLine line) throws Exception {
		noWarn = line.hasOption(SharedOptions.noWarnIfTwoFields.getOpt());
	
		EncoderType encType = EncoderType.SUFFIX;
		if (line.hasOption(SharedOptions.encoder.getOpt())) {
		    String encValue = line.getOptionValue(SharedOptions.encoder.getOpt());
		    try {
		        encType = EncoderType.valueOf(encValue.toUpperCase());
		    } catch (IllegalArgumentException e) {
		        throw new IllegalArgumentException("Invalid encoder: " + encValue + ", "
		            + "allowed values: " + Arrays.toString(EncoderType.values()));
		    }
		}

		separator = FSA5.DEFAULT_ANNOTATION;
		if (line.hasOption(SharedOptions.annotationSeparatorCharacterOption.getLongOpt())) {
			String sep = line.getOptionValue(SharedOptions.annotationSeparatorCharacterOption.getLongOpt());

			// Decode escape sequences.
			sep = StringEscapeUtils.unescapeJava(sep);
			if (sep.length() != 1) {
			    throw new IllegalArgumentException("Field separator must be a single character: " + sep);
			}
			if (sep.charAt(0) > 0xff) {
			    throw new IllegalArgumentException("Field separator not within byte range: " + (int) sep.charAt(0));
			}
            separator = sep.charAt(0);
			separatorByte = FSABuildTool.checkSingleByte(Character.toString(separator), Charset.defaultCharset());
		}
		
        encoder = new SequenceAssembler(SequenceEncoders.forType(encType), (byte) separator);

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
	 * Process input stream, writing to output stream.
	 *  
	 */
	protected void process(final DataInputStream input, final DataOutputStream output)
	        throws IOException {
		long lnumber = 0;
		try {
			int bufPos = 0;
			byte[] buf = new byte[0];
			ArrayList<byte[]> columns = new ArrayList<byte[]>();
			int dataByte;
			do  {
			    dataByte = input.read();
			    switch (dataByte) {
			        case '\r':
			            // Ignore CR
			            continue;

			        case '\t':
			            columns.add(Arrays.copyOf(buf, bufPos));
			            bufPos = 0;
			            break;

			        case -1:
			            // Process EOF as if we encountered \n. fall-through.

			        case '\n':
                        lnumber++;
                        if (bufPos == 0 && columns.isEmpty()) {
                            if (dataByte != -1) {
                                System.err.println(String.format(Locale.ROOT, 
                                    "Ignoring empty line %d.", lnumber));
                            }
                            break;
	                    }

			            columns.add(Arrays.copyOf(buf, bufPos));

                        if (columns.size() < 2 || columns.size() > 3) {
                            throw new IllegalArgumentException(
                                String.format(Locale.ROOT, "Every \\n-delimited 'line' must contain 2 or 3 columns, line %d has %d. US-ASCII version of this line: %s",
                                    lnumber,
                                    columns.size(),
                                    toAscii(columns)));
                        }

                        if (columns.size() == 2 && !noWarn) {
                            System.err.println(String.format(Locale.ROOT, 
                                "Line %d has %d columns. US-ASCII version of this line: %s",
                                lnumber,
                                columns.size(),
                                toAscii(columns)));
                        }

                        byte [] wordForm = columns.get(0);
                        byte [] wordLemma = columns.get(1);
                        if (contains(wordForm, separatorByte) ||
                            contains(wordLemma, separatorByte)) {
                            throw new IllegalArgumentException(
                                String.format(Locale.ROOT,
                                    "Either word or lemma in line %d contain the annotation byte '%s': %s",
                                    lnumber,
                                    separator,
                                    toAscii(columns)));
                        }

                        output.write(encoder.encode(
                            wordForm, 
                            wordLemma, 
                            columns.size() > 2 ? columns.get(2) : null));

                        output.writeByte('\n');

                        bufPos = 0;
                        columns.clear();
			            break;

		            default:
	                    if (bufPos >= buf.length) {
	                        buf = Arrays.copyOf(buf, buf.length + 1024);
	                    }
	                    buf[bufPos++] = (byte) dataByte;
			    }
			} while (dataByte != -1);		
		} finally {
			input.close();
		}
	}

	private boolean contains(byte [] seq, byte b) {
        for (int i = 0; i < seq.length; i++) {
            if (seq[i] == b) return true;
        }
        return false;
    }

    private String toAscii(ArrayList<byte []> columns)
    {
	    StringBuilder b = new StringBuilder();
	    for (int i = 0; i < columns.size(); i++) {
            if (i > 0) b.append("\t");
	        b.append(new String(columns.get(i), US_ASCII));
	    }
        return b.toString();
    }

    /**
	 * Command line options for the tool.
	 */
	protected void initializeOptions(Options options) {
		options.addOption(SharedOptions.inputFileOption);		
		options.addOption(SharedOptions.outputFileOption);
		options.addOption(SharedOptions.encoder);
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
	public static void main(String... args) throws Exception {
		final MorphEncodingTool tool = new MorphEncodingTool();
		tool.go(args);
	}
}