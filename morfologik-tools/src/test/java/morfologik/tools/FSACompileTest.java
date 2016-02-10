package morfologik.tools;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import morfologik.fsa.FSA;
import morfologik.stemming.BufferUtils;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Repeat;

public class FSACompileTest extends RandomizedTest {
  @Test
  @Repeat(iterations = 100)
  public void testCliInvocation() throws Exception {
    final Path input = newTempFile().toPath();
    final Path output = newTempFile().toPath();

    Set<String> sequences = new LinkedHashSet<>();
    for (int seqs = randomIntBetween(0, 100); --seqs >= 0;) {
      sequences.add(randomAsciiOfLengthBetween(1, 10));
    }

    try (OutputStream os = Files.newOutputStream(input)) {
      Iterator<String> i = sequences.iterator();
      while (i.hasNext()) {
        os.write(i.next().getBytes(StandardCharsets.UTF_8));
        
        // Sometimes don't add trailing '\n'.
        if (!i.hasNext() && randomBoolean()) {
          break;
        } else {
          os.write('\n');
          if (randomBoolean()) {
            os.write('\n');
          }
        }
      }
    }
    
    SerializationFormat format = randomFrom(SerializationFormat.values());

    Assertions.assertThat(new FSACompile(
        input,
        output,
        format,
        false,
        false,
        true).call()).isEqualTo(ExitStatus.SUCCESS);

    try (InputStream is = Files.newInputStream(output)) {
      FSA fsa = FSA.read(is);
      Assertions.assertThat(fsa).isNotNull();
      
      Set<String> result = new HashSet<>();
      for (ByteBuffer bb : fsa) {
        result.add(BufferUtils.toString(bb, StandardCharsets.UTF_8));
      }

      Assertions.assertThat(result).containsOnlyElementsOf(sequences);
    }
  }

  @Test
  public void testEmptyWarning() throws Exception {
    final Path input = newTempFile().toPath();
    final Path output = newTempFile().toPath();

    Files.write(input, "abc\n\ndef".getBytes(StandardCharsets.US_ASCII));

    String out = sysouts(new Callable<Void>() {
       @Override
      public Void call() throws Exception {
       FSACompile.main(new String[] {
           "--exit", "false",
           "--input", input.toAbsolutePath().toString(), 
           "--output", output.toAbsolutePath().toString() });
        return null;
      }
    });

    Assertions.assertThat(out).contains("--ignore-empty");
  }

  @Test
  public void testCrWarning() throws Exception {
    final Path input = newTempFile().toPath();
    final Path output = newTempFile().toPath();

    Files.write(input, "abc\r\ndef\r\n".getBytes(StandardCharsets.US_ASCII));

    String out = sysouts(new Callable<Void>() {
       @Override
      public Void call() throws Exception {
       FSACompile.main(new String[] {
           "--exit", "false",
           "--input", input.toAbsolutePath().toString(), 
           "--output", output.toAbsolutePath().toString() });
        return null;
      }
    });

    Assertions.assertThat(out).contains("CR");
  }

  @Test
  public void testBomWarning() throws Exception {
    final Path input = newTempFile().toPath();
    final Path output = newTempFile().toPath();

    // Emit UTF-8 BOM prefixed list of three strings.
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    baos.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });
    baos.write("abc\ndef\nxyz".getBytes(UTF8));
    Files.write(input, baos.toByteArray());

    String out = sysouts(new Callable<Void>() {
       @Override
      public Void call() throws Exception {
       FSACompile.main(new String[] {
           "--exit", "false",
           "--input", input.toAbsolutePath().toString(), 
           "--output", output.toAbsolutePath().toString() });
        return null;
      }
    });

    Assertions.assertThat(out).contains("UTF-8 BOM");
  }

  private String sysouts(Callable<Void> callable) throws Exception {
    PrintStream sout = System.out;
    PrintStream serr = System.err;

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos, true, "UTF-8");
    System.setOut(ps);
    System.setErr(ps);
    try {
      callable.call();
      return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    } finally {
      System.setOut(sout);
      System.setErr(serr);
    }
  }
}
