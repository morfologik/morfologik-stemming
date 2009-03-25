package morfologik.tools;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.text.MessageFormat;

import morfologik.fsa.FSA;
import morfologik.fsa.FSAHelpers;
import morfologik.stemming.Dictionary;
import morfologik.util.Arrays;
import morfologik.util.FileUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * This utility will dump the information and contents of a given {@link FSA}
 * dictionary.
 */
public final class FSADumpTool extends Tool {
    /**
     * @see #word
     */
    private static final int MAX_BUFFER_SIZE = 5000;

    /**
     * @see #word
     */
    private static final int BUFFER_INCREMENT = 1000;

    /**
     * Writer used to print messages and dictionary dump.
     */
    private PrintStream writer;

    /**
     * Encoding of the symbols inside the FSA.
     */
    private String encoding;

    /**
     * The array where all labels on a path from start to final node are
     * collected. The buffer automatically expands up to {@link MAX_BUFFER_SIZE}
     * when an exception is thrown.
     */
    private static byte[] word = new byte[BUFFER_INCREMENT];

    /**
     * 
     */
    protected void go(CommandLine line) throws Exception {
	final File dictionaryFile = (File) line
		.getParsedOptionValue(SharedOptions.fsaDictionaryFileOption
			.getOpt());
	FileUtils.assertExists(dictionaryFile, true, false);

	final boolean useAPI = line.hasOption(SharedOptions.useApiOption
		.getOpt());

	dump(dictionaryFile, useAPI);
    }

    /**
     * Dumps the content of a dictionary to a file.
     */
    private void dump(File dictionaryFile, boolean useAPI)
	    throws UnsupportedEncodingException, IOException {
	final long start = System.currentTimeMillis();

	final Dictionary dictionary;
	final FSA fsa;

	if (!dictionaryFile.canRead()) {
	    writer.println("Dictionary file does not exist: "
		    + dictionaryFile.getAbsolutePath());
	    return;
	}

	this.writer = System.out;

	if (hasFeatures(dictionaryFile)) {
	    dictionary = Dictionary.read(dictionaryFile);
	    fsa = dictionary.fsa;
	    this.encoding = dictionary.metadata.encoding;

	    if (!Charset.isSupported(encoding)) {
		writer
			.println("Dictionary's charset is not supported on this JVM: "
				+ encoding);
		return;
	    }
	} else {
	    encoding = null;
	    dictionary = null;
	    fsa = FSA.getInstance(dictionaryFile, "iso-8859-1");
	    writer
		    .println("Warning: FSA automaton without metadata file. Raw bytes emitted.");
	}

	writer.println("FSA properties");
	writer.println("--------------------");
	writer.println("FSA file version    : " + fsa.getVersion());
	writer.println("Compiled with flags : "
		+ FSAHelpers.flagsToString(fsa.getFlags()));
	writer.println("Number of arcs      : " + fsa.getNumberOfArcs());
	writer.println("Number of nodes     : " + fsa.getNumberOfNodes());
	writer.println("Annotation separator: " + fsa.getAnnotationSeparator());
	writer.println("Filler character    : " + fsa.getFillerCharacter());
	writer.println("");

	if (dictionary != null) {
	    final CharsetDecoder decoder = Charset.forName(encoding)
		    .newDecoder();
	    decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
	    decoder.onMalformedInput(CodingErrorAction.REPORT);
	    writer.println("Dictionary metadata");
	    writer.println("--------------------");
	    writer.println("Encoding            : "
		    + dictionary.metadata.encoding);
	    writer
		    .println("Separator           : "
			    + decoder
				    .decode(ByteBuffer
					    .wrap(new byte[] { dictionary.metadata.separator })));
	    writer.println("Uses prefixes       : "
		    + dictionary.metadata.usesPrefixes);
	    writer.println("Uses infixes        : "
		    + dictionary.metadata.usesInfixes);
	    writer.println("");
	}

	writer.println("FSA data");
	writer.println("--------------------");

	if (useAPI) {
	    if (encoding != null) {
		for (ByteBuffer bb : fsa) {
		    writer.println(new String(bb.array(), 0, bb.remaining(),
			    encoding));
		}
	    } else {
		for (ByteBuffer bb : fsa) {
		    writer.write(bb.array(), 0, bb.remaining());
		    writer.println();
		}
	    }
	} else {
	    dumpNode(fsa, fsa.getRootNode(), 0);
	}

	writer.println("--------------------");

	final long millis = System.currentTimeMillis() - start;
	writer.println(MessageFormat.format(
		"Dictionary dumped in {0,number,#.###} seconds.",
		new Object[] { new Double(millis / 1000.0) }));
    }

    /**
     * Called recursively traverses the automaton.
     */
    private void dumpNode(FSA fsa, int node, final int depth)
	    throws UnsupportedEncodingException {
	int arc = fsa.getFirstArc(node);
	do {
	    if (depth >= word.length) {
		if (word.length + BUFFER_INCREMENT > MAX_BUFFER_SIZE) {
		    throw new RuntimeException("Error: Buffer limit of "
			    + MAX_BUFFER_SIZE
			    + " bytes exceeded. A loop in the automaton maybe?");
		}

		word = Arrays.copyOf(word, word.length + BUFFER_INCREMENT);

		// Redo the operation.
		word[depth] = fsa.getArcLabel(arc);
	    }

	    word[depth] = fsa.getArcLabel(arc);

	    if (fsa.isArcFinal(arc)) {
		if (encoding != null) {
		    writer.println(new String(word, 0, depth + 1, encoding));
		} else {
		    writer.write(word, 0, depth + 1);
		    writer.println();
		}
	    }

	    if (fsa.isArcTerminal(arc)) {
		dumpNode(fsa, fsa.getEndNode(arc), depth + 1);
	    }

	    arc = fsa.getNextArc(node, arc);
	} while (arc != 0);
    }

    /**
     * Check if there is a metadata file for the given FSA automaton.
     */
    private static boolean hasFeatures(File fsaFile) {
	final File featuresFile = new File(fsaFile.getParent(), Dictionary
		.getExpectedFeaturesName(fsaFile.getName()));

	return featuresFile.canRead();
    }

    /**
     * Command line options for the tool.
     */
    protected void initializeOptions(Options options) {
	options.addOption(SharedOptions.fsaDictionaryFileOption);
	options.addOption(SharedOptions.useApiOption);
    }

    /**
     * Program's entry point.
     */
    public static void main(String[] args) throws Exception {
	final FSADumpTool fsaDump = new FSADumpTool();
	fsaDump.go(args);
    }
}