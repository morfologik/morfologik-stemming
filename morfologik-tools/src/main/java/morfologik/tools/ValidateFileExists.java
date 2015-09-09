package morfologik.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

public final class ValidateFileExists implements IValueValidator<Path> {
  @Override
  public void validate(String name, Path value) throws ParameterException {
    if (!Files.exists(value)) {
      throw new ParameterException(String.format(Locale.ROOT, "%s does not exist: %s", name, value));
    }

    if (!Files.isRegularFile(value)) {
      throw new ParameterException(String.format(Locale.ROOT, "%s is not a file: %s", name, value));
    }

    if (!Files.isReadable(value)) {
      throw new ParameterException(String.format(Locale.ROOT, "%s is not readable: %s", name, value));
    }
  }
}
