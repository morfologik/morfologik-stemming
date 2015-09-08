package morfologik.tools;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;

public class FSABuildToolTest extends RandomizedTest {
  /* */
  @Test
  public void testStemmingFile() throws Exception {
    // Create a simple plain text file.
    Path input = newTempFile().toPath();
    Path output = newTempFile().toPath();

    // Populate the file with data.
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    // Emit UTF-8 BOM prefixed list of three strings.
    baos.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
    baos.write("abc\ndef\nxyz".getBytes(UTF8));
    Files.write(input, baos.toByteArray());

    baos.reset();
    PrintStream prev = System.err;
    PrintStream ps = new PrintStream(baos);
    System.setErr(ps);
    try {
      FSABuildTool.main(new String[] { 
          "--input", input.toAbsolutePath().toString(), 
          "--output", output.toAbsolutePath().toString() });
    } finally {
      System.setErr(prev);
    }

    String logs = new String(baos.toByteArray(), Charset.defaultCharset());
    Assert.assertThat(logs, StringContains.containsString("UTF-8 BOM"));

    System.out.println(logs);
  }
}
