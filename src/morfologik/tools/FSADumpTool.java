package morfologik.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Iterator;

import morfologik.fsa.core.FSA;
import morfologik.fsa.core.FSAHelpers;
import morfologik.util.FileUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

/**
 * This utility will dump the information and contents of a given {@link FSA} dictionary.
 * 
 * @author Dawid Weiss
 */
public final class FSADumpTool extends BaseCommandLineTool {
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
     * Encoding of the dictionary
     */
    private String encoding;

    /**
     * The array where all labels on a path from start to final node are collected.
     * The buffer automatically expands up to {@link MAX_BUFFER_SIZE} when an exception is thrown.
     */
    private static byte[] word = new byte[BUFFER_INCREMENT];

    /**
     * 
     */
    protected void go(CommandLine line) throws Exception {
        final File fsaFile = (File) line.getOptionObject(
                CommandLineOptions.fsaDictionaryFileOption.getOpt());
        FileUtils.assertExists(fsaFile, true, false);

        final String encoding = line.getOptionValue(
                CommandLineOptions.characterEncodingOption.getOpt());

        assertEncodingSupported(encoding);

        final boolean useAPI = line.hasOption(
                CommandLineOptions.useApiOption.getOpt());
        
        dump(fsaFile, encoding, useAPI);
    }

    /**
     * Dumps the content of a dictionary to a file.
     */
    private void dump(File fsaFile, String encoding, boolean useAPI) 
        throws UnsupportedEncodingException, IOException
    {
        final long start = System.currentTimeMillis();
        final FSA fsa = FSA.getInstance(fsaFile, encoding);
        
        this.writer = System.out;
        this.encoding = encoding;

        writer.println("FSA file version    : " + fsa.getVersion());
        writer.println("Compiled with flags : " + FSAHelpers.flagsToString(fsa.getFlags()));
        writer.println("Number of arcs      : " + fsa.getNumberOfArcs());
        writer.println("Number of nodes     : " + fsa.getNumberOfNodes());
        writer.println("Annotation separator: " + fsa.getAnnotationSeparator());
        writer.println("Filler character    : " + fsa.getFillerCharacter());
        writer.println("--------------------");        

        if (useAPI) {
            final Iterator i = fsa.getTraversalHelper().getAllSubsequences(fsa.getStartNode()); 
            while (i.hasNext()) {
                final byte[] sequence = (byte[]) i.next();
                writer.println(new String(sequence, encoding));
            }
        } else {
            dumpNode(fsa.getStartNode(), 0);
        }            

        writer.println("--------------------");        

        final long millis = System.currentTimeMillis() - start;
        writer.println(MessageFormat.format("Dictionary dumped in {0,number,#.###} seconds.",
                new Object[] { new Double(millis / 1000.0) }));
    }

    /** 
     * Called recursively traverses the automaton.
     */
    public void dumpNode(FSA.Node node, int depth)
        throws UnsupportedEncodingException
    {
        FSA.Arc arc = node.getFirstArc();
        do {
            if (depth >= word.length) {
                if (word.length + BUFFER_INCREMENT > MAX_BUFFER_SIZE) {
                    throw new RuntimeException("Error: Buffer limit of " + MAX_BUFFER_SIZE
                            + " bytes exceeded. A loop in the automaton maybe?");
                }

                word = FSAHelpers.resizeByteBuffer(word, word.length + BUFFER_INCREMENT);

                // Redo the operation.
                word[depth] = arc.getLabel();
            }

            word[depth] = arc.getLabel();

            if (arc.isFinal()) {
                writer.println(new String(word, 0, depth + 1, encoding));
            }

            if (!arc.isTerminal()) {
                dumpNode(arc.getDestinationNode(), depth + 1);
            }

            arc = node.getNextArc(arc);
        } while (arc != null);
    }

    /**
     * Command line options for the tool.
     */
    protected void initializeOptions(Options options) {
        options.addOption(CommandLineOptions.fsaDictionaryFileOption);
        options.addOption(CommandLineOptions.characterEncodingOption);
        options.addOption(CommandLineOptions.useApiOption);
    }

    /** 
     * Program's entry point.
     */
    public static void main(String[] args) throws Exception {
        final FSADumpTool fsaDump = new FSADumpTool();
        fsaDump.go(args);
    }
}