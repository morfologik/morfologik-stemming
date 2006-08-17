
package com.dawidweiss.fsa.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Iterator;

import com.dawidweiss.fsa.FSA;
import com.dawidweiss.fsa.FSAMatch;
import com.dawidweiss.fsa.FSATraverseHelper;


/**
 * This utility will look for a word in the given dictionary and print a pair:
 * <code>word:[yes|no|partial]:suffix</code>
 * for each word.
 *
 * Words to be checked against the dictionary are read from standard input. A summary is printed to
 * standard error stream at the end of processing.
 *
 * @author Dawid Weiss
 */
public final class FSAFind
{
    // The character encoding used for converting labels to unicode.
    private String encoding;


    /** Program's entry point. */
    public static void main( String [] args )
        throws Exception
    {
        FSAFind program = new FSAFind();
        program.execute( args );
    }


    /** Read and dump the contents of an automaton. */
    public void execute( String [] args )
        throws IOException
    {
        if (args.length == 0 || args.length > 2)
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

        FSA fsa = FSA.getInstance( fsaFile, encoding );
        FSATraverseHelper fsaMatcher = fsa.getTraverseHelper();

        long start = System.currentTimeMillis();

        StreamTokenizer st = new StreamTokenizer( new InputStreamReader( System.in, encoding ) );
        st.eolIsSignificant(false);
        st.wordChars('+','+');

outerLoop:
        while (true)
        {
            switch (st.nextToken())
            {
                case StreamTokenizer.TT_EOF  :
                    break outerLoop;
                case StreamTokenizer.TT_WORD :
                    String word = st.sval;
                    FSAMatch m = fsaMatcher.matchSequence( word.getBytes( encoding ), fsa );

                    switch (m.getMatchResult())
                    {
                        case FSAMatch.EXACT_MATCH:
                            System.out.println(word + ": yes");
                            break;
                        case FSAMatch.PREFIX_FOUND:
                            System.out.println(word + ": no : prefix found \""+ word.substring(0, m.getMismatchIndex()) +"\"");
                            break;
                        case FSAMatch.PREMATURE_PATH_END_FOUND:
                            System.out.println(word + ": no : dictionary has a prefix: \"" + word.substring(0, m.getMismatchIndex())+ "\"");
                            break;
                        case FSAMatch.PREMATURE_WORD_END_FOUND:
                            System.out.println(word + ": no : dictionary has these sequences starting with the sequence:");
                            for (Iterator i=fsaMatcher.getAllSubsequences( m.getMismatchNode() ); i.hasNext();)
                            {
                                byte [] sequence = (byte []) i.next();
                                System.out.println( "\t" + new String(sequence, encoding) );
                            }
                            break;
                    }

                    break;
            }
        }

        long millis = System.currentTimeMillis() - start;

        System.err.println(
            MessageFormat.format("It took {0,number,#.###} seconds to dump the dictionary.", new Object[] { new Float( millis / 1000.0) }));
    }


    /** Prints the usage info. */
    public void usage(String message)
    {
        if (message != null)
        {
            System.err.println("Error: " + message);
        }

        System.err.println("Usage: java FSAFind <dictionaryFile> [Byte2StringEncoding]");
    }
}
