package morfologik.tools;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IStringConverterFactory;

class CustomParameterConverters implements IStringConverterFactory {
  public static class PathConverter implements IStringConverter<Path> {
    @Override
    public Path convert(String value) {
      return Paths.get(value);
    }
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public <T> Class<? extends IStringConverter<T>> getConverter(Class<T> forType) {
    if (forType.equals(Path.class)) {
      return (Class) PathConverter.class;
    }
    return null;
  }
}
