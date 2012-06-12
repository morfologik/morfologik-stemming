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
        options.addOption(SharedOptions.help);
		initializeOptions(options);
		
		// Commons-cli is pretty dumb in terms of option parsing because it
		// validates immediately and there is no way to determine
		// if an option exists without bailing out with an exception. This
		// is a hardcoded workaround for --help
		for (String arg : args) { 
		    if ("--help".equals(arg)) {
		        printUsage();
	            return;
		    }
		}

		final Parser parser = new GnuParser();
		final CommandLine line;
		try {
			line = parser.parse(options, args);
			if (line.hasOption(SharedOptions.help.getLongOpt())) {
			    printUsage();
			    return;
			}
			if (line.getArgList().size() > 0) {
			    printError("Unreconized left over command line arguments: "
			            + line.getArgList());
			    return;
			}

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
		System.err.println("Invoke with '--help' for help.");
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
