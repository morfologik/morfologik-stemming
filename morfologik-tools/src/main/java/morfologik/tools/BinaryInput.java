package morfologik.tools;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.beust.jcommander.Parameter;

final class BinaryInput {
  private static final String ARG_ACCEPT_BOM = "--accept-bom";
  private static final String ARG_ACCEPT_CR = "--accept-cr";
  private static final String ARG_IGNORE_EMPTY  = "--ignore-empty";

  private static interface LineConsumer {
    byte[] process(byte[] buffer, int length);
  }

  @Parameter(
      names = BinaryInput.ARG_ACCEPT_BOM,
      arity = 0,
      description = "Accept leading BOM bytes (UTF-8).")  
  private boolean acceptBom;

  @Parameter(
      names = BinaryInput.ARG_ACCEPT_CR,
      arity = 0,
      description = "Accept CR bytes in input sequences (\\r).")  
  private boolean acceptCr;

  @Parameter(
      names = BinaryInput.ARG_IGNORE_EMPTY,
      arity = 0,
      description = "Ignore empty lines in the input.")  
  private boolean ignoreEmpty;
  
  BinaryInput() {}

  public BinaryInput(boolean acceptBom,
                     boolean acceptCr,
                     boolean ignoreEmpty) {
    this.acceptBom = acceptBom;
    this.acceptCr = acceptCr;
    this.ignoreEmpty = ignoreEmpty;
  }
  
  List<byte[]> readBinarySequences(Path input, byte separator) throws IOException {
    final List<byte[]> sequences = new ArrayList<>();
    try (InputStream is = new BufferedInputStream(Files.newInputStream(input))) {
      if (!acceptBom) {
        is.mark(4);
        if (is.read() == 0xef &&
            is.read() == 0xbb &&
            is.read() == 0xbf) {
          throw new ExitStatusException(ExitStatus.ERROR_OTHER,
              "The input starts with UTF-8 BOM bytes which is" +
              " most likely not what you want. Use header-less UTF-8 file or override with %s.", ARG_ACCEPT_BOM);
        }
        is.reset();
      }

      forAllLines(is, separator, new LineConsumer() {
        @Override
        public byte[] process(byte[] buffer, int length) {
          if (!acceptCr && hasCr(buffer, length)) {
            throw new ExitStatusException(ExitStatus.ERROR_OTHER,
                "The input contains \\r byte (CR) which would be encoded as part of the"
                + " automaton. If this is desired, use %s.", ARG_ACCEPT_CR);
          }

          if (length == 0) {
            if (!ignoreEmpty) {
              throw new ExitStatusException(ExitStatus.ERROR_OTHER,
                  "The input contains empty sequences."
                  + " If these can be ignored, use --ignore-empty.");
            }
          } else {
            sequences.add(Arrays.copyOf(buffer, length));
          }

          return buffer;
        }
      });
    }
    
    return sequences;
  }

  private static boolean hasCr(byte[] seq, int length) {
    for (int o = length; --o >= 0;) {
      if (seq[o] == '\r') {
        return true;
      }
    }
    return false;
  }

  /**
   * Read all byte-separated sequences.
   */
  private static int forAllLines(InputStream is, byte separator, LineConsumer lineConsumer) throws IOException {
    int lines = 0;
    byte[] buffer = new byte[0];
    int b, pos = 0;
    while ((b = is.read()) != -1) {
      if (b == separator) {
        buffer = lineConsumer.process(buffer, pos);
        pos = 0;
        lines++;
      } else {
        if (pos >= buffer.length) {
          buffer = java.util.Arrays.copyOf(buffer, buffer.length + Math.max(10, buffer.length / 10));
        }
        buffer[pos++] = (byte) b;
      }
    }

    if (pos > 0) {
      lineConsumer.process(buffer, pos);
      lines++;
    }
    return lines;
  }
}
