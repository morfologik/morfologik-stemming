package morfologik.stemming;

import static morfologik.stemming.DictionaryAttribute.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Description of attributes, their types and default values.
 */
public final class DictionaryMetadata {
  /**
   * Default attribute values.
   */
  private static Map<DictionaryAttribute, String> DEFAULT_ATTRIBUTES = new DictionaryMetadataBuilder()
    .frequencyIncluded(false)
    .ignorePunctuation()
    .ignoreNumbers()
    .ignoreCamelCase()
    .ignoreAllUppercase()
    .ignoreDiacritics()
    .convertCase()
    .supportRunOnWords()
    .toMap();

  /**
   * Required attributes.
   */
  private static EnumSet<DictionaryAttribute> REQUIRED_ATTRIBUTES = EnumSet.of(
      SEPARATOR,
      ENCODER,
      ENCODING);

  /**
   * A separator character between fields (stem, lemma, form). The character
   * must be within byte range (FSA uses bytes internally).
   */
  private byte separator;
  private char separatorChar;

  /**
   * Encoding used for converting bytes to characters and vice versa.
   */
  private String encoding;

  private Charset charset;
  private Locale locale = Locale.getDefault();

  /**
   * Replacement pairs for non-obvious candidate search in a speller dictionary.
   */
  private LinkedHashMap<String, List<String>> replacementPairs = new LinkedHashMap<>();

  /**
   * Conversion pairs for input conversion, for example to replace ligatures.
   */
  private LinkedHashMap<String, String> inputConversion = new LinkedHashMap<>();

  /**
   * Conversion pairs for output conversion, for example to replace ligatures.
   */
  private LinkedHashMap<String, String> outputConversion = new LinkedHashMap<>();

  /**
   * Equivalent characters (treated similarly as equivalent chars with and without
   * diacritics). For example, Polish <tt>ł</tt> can be specified as equivalent to <tt>l</tt>.
   * 
   * This implements a feature similar to hunspell MAP in the affix file.
   */
  private LinkedHashMap<Character, List<Character>> equivalentChars = new LinkedHashMap<>();

  /**
   * All attributes.
   */
  private final EnumMap<DictionaryAttribute, String> attributes;

  /**
   * All "enabled" boolean attributes.
   */
  private final EnumMap<DictionaryAttribute,Boolean> boolAttributes;

  /**
   * Sequence encoder.
   */
  private EncoderType encoderType;

  /**
   * Expected metadata file extension.
   */
  public final static String METADATA_FILE_EXTENSION = "info";

  /**
   * @return Return all metadata attributes.  
   */
  public Map<DictionaryAttribute, String> getAttributes() {
    return Collections.unmodifiableMap(attributes);
  }

  // Cached attrs.
  public String getEncoding()      { return encoding; }
  public byte getSeparator()       { return separator; }
  public Locale getLocale()        { return locale; }

  public LinkedHashMap<String, String> getInputConversionPairs() { return inputConversion; }
  public LinkedHashMap<String, String> getOutputConversionPairs() { return outputConversion; }

  public LinkedHashMap<String, List<String>> getReplacementPairs() { return replacementPairs; }
  public LinkedHashMap<Character, List<Character>> getEquivalentChars() { return equivalentChars; }

  // Dynamically fetched.
  public boolean isFrequencyIncluded()    { return boolAttributes.get(FREQUENCY_INCLUDED); }
  public boolean isIgnoringPunctuation()  { return boolAttributes.get(IGNORE_PUNCTUATION); }
  public boolean isIgnoringNumbers()      { return boolAttributes.get(IGNORE_NUMBERS); }
  public boolean isIgnoringCamelCase()    { return boolAttributes.get(IGNORE_CAMEL_CASE); }
  public boolean isIgnoringAllUppercase() { return boolAttributes.get(IGNORE_ALL_UPPERCASE); }
  public boolean isIgnoringDiacritics()   { return boolAttributes.get(IGNORE_DIACRITICS); }
  public boolean isConvertingCase()       { return boolAttributes.get(CONVERT_CASE); }
  public boolean isSupportingRunOnWords() { return boolAttributes.get(RUN_ON_WORDS); }

  /**
   * Create an instance from an attribute map.
   * 
   * @param attrs A set of {@link DictionaryAttribute} keys and their associated values.
   * @see DictionaryMetadataBuilder
   */
  public DictionaryMetadata(Map<DictionaryAttribute, String> attrs) {
    this.boolAttributes = new EnumMap<DictionaryAttribute,Boolean>(DictionaryAttribute.class);
    this.attributes = new EnumMap<DictionaryAttribute, String>(DictionaryAttribute.class);
    this.attributes.putAll(attrs);

    EnumMap<DictionaryAttribute, String> attributeMap = new EnumMap<DictionaryAttribute, String>(DEFAULT_ATTRIBUTES);
    attributeMap.putAll(attrs);

    // Convert some attrs from the map to local fields for performance reasons.
    EnumSet<DictionaryAttribute> requiredAttributes = EnumSet.copyOf(REQUIRED_ATTRIBUTES);

    for (Map.Entry<DictionaryAttribute,String> e : attributeMap.entrySet()) {
      requiredAttributes.remove(e.getKey());

      // Run validation and conversion on all of them.
      Object value = e.getKey().fromString(e.getValue());
      switch (e.getKey()) {
        case ENCODING:
          this.encoding = e.getValue();
          if (!Charset.isSupported(encoding)) {
            throw new IllegalArgumentException("Encoding not supported on this JVM: "
                + encoding);
          }
          this.charset = (Charset) value;
          break;
  
        case SEPARATOR:
          this.separatorChar = (Character) value;
          break;
  
        case LOCALE:
          this.locale = (Locale) value;
          break;
  
        case ENCODER:
          this.encoderType = (EncoderType) value;
          break;
  
        case INPUT_CONVERSION:
        {
          @SuppressWarnings("unchecked")
          LinkedHashMap<String, String> gvalue = (LinkedHashMap<String, String>) value;
          this.inputConversion = gvalue;
        }
        break;
  
        case OUTPUT_CONVERSION:
        {
          @SuppressWarnings("unchecked")
          LinkedHashMap<String, String> gvalue = (LinkedHashMap<String, String>) value;
          this.outputConversion = gvalue;
        }
        break;
  
        case REPLACEMENT_PAIRS:
        {
          @SuppressWarnings("unchecked")
          LinkedHashMap<String, List<String>> gvalue = (LinkedHashMap<String, List<String>>) value;
          this.replacementPairs = gvalue;
        }
        break;
  
        case EQUIVALENT_CHARS:
        {
          @SuppressWarnings("unchecked")
          LinkedHashMap<Character, List<Character>> gvalue = (LinkedHashMap<Character, List<Character>>) value;
          this.equivalentChars = gvalue;
        }
        break;
  
        case IGNORE_PUNCTUATION:
        case IGNORE_NUMBERS:
        case IGNORE_CAMEL_CASE:
        case IGNORE_ALL_UPPERCASE:
        case IGNORE_DIACRITICS:
        case CONVERT_CASE:
        case RUN_ON_WORDS:
        case FREQUENCY_INCLUDED:
          this.boolAttributes.put(e.getKey(), (Boolean) value);
          break;
  
        case AUTHOR:
        case LICENSE:
        case CREATION_DATE:
          // Just run validation.
          e.getKey().fromString(e.getValue());
          break;
  
        default:
          throw new RuntimeException("Unexpected code path (attribute should be handled but is not): " + e.getKey());
      }
    }

    if (!requiredAttributes.isEmpty()) {
      throw new IllegalArgumentException("At least one the required attributes was not provided: "
          + requiredAttributes.toString());
    }

    // Sanity check.
    CharsetEncoder encoder = getEncoder();
    try {
      ByteBuffer encoded = encoder.encode(CharBuffer.wrap(new char [] { separatorChar }));
      if (encoded.remaining() > 1) {
        throw new IllegalArgumentException("Separator character is not a single byte in encoding "
            + encoding + ": " + separatorChar);
      }
      this.separator = encoded.get();
    } catch (CharacterCodingException e) {
      throw new IllegalArgumentException("Separator character cannot be converted to a byte in "
          + encoding + ": " + separatorChar, e);
    }
  }

  /**
   * @return Returns a new {@link CharsetDecoder} for the {@link #encoding}. 
   */
  public CharsetDecoder getDecoder() {
    try {
      return charset.newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT);
    } catch (UnsupportedCharsetException e) {
      throw new RuntimeException(
          "FSA's encoding charset is not supported: " + encoding);
    }
  }

  /**
   * @return Returns a new {@link CharsetEncoder} for the {@link #encoding}.
   */
  public CharsetEncoder getEncoder() {
    try {
      return charset.newEncoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT);
    } catch (UnsupportedCharsetException e) {
      throw new RuntimeException(
          "FSA's encoding charset is not supported: " + encoding);
    }
  }

  /**
   * @return Return sequence encoder type.
   */
  public EncoderType getSequenceEncoderType() {
    return encoderType;
  }

  /**
   * @return Returns the {@link #separator} byte converted to a single
   *         <code>char</code>.
   * @throws RuntimeException
   *           if this conversion is for some reason impossible (the byte is a
   *           surrogate pair, FSA's {@link #encoding} is not available).
   */
  public char getSeparatorAsChar() {
    return separatorChar;
  }

  /**
   * @return A shortcut returning {@link DictionaryMetadataBuilder}.
   */
  public static DictionaryMetadataBuilder builder() {
    return new DictionaryMetadataBuilder();
  }
  
  /**
   * Returns the expected name of the metadata file, based on the name of the
   * dictionary file. The expected name is resolved by truncating any
   * file extension of <code>name</code> and appending
   * {@link DictionaryMetadata#METADATA_FILE_EXTENSION}.
   * 
   * @param dictionaryFile The name of the dictionary (<code>*.dict</code>) file. 
   * @return Returns the expected name of the metadata file. 
   */
  public static String getExpectedMetadataFileName(String dictionaryFile) {
    final int dotIndex = dictionaryFile.lastIndexOf('.');
    final String featuresName;
    if (dotIndex >= 0) {
      featuresName = dictionaryFile.substring(0, dotIndex) + "." + METADATA_FILE_EXTENSION;
    } else {
      featuresName = dictionaryFile + "." + METADATA_FILE_EXTENSION;
    }
  
    return featuresName;
  }

  /**
   * @param dictionary The location of the dictionary file.
   * @return Returns the expected location of a metadata file.
   */
  public static Path getExpectedMetadataLocation(Path dictionary) {
    return dictionary.resolveSibling(
        getExpectedMetadataFileName(dictionary.getFileName().toString()));
  }

  /**
   * Read dictionary metadata from a property file (stream).
   * 
   * @param metadataStream The stream with metadata. 
   * @return Returns {@link DictionaryMetadata} read from a the stream (property file).
   * @throws IOException Thrown if an I/O exception occurs. 
   */
  public static DictionaryMetadata read(InputStream metadataStream) throws IOException {
    Map<DictionaryAttribute, String> map = new HashMap<DictionaryAttribute, String>();
    final Properties properties = new Properties();
    properties.load(new InputStreamReader(metadataStream, "UTF-8"));

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

    return new DictionaryMetadata(map);
  }

  /**
   * Write dictionary attributes (metadata).
   * 
   * @param writer The writer to write to.
   * @throws IOException Thrown when an I/O error occurs.
   */
  public void write(Writer writer) throws IOException {
    final Properties properties = new Properties();

    for (Map.Entry<DictionaryAttribute, String> e : getAttributes().entrySet()) {
      properties.setProperty(e.getKey().propertyName, e.getValue());
    }

    properties.store(writer, "# " + getClass().getName());
  }
}
