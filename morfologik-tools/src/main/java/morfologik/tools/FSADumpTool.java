package morfologik.tools;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import morfologik.fsa.FSA;
import morfologik.fsa.FSA5;
import morfologik.fsa.FSAInfo;
import morfologik.fsa.FSAUtils;
import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryAttribute;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.WordData;
import morfologik.util.FileUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * This utility will dump the information and contents of a given {@link FSA}
 * dictionary. It can dump dictionaries in the raw form (as fed to the
 * <code>fsa_build</code> program) or decoding compressed stem forms.
 */
public final class FSADumpTool extends Tool {
	/**
	 * Direct binary stream used for dictionary dumps.
	 */
	private OutputStream os;

	/**
     * A writer for messages and any text-based output.
     */
    private Writer w;

	/**
	 * Print raw data only, no headers.
	 */
	private boolean dataOnly;

	/**
	 * Decode from prefix/infix/suffix encodings. 
	 */
	private boolean decode;

	/**
	 * Dump graphviz DOT file instead of automaton sequences.
	 */
	private boolean dot;

	/**
	 * Command line entry point after parsing arguments.
	 */
	protected void go(CommandLine line) throws Exception {
		final File dictionaryFile = (File) line
		        .getParsedOptionValue(SharedOptions.fsaDictionaryFileOption
		                .getOpt());

		dataOnly = line.hasOption(SharedOptions.dataOnly.getOpt());
		decode = line.hasOption(SharedOptions.decode.getOpt());
		dot = line.hasOption(SharedOptions.dot.getLongOpt());

		FileUtils.assertExists(dictionaryFile, true, false);

		dump(dictionaryFile);
	}

	/**
	 * Dumps the content of a dictionary to a file.
	 */
	public String dump(File dictionaryFile)
	        throws UnsupportedEncodingException, IOException {
		
		String dictAsString = "";
		
		final long start = System.currentTimeMillis();

		final Dictionary dictionary;
		final FSA fsa;

		if (!dictionaryFile.canRead()) {
			printWarning("Dictionary file does not exist: "
			        + dictionaryFile.getAbsolutePath());
			return;
		}

		this.os = new BufferedOutputStream(System.out, 1024 * 32);
		this.w =  new OutputStreamWriter(os, "UTF-8");

		File metadataFile = expectedMetadataFile(dictionaryFile).getAbsoluteFile();
		if (metadataFile.exists() &&
		    metadataFile.isFile() &&
		    metadataFile.canRead()) {
			dictionary = Dictionary.read(dictionaryFile);
			fsa = dictionary.fsa;

			final String encoding = dictionary.metadata.getEncoding();
			if (!Charset.isSupported(encoding)) {
				printWarning("Dictionary's charset is not supported "
				        + "on this JVM: " + encoding);
				return;
			}
		} else {
			dictionary = null;
			fsa = FSA.read(new FileInputStream(dictionaryFile));
			printWarning("Warning: FSA automaton without metadata *.info file. The metadata file was" +
			             " expected at: " + metadataFile);
		}

		printExtra("FSA properties");
		printExtra("--------------");
		printExtra("FSA implementation     : " + fsa.getClass().getName());
		printExtra("Compiled with flags    : " + fsa.getFlags().toString());

		if (!dataOnly) {
    		final FSAInfo info = new FSAInfo(fsa);
    		printExtra("Number of arcs         : " + info.arcsCount + "/" + info.arcsCountTotal);
    		printExtra("Number of nodes        : " + info.nodeCount);
    		printExtra("Number of final states : " + info.finalStatesCount);
    		printExtra("");
		}

		// Separator for dumping.
		char separator = '\t';

		if (fsa instanceof FSA5) {
			printExtra("FSA5 properties");
			printExtra("---------------");
			printFSA5((FSA5) fsa);			
			printExtra("");
		}

		if (dictionary != null) {
			printExtra("Dictionary metadata");
            printExtra("-------------------");
            
            Map<DictionaryAttribute,String> values =
                new LinkedHashMap<DictionaryAttribute,String>(dictionary.metadata.getAttributes());
            values.put(DictionaryAttribute.ENCODING, dictionary.metadata.getEncoding());
            values.put(DictionaryAttribute.SEPARATOR, "0x"
                + Integer.toHexString(dictionary.metadata.getSeparator())
                + " ('" + dictionary.metadata.getSeparatorAsChar() + "')");

            for (Map.Entry<DictionaryAttribute,String> e : values.entrySet()) {
                printExtra(String.format(Locale.ENGLISH,
                    "%-40s: %s",
                    e.getKey().propertyName,
                    e.getValue()));
            }
            printExtra("");
		}

		int sequences = 0;
		if (decode) {
			if (dictionary == null) {
				printWarning("No dictionary metadata available.");
				return;
			}

			printExtra("Decoded FSA data (in the encoding above)");
			printExtra("----------------------------------------");

			final DictionaryLookup dl = new DictionaryLookup(dictionary);
			final StringBuilder builder = new StringBuilder();
			final OutputStreamWriter osw = new OutputStreamWriter(os, dictionary.metadata.getEncoding());

			CharSequence t;
			for (WordData wd : dl) {
				builder.setLength(0);
				builder.append(wd.getWord());
				builder.append(separator);

				t = wd.getStem();
				if (t == null)
					t = "";
				builder.append(t);
				builder.append(separator);

				t = wd.getTag();
				if (t == null)
					t = "";
				builder.append(t);
				builder.append('\n');
				
				dictAsString = builder.toString();
				osw.write(dictAsString);
				sequences++;
			}
			osw.flush();
		} else {
			if (dot) {
				FSAUtils.toDot(w, fsa, fsa.getRootNode());
				w.flush();
			} else {
    			printExtra("FSA data (raw bytes in the encoding above)");
    			printExtra("------------------------------------------");
    
    			for (ByteBuffer bb : fsa) {
    				os.write(bb.array(), 0, bb.remaining());
    				os.write(0x0a);
    				sequences++;
    			}
			}
		}

		printExtra("--------------------");

		final long millis = Math.max(1, System.currentTimeMillis() - start);
		printExtra(String
		        .format(
		                Locale.ENGLISH,
		                "Dictionary dumped in %.3f second(s), %d sequences (%d sequences/sec.).",
		                millis / 1000.0, sequences,
		                (int) (sequences / (millis / 1000.0))));

		os.flush();
		return dictAsString;
	}

	/**
	 * Print {@link FSA5}-specific stuff.
	 */
	private void printFSA5(FSA5 fsa) throws IOException {
		printExtra("GTL                    : " + fsa.gtl);
		printExtra("Node extra data        : " + fsa.nodeDataLength);
		printExtra("Annotation separator   : " + byteAsChar(fsa.annotation));
		printExtra("Filler character       : " + byteAsChar(fsa.filler));
    }

	/**
	 * Convert a byte to a character, no charset decoding, simple ASCII range mapping.
	 */
	private char byteAsChar(byte v) {
		char chr = (char) (v & 0xff);
		if (chr < 127)
			return chr;
		else
			return '?';
    }

	/*
     * 
     */
	private void printExtra(String msg) throws IOException {
		if (dataOnly)
			return;
		w.write(msg);
		w.write('\n');
		w.flush();
	}

	/*
     * 
     */
	private void printWarning(String msg) {
		System.err.println(msg);
	}

	/**
	 * Check if there is a metadata file for the given FSA automaton.
	 */
	private static File expectedMetadataFile(File fsaFile) {
		return new File(fsaFile.getParent(), Dictionary.getExpectedFeaturesName(fsaFile.getName()));
	}

	/**
	 * Command line options for the tool.
	 */
	protected void initializeOptions(Options options) {
		options.addOption(SharedOptions.fsaDictionaryFileOption);
		options.addOption(SharedOptions.dataOnly);
		options.addOption(SharedOptions.decode);
		options.addOption(SharedOptions.dot);
	}

	/**
	 * Command line entry point.
	 */
	public static void main(String... args) throws Exception {
		final FSADumpTool fsaDump = new FSADumpTool();
		fsaDump.go(args);
	}
}