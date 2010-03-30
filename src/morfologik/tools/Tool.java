package morfologik.tools;

import org.apache.commons.cli.*;

/**
 * Base class for command-line applications.
 */
abstract class Tool {
	/** Command line options. */
	protected final Options options = new Options();

	/**
	 * Initializes application context.
	 */
	protected final void go(String[] args) {
		initializeOptions(options);

		if (args.length == 0) {
			printUsage();
			return;
		}

		final Parser parser = new GnuParser();
		final CommandLine line;
		try {
			line = parser.parse(options, args);
			try {
				go(line);
			} catch (Throwable e) {
				printError("Unhandled program error occurred.", e);
			}
		} catch (MissingArgumentException e) {
			printError("Provide the required argument for option: "
			        + e.getMessage());
		} catch (MissingOptionException e) {
			printError("Provide the required option: " + e.getMessage());
		} catch (UnrecognizedOptionException e) {
			printError(e.getMessage());
		} catch (ParseException e) {
			printError("Could not parse command line: " + e.getMessage());
		}
	}

	/**
	 * Print an error and an associated exception.
	 */
	protected void printError(String msg, Throwable t) {
		printError(msg);
		t.printStackTrace(System.err);
	}

	/**
	 * Print an error without an exception.
	 */
	protected void printError(String msg) {
		System.err.println();
		System.err.println(msg);
	}

	/**
	 * Prints usage (options).
	 */
	protected void printUsage() {
		final HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(this.getClass().getName(), options, true);
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
	 * Is the tool available? <code>true</code> by default.
	 */
	protected boolean isAvailable() {
		return true;
	}
}
