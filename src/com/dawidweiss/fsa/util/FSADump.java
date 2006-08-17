
package com.dawidweiss.fsa.util;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Iterator;

import com.dawidweiss.fsa.FSA;
import com.dawidweiss.fsa.FSAHelpers;


/**
 * This utility will dump the contents of a dictionary to standard output.
 * Also, some information about the FSA file is written to stderr (the version,
 * flags, number of nodes and such).
 *
 * @author Dawid Weiss
 */
public final class FSADump
{
    // The array where all labels on a path from start to final node are collected.
    // The buffer automatically expands up to MAX_BUFFER_SIZE when an exception
    // is thrown.
    static final int MAX_BUFFER_SIZE  = 5000;
    static final int BUFFER_INCREMENT = 1000;
    static       byte [] word = new byte [BUFFER_INCREMENT];

    // The character encoding used for converting labels to unicode.
    private String encoding;


    /** Program's entry point. */
    public static void main( String [] args )
        throws Exception
    {
        FSADump fsaDump = new FSADump();
        fsaDump.execute( args );
    }


    /** Read and dump the contents of an automaton. */
    public void execute( String [] args )
        throws IOException
    {
        if (args.length == 0 || args.length > 3)
        {
            usage("Wrong number of arguments.");
            return;
        }

        File fsaFile = new File( args[0] );

        if (!fsaFile.exists())
        {
            usage("FSA file does not exist: " + fsaFile.getCanonicalPath());
            return;
        }

        encoding = "iso8859-1";
        if (args.length>1)
        {
            try
            {
                if (new String("testEncoding".getBytes( args[1] ), args[1]).equals("testEncoding"))
                {
                    encoding = args[1];
                }
            }
            catch (UnsupportedEncodingException e)
            {
                usage("This encoding is not supported: " + args[2]);
                return;
            }
        }

        // dumps the dictionary using standard FSATraverseHelper api.
        boolean apidump = false;
        if (args.length>2)
        {
            if (args[2].equals("apidump"))
            {
                apidump = true;
            }
        }

        long start = System.currentTimeMillis();
        FSA fsa = FSA.getInstance( fsaFile, encoding );

        System.err.println("FSA file version    : " + fsa.getVersion());
        System.err.println("Compiled with flags : " + FSAHelpers.flagsToString(fsa.getFlags())  );
        System.err.println("Number of arcs      : " + fsa.getNumberOfArcs() );
        System.err.println("Number of nodes     : " + fsa.getNumberOfNodes() );

        if (apidump==false)
            dumpNode( fsa.getStartNode() , 0 );
        else
        {
            for (Iterator i=fsa.getTraverseHelper().getAllSubsequences( fsa.getStartNode() ); i.hasNext();)
            {
                byte [] sequence = (byte []) i.next();
                System.out.println( new String(sequence, encoding) );
            }
        }

        long millis = System.currentTimeMillis() - start;

        System.err.println(
            MessageFormat.format("It took {0,number,#.###} seconds to dump the dictionary.", new Object[] { new Float( millis / 1000.0) }));
    }


    /** Called recursively traverses the automaton. */
    public void dumpNode( FSA.Node node, int depth )
        throws UnsupportedEncodingException
    {
        FSA.Arc arc = node.getFirstArc();

        do
        {
            try
            {
                word[depth] = arc.getLabel();
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                if (word.length + BUFFER_INCREMENT > MAX_BUFFER_SIZE)
                {
                    System.err.println("Error: Buffer limit of " + MAX_BUFFER_SIZE + " bytes exceeded. A loop in the automaton maybe?");
                    System.exit(0);
                }

                System.err.println("Warning: A path in the automaton exceeded " + word.length + " bytes. Expanding buffer.");
                byte [] newbuffer = new byte [ word.length + BUFFER_INCREMENT ];
                System.arraycopy( word, 0, newbuffer, 0, word.length);
                word = newbuffer;

                // redo the operation.
                word[depth] = arc.getLabel();
            }

            if (arc.pointsToFinalNode())
            {
                System.out.println( new String( word, 0, depth+1, encoding ) );
            }
            else
            {
                dumpNode( arc.getDestinationNode(), depth+1 );
            }

            arc = node.getNextArc(arc);
        } while (arc!=null);
    }


    /** Prints the usage info. */
    public void usage(String message)
    {
        if (message != null)
        {
            System.err.println("Error: " + message);
        }

        System.err.println("Usage: java FSADump <dictionaryFile> [Byte2StringEncoding] [apidump]");
    }
}
