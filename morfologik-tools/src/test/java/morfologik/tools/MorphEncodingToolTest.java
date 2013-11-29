package morfologik.tools;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

import morfologik.fsa.FSA;
import morfologik.stemming.Dictionary;
import morfologik.stemming.DictionaryLookup;
import morfologik.stemming.DictionaryMetadataBuilder;
import morfologik.stemming.EncoderType;
import morfologik.stemming.WordData;

import org.fest.assertions.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.google.common.base.Charsets;
import com.google.common.io.Closer;

/*
 * 
 */
public class MorphEncodingToolTest extends RandomizedTest {
    private Closer closer = Closer.create();

    @After
    public void cleanup() throws IOException {
        closer.close();
    }
    
	@Test
	public void testTool() throws Exception {
		// Create a simple plain text file.
		File input = super.newTempFile();
		File output = super.newTempFile();

		// Populate the file with data.
		PrintWriter w = 
		    new PrintWriter(
		        new OutputStreamWriter(
		            closer.register(new FileOutputStream(input)), "UTF-8"));
		w.println("passagère\tpassager\ttag");
		w.println("nieduży\tduży\ttest");
		w.print("abcd\tabc\txyz");
		w.close();

		// suffix
		MorphEncodingTool.main(new String[] { 
				"--input", input.getAbsolutePath(), 
				"--output", output.getAbsolutePath(), 
				"--encoder", "suffix" });

		BufferedReader testOutput = 
		    new BufferedReader(
		        new InputStreamReader(
		            closer.register(new FileInputStream(output.getAbsolutePath())), "UTF-8"));
		Assert.assertEquals("passagère+Eer+tag", testOutput.readLine());
		Assert.assertEquals("nieduży+Iduży+test", testOutput.readLine());
		Assert.assertEquals("abcd+B+xyz", testOutput.readLine());

		// prefix
		MorphEncodingTool.main(new String[] { 
				"--input", input.getAbsolutePath(), 
				"--output", output.getAbsolutePath(), 
				"--encoder", "prefix" });

		testOutput = 
		    new BufferedReader(
		        new InputStreamReader(
		            closer.register(new FileInputStream(output.getAbsolutePath())), "UTF-8"));
		Assert.assertEquals("passagère+AEer+tag", testOutput.readLine());
		Assert.assertEquals("nieduży+DA+test", testOutput.readLine());
		Assert.assertEquals("abcd+AB+xyz", testOutput.readLine());

		// infix
		MorphEncodingTool.main(new String[] { 
				"--input", input.getAbsolutePath(), 
				"--output", output.getAbsolutePath(), 
				"--encoder", "infix" });

		testOutput = 
		    new BufferedReader(
		        new InputStreamReader(
		            closer.register(new FileInputStream(output.getAbsolutePath())), "UTF-8"));
		Assert.assertEquals("passagère+GDAr+tag", testOutput.readLine());
		Assert.assertEquals("nieduży+ADA+test", testOutput.readLine());
		Assert.assertEquals("abcd+AAB+xyz", testOutput.readLine());

		// custom annotation - test tabs
        MorphEncodingTool.main(new String[] {
                "--annotation", "\t",
                "--input", input.getAbsolutePath(), 
                "--output", output.getAbsolutePath(), 
                "--encoder", "infix" });

        testOutput = 
            new BufferedReader(
                new InputStreamReader(
                    closer.register(new FileInputStream(output.getAbsolutePath())), "UTF-8"));
        Assert.assertEquals("passagère\tGDAr\ttag", testOutput.readLine());
        Assert.assertEquals("nieduży\tADA\ttest", testOutput.readLine());
        Assert.assertEquals("abcd\tAAB\txyz", testOutput.readLine());
	}

	/* */
	@Test
	public void testStemmingFile() throws Exception {
		// Create a simple plain text file.
		File input = super.newTempFile();
		File output = super.newTempFile();

		PrintWriter w = 
		    new PrintWriter(
		        new OutputStreamWriter(
		            closer.register(new FileOutputStream(input)), "UTF-8"));
		w.println("passagère\tpassager");
		w.println("nieduży\tduży");
		w.println();
		w.println("abcd\tabc");
		w.close();

		MorphEncodingTool.main(new String[] { 
		    "--input", input.getAbsolutePath(), 
		    "--output", output.getAbsolutePath(),
		    "-e", "suffix" });

		BufferedReader testOutput = 
		    new BufferedReader(
		        new InputStreamReader(
		            closer.register(new FileInputStream(output.getAbsolutePath())), "UTF-8"));
		Assert.assertEquals("passagère+Eer+", testOutput.readLine());
		Assert.assertEquals("nieduży+Iduży+", testOutput.readLine());
		Assert.assertEquals("abcd+B+", testOutput.readLine());

		testOutput.close();
	}

    /* */
    @Test
    public void testZeroByteSeparator() throws Exception {
        // Create a simple plain text file.
        File input = newTempFile();
        File output = newTempFile();

        // Populate the file with data.
        PrintWriter w = 
            new PrintWriter(
                new OutputStreamWriter(
                    closer.register(new FileOutputStream(input)), "UTF-8"));
        w.println("passagère\tpassager\tTAG1");
        w.println("nieduży\tduży\tTAG2");
        w.println("abcd\tabc\tTAG3");
        w.close();

        MorphEncodingTool.main(new String[] { 
            "--input", input.getAbsolutePath(), 
            "--output", output.getAbsolutePath(),
            "-e", "suffix",
            "--annotation", "\u0000"});

        BufferedReader testOutput = 
            new BufferedReader(
                new InputStreamReader(
                    closer.register(new FileInputStream(output.getAbsolutePath())), "UTF-8"));

        Assert.assertEquals("passagère\u0000Eer\u0000TAG1", testOutput.readLine());
        Assert.assertEquals("nieduży\u0000Iduży\u0000TAG2", testOutput.readLine());
        Assert.assertEquals("abcd\u0000B\u0000TAG3", testOutput.readLine());
 
        File fsaFile = newTempFile();
        FSABuildTool.main(
            "--input", output.getAbsolutePath(),
            "--output", fsaFile.getAbsolutePath());

        FSA fsa = FSA.read(fsaFile);
        DictionaryLookup dl = new DictionaryLookup(
            new Dictionary(
                fsa, 
                new DictionaryMetadataBuilder()
                    .separator((char) 0)
                    .encoding(Charsets.UTF_8)
                    .encoder(EncoderType.SUFFIX)
                    .build()));

        checkEntry(dl, "passagère", "passager", "TAG1");
        checkEntry(dl, "nieduży", "duży", "TAG2");
        checkEntry(dl, "abcd", "abc", "TAG3");
    }

    /* */
    @Test
    public void testAnnotationCharacterInBaseOrDerivedWord() throws Exception {
        // Create a simple plain text file.
        File input = newTempFile();
        File output = newTempFile();

        // Populate the file with data.
        PrintWriter w = 
            new PrintWriter(
                new OutputStreamWriter(
                    closer.register(new FileOutputStream(input)), "UTF-8"));
        w.println("foo+\tbar-\tTAG1");
        w.close();

        PrintStream err = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            System.setErr(new PrintStream(baos, true, "UTF-8"));
            MorphEncodingTool.main(new String[] { 
                "--input", input.getAbsolutePath(), 
                "--output", output.getAbsolutePath(),
                "-e", "suffix",
                "--annotation", "+"});
        } finally {
            System.err.flush();
            System.setErr(err);
        }

        Assertions.assertThat(new String(baos.toByteArray(), Charsets.UTF_8))
            .contains("contain the annotation byte");
    }

    private void checkEntry(DictionaryLookup dl, String word, String base, String tag) {
        List<WordData> lookup = dl.lookup(word);
        Assertions.assertThat(lookup.size()).isEqualTo(1);
        WordData wordData = lookup.get(0);
        Assertions.assertThat(wordData.getWord().toString()).isEqualTo(word);
        Assertions.assertThat(wordData.getStem().toString()).isEqualTo(base);
        Assertions.assertThat(wordData.getTag().toString()).isEqualTo(tag);
    }
}
