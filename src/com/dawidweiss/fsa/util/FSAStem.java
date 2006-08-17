
package com.dawidweiss.fsa.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

import com.dawidweiss.fsa.FSA;


/**
 * This utility will apply "dictionary stemming" to the words found in the input.
 *
 * @author Dawid Weiss
 */
public final class FSAStem
{
    // The character encoding used for converting labels to unicode.
    private String encoding;


    /** Program's entry point. */
    public static void main( String [] args )
        throws Exception
    {
        FSAStem fsaStem = new FSAStem();
        fsaStem.execute( args );
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

        final File fsaFile = new File(args[0]);
        if (!fsaFile.exists())
        {
            usage("FSA file does not exist: " + fsaFile.getCanonicalPath());
            return;
        }

        int index = 1;
        boolean showForms = false;
        encoding = "iso8859-1";

        while (index < args.length) {
	        if ("-forms".equals(args[index])) {
	        	showForms = true;
	        	index++;
	        	continue;
	        } else {
		        try
		        {
		            if (new String("testEncoding".getBytes(args[index]), args[index]).equals("testEncoding"))
		            {
		                encoding = args[index];
		            }
		        }
		        catch (UnsupportedEncodingException e)
		        {
		            usage("This encoding is not supported: " + args[index]);
		            return;
		        }
	        }
        }

        // Stems the word on the input.

        final FSA fsa = FSA.getInstance(fsaFile, encoding);
        final FSAStemmer stemmer = new FSAStemmer(fsa, encoding, '+');

        long start = System.currentTimeMillis();

        final StreamTokenizer st = new StreamTokenizer(new BufferedReader(new InputStreamReader(System.in, encoding)));
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
                    System.out.print( st.sval + "\t");

                    final String [] stems;
                    if (showForms) {
                    	stems = stemmer.stemAndForm(st.sval);
                    } else {
                    	stems = stemmer.stem(st.sval);
                    }
                    if (stems.length == 0)
                    {
                        System.out.println("-");
                    }
                    else
                    {
                        if (showForms) {
	                        for (int i=0; i < stems.length; i += 2)
	                        {
	                            System.out.print(
	                            		stems[i] + " (" + stems[i+1] + ")");
	                        }
                        } else {
	                        for (int i=0; i < stems.length; i++)
	                        {
	                            System.out.print(stems[i]);
	                            System.out.print(" ");
	                        }
                        }
                        System.out.println();
                    }
                    break;
            }
        }

        long millis = System.currentTimeMillis() - start;

        System.err.println(
            MessageFormat.format("Operation took {0,number,#.###} seconds.", new Object[] { new Float( millis / 1000.0) }));
    }


    /** Prints the usage info. */
    public void usage(String message)
    {
        if (message != null)
        {
            System.err.println("Error: " + message);
        }

        System.err.println("Usage: java FSAStem <dictionaryFile> [-forms] [Byte2StringEncoding]");
    }
}
