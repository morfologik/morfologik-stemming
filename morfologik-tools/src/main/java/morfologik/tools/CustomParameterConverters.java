package morfologik.tools;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IStringConverterFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

class CustomParameterConverters implements IStringConverterFactory {
  public static class PathConverter implements IStringConverter<Path> {
    @Override
    public Path convert(String value) {
      return Paths.get(value);
    }
  }

  @Override
  public Class<? extends IStringConverter<?>> getConverter(Class<?> forType) {
    if (forType.equals(Path.class)) {
      return PathConverter.class;
    }
    return null;
  }
}
