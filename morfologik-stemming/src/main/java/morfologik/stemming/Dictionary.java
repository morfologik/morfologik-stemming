package morfologik.stemming;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import morfologik.fsa.FSA;

/**
 * A dictionary combines {@link FSA} automaton and {@link DictionaryMetadata}
 * describing the way terms are encoded in the automaton.
 * 
 * <p>
 * A dictionary consists of two files:
 * <ul>
 * <li>an actual compressed FSA file,
 * <li>{@link DictionaryMetadata}, describing the way terms are encoded.
 * </ul>
 */
public final class Dictionary {
  /**
   * Expected metadata file extension.
   */
  public final static String METADATA_FILE_EXTENSION = "info";

  /**
   * {@link FSA} automaton with the compiled dictionary data.
   */
  public final FSA fsa;

  /**
   * Metadata associated with the dictionary.
   */
  public final DictionaryMetadata metadata;

  /**
   * It is strongly recommended to use static methods in this class for
   * reading dictionaries.
   * 
   * @param fsa
   *            An instantiated {@link FSA} instance.
   * 
   * @param metadata
   *            A map of attributes describing the compression format and
   *            other settings not contained in the FSA automaton. For an
   *            explanation of available attributes and their possible values,
   *            see {@link DictionaryMetadata}.
   */
  public Dictionary(FSA fsa, DictionaryMetadata metadata) {
    this.fsa = fsa;
    this.metadata = metadata;
  }

  /**
   * Attempts to load a dictionary using the path to the FSA file and the
   * expected metadata extension.
   */
  public static Dictionary read(Path fsa) throws IOException {
    final Path featureMap = 
        fsa.resolveSibling(getExpectedFeatureMapFileName(fsa.getFileName().toString()));

    try (InputStream fsaStream = Files.newInputStream(fsa);
         InputStream metadataStream = Files.newInputStream(featureMap)) {
      return read(fsaStream, metadataStream);
    }
  }

  /**
   * Attempts to load a dictionary using the path to the FSA file and the
   * expected metadata extension.
   */
  public static Dictionary read(File fsa) throws IOException {
    return read(fsa.toPath());
  }

  /**
   * Attempts to load a dictionary using the URL to the FSA file and the
   * expected metadata extension.
   */
  public static Dictionary read(URL fsaURL) throws IOException {
    final URL featureMapURL;
    try {
      featureMapURL = new URL(fsaURL, getExpectedFeatureMapFileName(fsaURL.toURI().getPath()));
    } catch (MalformedURLException | URISyntaxException e) {
      throw new IOException("Couldn't construct relative feature map URL for: " + fsaURL, e);
    }

    try (InputStream fsaStream = fsaURL.openStream();
         InputStream metadataStream = featureMapURL.openStream()) {
      return read(fsaStream, metadataStream);
    }
  }

  /**
   * Attempts to load a dictionary from opened streams of FSA dictionary data
   * and associated metadata. Input streams are not closed automatically.
   */
  public static Dictionary read(InputStream fsaData, InputStream featureMapData) throws IOException {
    Map<DictionaryAttribute, String> map = new HashMap<DictionaryAttribute, String>();
    final Properties properties = new Properties();
    properties.load(new InputStreamReader(featureMapData, "UTF-8"));

    // Handle back-compatibility for encoder specification.
    if (!properties.containsKey(DictionaryAttribute.ENCODER.propertyName)) {
      boolean hasDeprecated = properties.containsKey("fsa.dict.uses-suffixes") ||
                              properties.containsKey("fsa.dict.uses-infixes") ||
                              properties.containsKey("fsa.dict.uses-prefixes");

      boolean usesSuffixes = Boolean.valueOf(properties.getProperty("fsa.dict.uses-suffixes", "true"));
      boolean usesPrefixes = Boolean.valueOf(properties.getProperty("fsa.dict.uses-prefixes", "false"));
      boolean usesInfixes  = Boolean.valueOf(properties.getProperty("fsa.dict.uses-infixes",  "false"));

      final EncoderType encoder;
      if (usesInfixes) {
        encoder = EncoderType.INFIX;
      } else if (usesPrefixes) {
        encoder = EncoderType.PREFIX;
      } else if (usesSuffixes) {
        encoder = EncoderType.SUFFIX;
      } else {
        encoder = EncoderType.NONE;
      }

      if (!hasDeprecated) {
        throw new IOException("Use an explicit " +
            DictionaryAttribute.ENCODER.propertyName + "=" + encoder.name() +
            " metadata key: ");
      }

      throw new IOException("Deprecated encoder keys in metadata. Use " +
          DictionaryAttribute.ENCODER.propertyName + "=" + encoder.name());
    }

    for (Enumeration<?> e = properties.propertyNames(); e.hasMoreElements();) {
      String key = (String) e.nextElement();
      map.put(DictionaryAttribute.fromPropertyName(key), properties.getProperty(key));
    }
    final DictionaryMetadata features = new DictionaryMetadata(map);
    final FSA fsa = FSA.read(fsaData);

    return new Dictionary(fsa, features);    
  }

  /**
   * Returns the expected name of the metadata file, based on the name of the
   * FSA dictionary file. The expected name is resolved by truncating any
   * suffix of <code>name</code> and appending
   * {@link #METADATA_FILE_EXTENSION}.
   */
  public static String getExpectedFeatureMapFileName(String name) {
    final int dotIndex = name.lastIndexOf('.');
    final String featuresName;
    if (dotIndex >= 0) {
      featuresName = name.substring(0, dotIndex) + "."
          + METADATA_FILE_EXTENSION;
    } else {
      featuresName = name + "." + METADATA_FILE_EXTENSION;
    }

    return featuresName;
  }
}
