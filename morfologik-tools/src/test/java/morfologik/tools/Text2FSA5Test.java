package morfologik.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import morfologik.fsa.FSA;
import morfologik.fsa.FSA5;
import morfologik.fsa.builders.FSAInfo;

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

		try (InputStream is = new FileInputStream(output)) {
	    FSA5 fsa = FSA.read(is, FSA5.class);
	    Assert.assertEquals(3, new FSAInfo(fsa).finalStatesCount);
		}
	}
}
