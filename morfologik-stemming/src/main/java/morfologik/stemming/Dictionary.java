package morfologik.stemming;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;

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
   * Default loaded dictionaries.
   */
  public static final WeakHashMap<String, Dictionary> defaultDictionaries = new WeakHashMap<String, Dictionary>();

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
  public static Dictionary read(File fsaFile) throws IOException {
    final File featuresFile = new File(fsaFile.getParent(),
        getExpectedFeaturesName(fsaFile.getName()));

    try (InputStream fsaStream = new FileInputStream(fsaFile);
         InputStream metadataStream = new FileInputStream(featuresFile)) {
      return read(fsaStream, metadataStream);
    }
  }

  /**
   * <p>
   * Attempts to load a dictionary using the URL to the FSA file and the
   * expected metadata extension.
   * 
   * <p>
   * This method can be used to load resource-based dictionaries, but be aware
   * of JAR resource-locking issues that arise from resource URLs.
   */
  public static Dictionary read(URL fsaURL) throws IOException {
    final String fsa = fsaURL.toExternalForm();
    final String features = getExpectedFeaturesName(fsa);

    try (InputStream fsaStream = ResourceUtils.openInputStream(fsa);
         InputStream metadataStream = ResourceUtils.openInputStream(features)) {
      return read(fsaStream, metadataStream);
    }
  }

  /**
   * Attempts to load a dictionary from opened streams of FSA dictionary data
   * and associated metadata.
   */
  public static Dictionary read(InputStream fsaData, InputStream featuresData) throws IOException
  {
    Map<DictionaryAttribute, String> map = new HashMap<DictionaryAttribute, String>();
    final Properties properties = new Properties();
    properties.load(new InputStreamReader(featuresData, "UTF-8"));

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
  public static String getExpectedFeaturesName(String name) {
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

  /**
   * Return a built-in dictionary for a given ISO language code. Dictionaries
   * are cached internally for potential reuse.
   * 
   * @throws RuntimeException
   *             Throws a {@link RuntimeException} if the dictionary is not
   *             bundled with the library.
   */
  public static Dictionary getForLanguage(String languageCode) {
    if (languageCode == null || "".equals(languageCode)) {
      throw new IllegalArgumentException(
          "Language code must not be empty.");
    }

    synchronized (defaultDictionaries) {
      Dictionary dict = defaultDictionaries.get(languageCode);
      if (dict != null)
        return dict;

      try {
        final String dictPath = "morfologik/dictionaries/" + languageCode + ".dict";
        final String metaPath = Dictionary.getExpectedFeaturesName(dictPath);

        try (InputStream fsaStream = ResourceUtils.openInputStream(dictPath);
            InputStream metadataStream = ResourceUtils.openInputStream(metaPath)) {
         dict = read(fsaStream, metadataStream);
        }

        defaultDictionaries.put(languageCode, dict);
        return dict;
      } catch (IOException e) {
        throw new RuntimeException(
            "Default dictionary resource for language '"
                + languageCode + "not found.", e);
      }
    }
  }

  /**
   * Converts the words on input or output according to conversion tables.
   * 
   * Useful if the input words need to be normalized (i.e., ligatures,
   * apostrophes and such).
   * 
   * @param str - input character sequence to be converted
   * @param conversionMap - conversion map used to convert the string (a map
   * from String to String)
   * @return a converted string.
   * 
   * @since 1.9.0
   * 
   */
  public static CharSequence convertText(final CharSequence str, final Map<String, String> conversionMap) {
    StringBuilder sb = new StringBuilder();
    sb.append(str);
    for (final String auxKey : conversionMap.keySet()) {
      int index = sb.indexOf(auxKey);
      while (index != -1) {
        sb.replace(index, index + auxKey.length(), conversionMap.get(auxKey));
        index = sb.indexOf(auxKey);
      }
    }
    return sb.toString();
  }

}
