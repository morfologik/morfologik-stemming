package morfologik.tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import morfologik.fsa.CFSAEncoder;

import org.apache.commons.cli.*;

/**
 * Convert from FSA5 to CFSA format.
 */
public final class FSA2CFSA extends Tool {
	/**
	 * Command line entry point after parsing arguments.
	 */
	protected void go(CommandLine line) throws Exception {
		String [] args = line.getArgs();
		if (args.length != 2)
		{
			throw new MissingArgumentException("Args: in.fsa out.cfsa");
		}
		
		FileInputStream fsa5 = new FileInputStream(args[0]);
		FileOutputStream cfsa = new FileOutputStream(args[1]);
		try
		{
			CFSAEncoder.convert(fsa5, cfsa);
		}
		finally
		{
			fsa5.close();
			cfsa.close();
		}
	}

	@Override
    protected void initializeOptions(Options options) {
		// None.
    }

	/**
	 * Command line entry point.
	 */
	public static void main(String[] args) throws Exception {
		final FSA2CFSA tool = new FSA2CFSA();
		tool.go(args);
	}
}