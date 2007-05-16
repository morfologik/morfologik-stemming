package morfologik.tools;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import morfologik.util.StringUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.UnrecognizedOptionException;

/**
 * Base class for command-line applications.
 */
public abstract class BaseCommandLineTool {
    /**
     * Logger for the application.
     */
    protected final Logger logger = Logger.getLogger(this.getClass().getName());

    /** Command line options. */
    protected final Options cliOptions = new Options();

    /**
     * Initializes application context.
     */
    protected final void go(String[] args) {
        initializeOptions(cliOptions);

        if (args.length == 0) {
            printUsage();
            return;
        }

        final Parser parser = new GnuParser();
        final CommandLine line;
        try {
            line = parser.parse(cliOptions, args);
            try {
                go(line);
            } catch (ConfigurationException e) {
                logger.severe(StringUtils.chainExceptionMessages(e));
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Unhandled program error occurred.", e);
            }
        } catch (MissingArgumentException e) {
            logger.log(Level.SEVERE, "Provide the required argument for option " + e.getMessage());
            printUsage();
        } catch (MissingOptionException e) {
            logger.log(Level.SEVERE, "Provide the required option " + e.getMessage());
            printUsage();
        } catch (UnrecognizedOptionException e) {
            logger.log(Level.SEVERE, e.getMessage() + "\n");
            printUsage();
        } catch (ParseException exp) {
            logger.log(Level.SEVERE, "Could not parse command line: " + exp.getMessage());
            printUsage();
        }
    }

    /**
     * Checks if the given encoding is supported on this VM. 
     */
    protected static void assertEncodingSupported(String encoding)
        throws ConfigurationException
    {
        try {
            new String("abc".getBytes(encoding), encoding);
        } catch (UnsupportedEncodingException e) {
            throw new ConfigurationException("Encoding not supported on this virtual machine: "
                    + encoding);
        }
    }

    /**
     * Prints usage (options).
     */
    protected void printUsage() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(this.getClass().getName(), cliOptions, true);
    }

    /**
     * Override and write your stuff using command line options.
     */
    protected abstract void go(CommandLine line) throws Exception;

    /**
     * Override and initialize options.
     */
    protected abstract void initializeOptions(Options options);

    /**
     * Returns the logger for this class.
     */
    protected final Logger getLogger() {
        return logger;
    }
}
