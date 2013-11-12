package morfologik.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.Charset;

import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

public class FSABuildToolTest
{
    /* */
    @Test
    public void testStemmingFile() throws Exception {
        // Create a simple plain text file.
        File input = File.createTempFile("input", "in");
        File output = File.createTempFile("output", "fsa.txt");
        input.deleteOnExit();
        output.deleteOnExit();

        // Populate the file with data.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Emit UTF-8 BOM prefixed list of three strings.
        baos.write(new byte [] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        baos.write(Joiner.on('\n').join("abc", "def", "xyz").getBytes(Charsets.UTF_8));
        Files.copy(ByteStreams.newInputStreamSupplier(baos.toByteArray()), input);

        baos.reset();
        PrintStream prev = System.err;
        PrintStream ps = new PrintStream(baos);
        System.setErr(ps);
        try {
            FSABuildTool.main(new String [] {
                "--input", input.getAbsolutePath(),
                "--output", output.getAbsolutePath()
            });
        } finally {
            System.setErr(prev);
        }

        String logs = new String(baos.toByteArray(), Charset.defaultCharset());
        Assert.assertThat(logs, StringContains.containsString("UTF-8 BOM"));
        
        System.out.println(logs);
    }
}
