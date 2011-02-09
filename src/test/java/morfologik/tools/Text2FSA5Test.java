package morfologik.tools;

import java.io.*;

import morfologik.fsa.*;

import org.junit.Assert;
import org.junit.Test;

/*
 * 
 */
public class Text2FSA5Test {
	@Test
	public void testTool() throws Exception {
		// Create a simple plain text file.
		File input = File.createTempFile("input", "in");
		File output = File.createTempFile("output", "fsa");
		input.deleteOnExit();
		output.deleteOnExit();

		// Populate the file with data.
		PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(input), "UTF-8"));
		w.println("b");
		w.println("cab");
		w.println("ab");
		w.close();
		
		FSABuildTool.main(new String [] {
				"--input", input.getAbsolutePath(),
				"--output", output.getAbsolutePath()
		});

		FSA5 fsa = FSA.read(new FileInputStream(output));
		Assert.assertEquals(3, new FSAInfo(fsa).finalStatesCount);
	}
}
