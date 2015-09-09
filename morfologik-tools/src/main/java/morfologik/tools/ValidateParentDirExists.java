package morfologik.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

public final class ValidateParentDirExists implements IValueValidator<Path> {
  @Override
  public void validate(String name, Path value) throws ParameterException {
    value = value.toAbsolutePath().normalize().getParent();

    if (!Files.exists(value)) {
      throw new ParameterException(String.format(Locale.ROOT, "Directory does not exist: %s", value));
    }

    if (!Files.isDirectory(value)) {
      throw new ParameterException(String.format(Locale.ROOT, "Path is not a directory: %s", value));
    }

    if (!Files.isWritable(value)) {
      throw new ParameterException(String.format(Locale.ROOT, "Path is not writable: %s", value));
    }
  }
}
