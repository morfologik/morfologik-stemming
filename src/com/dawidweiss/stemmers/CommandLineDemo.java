package com.dawidweiss.stemmers;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.StreamTokenizer;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * A command line demo of any of the stemmers.
 * @author Dawid Weiss
 */
public class CommandLineDemo {
    private String inputEncoding;
    private String outputEncoding;

    private void help() {
        System.err.println(
                "Words are read from the standard input and written\n" +
                "to standard output. Options passed as command line arguments:\n" +
                "\t--help  This help\n" +
                "\t-ie or --input-encoding  Specifies input stream encoding.\n" +
                "\t        platform encoding is used if not provided.\n" +
                "\t-oe or --output-encoding  Specifies output stream encoding.\n" +
                "\t        platform encoding is used if not provided.\n");
    }
    
    public void start(Stemmer stemmer, String [] args) {
        // parse command line arguments.
        try {
	        for (int i=0;i<args.length;i++) {
	            if ("--help".equals(args[i]) || "-help".equals(args[i])) {
	                help();
	                return;
	            } else if ("-ie".equals(args[i]) || "--input-encoding".equals(args[i])) {
	                inputEncoding = args[i+1];
	                i++;
	            } else if ("-oe".equals(args[i]) || "--output-encoding".equals(args[i])) {
	                outputEncoding = args[i+1];
	                i++;
	            } else {
	                System.err.println("Unrecognized argument: " + args[i]);
	            }
	        }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Incorrect number of parameters? '--help' for help.");
        }
        
        LineNumberReader reader = null;
        try {
            // open the input reader.
            if (inputEncoding != null) {
	            reader = new LineNumberReader( new InputStreamReader(
                            System.in , inputEncoding));
            } else {
	            reader = new LineNumberReader( new InputStreamReader(
                         System.in ));
            }
        } catch (UnsupportedEncodingException e1) {
            System.err.println("Unsupported encoding: " + inputEncoding);
        }
        
        Writer writer = null;
        try {
            // open the writer
            if (outputEncoding != null) {
	            writer = new OutputStreamWriter(
                        System.out, outputEncoding);
            } else {
	            writer = new OutputStreamWriter(
                        System.out);
            }
        } catch (UnsupportedEncodingException e1) {
            System.err.println("Unsupported encoding: " + inputEncoding);
            try {
                reader.close();
            } catch (IOException e) {
            }
        }

        StreamTokenizer tokenizer = new StreamTokenizer( reader );
        try {
            int token;
            String [] stems;
            while ( ( token = tokenizer.nextToken() ) != StreamTokenizer.TT_EOF ) {
                if (token != StreamTokenizer.TT_WORD)
                    continue;
                stems = stemmer.stem(tokenizer.sval);
                writer.write( tokenizer.sval );
                writer.write( " ; " );
                if (stems == null || stems.length == 0) {
                    writer.write( "?" );
                } else {
                    for (int i =0;i<stems.length;i++) {
                        if (i > 0) writer.write(", ");
                        if (stems[i] != null) {
                            writer.write(stems[i]);
                        } else {
                            writer.write("?null?");
                        }
                    }
                }
                writer.write("\n");
                writer.flush();
            }
        } catch (IOException e2) {
            System.err.println("I/O Exception occurred: " + e2.toString());
        }
    }

    public static void main(String[] args) {
        System.err.println("Invoke Lametyzator or Stempel class directly.");
    }
}
