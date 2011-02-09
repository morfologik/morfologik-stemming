package morfologik.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.junit.Assert;
import org.junit.Test;

/*
 * 
 */
public class MorphEncodingToolTest {

	@Test
	public void testTool() throws Exception {
		// Create a simple plain text file.
		File input = File.createTempFile("input", "in");
		File output = File.createTempFile("output", "fsa.txt");
		input.deleteOnExit();
		output.deleteOnExit();

		// Populate the file with data.
		PrintWriter w = new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(input), "UTF-8"));
		w.println("passagère\tpassager\ttag");
		w.println("nieduży\tduży\ttest");
		w.println("abcd\tabc\txyz");
		w.close();

		// suffix
		MorphEncodingTool.main(new String[] { 
				"--input", input.getAbsolutePath(), 
				"--output", output.getAbsolutePath(), 
				"-suf" });

		BufferedReader testOutput = new BufferedReader(new InputStreamReader(
				new FileInputStream(output.getAbsolutePath()), "UTF-8"));
		Assert.assertEquals("passagère+Eer+tag", testOutput.readLine());
		Assert.assertEquals("nieduży+Iduży+test", testOutput.readLine());
		Assert.assertEquals("abcd+B+xyz", testOutput.readLine());

		testOutput.close();

		// prefix
		MorphEncodingTool.main(new String[] { 
				"--input", input.getAbsolutePath(), 
				"--output", output.getAbsolutePath(), 
				"-pre" });

		testOutput = new BufferedReader(new InputStreamReader(
				new FileInputStream(output.getAbsolutePath()), "UTF-8"));
		Assert.assertEquals("passagère+AEer+tag", testOutput.readLine());
		Assert.assertEquals("nieduży+DA+test", testOutput.readLine());
		Assert.assertEquals("abcd+AB+xyz", testOutput.readLine());

		testOutput.close();

		// infix
		MorphEncodingTool.main(new String[] { 
				"--input", input.getAbsolutePath(), 
				"--output", output.getAbsolutePath(), 
				"-inf" });

		testOutput = new BufferedReader(new InputStreamReader(
				new FileInputStream(output.getAbsolutePath()), "UTF-8"));
		Assert.assertEquals("passagère+AAEer+tag", testOutput.readLine());
		Assert.assertEquals("nieduży+ADA+test", testOutput.readLine());
		Assert.assertEquals("abcd+AAB+xyz", testOutput.readLine());

		testOutput.close();

	}

	/* */
	@Test
	public void testStemmingFile() throws Exception {
		// Create a simple plain text file.
		File input = File.createTempFile("input", "in");
		File output = File.createTempFile("output", "fsa.txt");
		input.deleteOnExit();
		output.deleteOnExit();

		// Populate the file with data.
		
		// stemming only
		PrintWriter w = new PrintWriter(new OutputStreamWriter(
				new FileOutputStream(input), "UTF-8"));
		w.println("passagère\tpassager");
		w.println("nieduży\tduży");
		w.println("abcd\tabc");
		w.close();

		MorphEncodingTool.main(new String[] { "--input",
				input.getAbsolutePath(), "--output", output.getAbsolutePath(),
		"-suf" });

		BufferedReader testOutput = new BufferedReader(new InputStreamReader(
				new FileInputStream(output.getAbsolutePath()), "UTF-8"));
		Assert.assertEquals("passagère+Eer+", testOutput.readLine());
		Assert.assertEquals("nieduży+Iduży+", testOutput.readLine());
		Assert.assertEquals("abcd+B+", testOutput.readLine());

		testOutput.close();
	}
}
