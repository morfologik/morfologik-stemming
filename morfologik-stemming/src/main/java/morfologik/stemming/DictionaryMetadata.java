package morfologik.stemming;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * Description of attributes, their types and default values.
 * 
 * @see Dictionary
 */
public final class DictionaryMetadata {
	/**
	 * Attribute name for {@link #separator}.
	 */
	public final static String ATTR_NAME_SEPARATOR = "fsa.dict.separator";

	/**
	 * Attribute name for {@link #encoding}.
	 */
	public final static String ATTR_NAME_ENCODING = "fsa.dict.encoding";

	/**
	 * Attribute name for {@link #usesPrefixes}.
	 */
	public final static String ATTR_NAME_USES_PREFIXES = "fsa.dict.uses-prefixes";

	/**
	 * Attribute name for {@link #usesInfixes}.
	 */
	public final static String ATTR_NAME_USES_INFIXES = "fsa.dict.uses-infixes";

	/**
     * Attribute name for {@link #ignoreNumbers}.
     */
    public final static String ATTR_NAME_IGNORE_NUMBERS = "fsa.dict.speller.ignore-numbers";

    /**
     * Attribute name for {@link #dictionaryLocale}.
     */
    public final static String ATTR_NAME_LOCALE = "fsa.dict.speller.locale";    
    
    /**
     * Attribute name for {@link #ignorePunctuation}.
     */
    public final static String ATTR_NAME_IGNORE_PUNCTUATION = "fsa.dict.speller.ignore-punctuation";    
    
    /**
     * Attribute name for {@link #ignoreMixedCase}.
     */
    public final static String ATTR_NAME_IGNORE_MIXED_CASE = "fsa.dict.speller.ignore-mixed-case";
    
    /**
     * Attribute name for {@link #ignoreDiacritics}.
     */
    public final static String ATTR_NAME_IGNORE_DIACRITICS = "fsa.dict.speller.ignore-diacritics";
    
    
    /**
     * Attribute name for {@link #convertCase}.
     */
    public final static String ATTR_NAME_CONVERT_CASE = "fsa.dict.speller.convert-case";
    
    /**
     * Attribute name for {@link #runOnWords}.
     */
    public final static String ATTR_NAME_RUN_ON_WORDS = "fsa.dict.speller.runon-words";
    
    
	/**
	 * A separator character between fields (stem, lemma, form). The character
	 * must be within byte range (FSA uses bytes internally).
	 */
	public final byte separator;

	/**
	 * Encoding used for converting bytes to characters and vice versa.
	 */
	public final String encoding;

	/**
	 * True if the dictionary was compiled with prefix compression.
	 */
	public final boolean usesPrefixes;

	/**
	 * True if the dictionary was compiled with infix compression.
	 */
	public final boolean usesInfixes;
	
	/**
	 * True if the spelling dictionary is supposed to ignore words containing digits. 
	 */
	public final boolean ignoreNumbers;
	
	/**
     * Locale of the dictionary. 
     */
    public final Locale dictionaryLocale;
    
    /**
     * True if the spelling dictionary is supposed to ignore punctuation. 
     */
    public final boolean ignorePunctuation;    
    
    /**
     * True if the spelling dictionary is supposed to ignore punctuation. 
     */
    public final boolean ignoreMixedCase;    
    
    /**
     * True if the spelling dictionary is supposed to ignore diacritics, so that
     * 'a' would be treated as equivalent to 'ą'. 
     */
    public final boolean ignoreDiacritics;    
    
    
    /**
     * True if the spelling dictionary is supposed to treat upper and lower case
     * as equivalent. 
     */
    public final boolean convertCase;    

    /**
     * True if the spelling dictionary is supposed to split runOnWords; 
     */
    public final boolean runOnWords;
    
	/**
	 * Other meta data not included above.
	 */
	public final Map<String, String> metadata;

	/**
	 * Creates an immutable instance of {@link DictionaryMetadata}.
	 *  
	 */
	public DictionaryMetadata(char separator, String encoding,
	        boolean usesPrefixes, boolean usesInfixes, boolean
	        ignoreNumbers, boolean ignorePunctuation, boolean ignoreMixedCase,
	        boolean ignoreDiacritics, boolean convertCase, boolean runOnWords, Locale locale, Map<String, String> metadata) {
		this.encoding = encoding;
		this.usesPrefixes = usesPrefixes;
		this.usesInfixes = usesInfixes;
		this.ignoreNumbers = ignoreNumbers;
		this.dictionaryLocale = locale;
		this.ignorePunctuation = ignorePunctuation;
		this.ignoreMixedCase = ignoreMixedCase;
		this.convertCase = convertCase;
		this.runOnWords = runOnWords;
		this.ignoreDiacritics = ignoreDiacritics;
		
		try {
			final byte[] separatorBytes = new String(new char[] { separator })
			        .getBytes(encoding);
			if (separatorBytes.length != 1) {
				throw new RuntimeException(
				        "Separator character '"
				                + separator
				                + "' must be a single byte after transformation with encoding: "
				                + encoding);
			}
			this.separator = separatorBytes[0];
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Encoding not supported on this VM: "
			        + encoding);
		}

		this.metadata = Collections
		        .unmodifiableMap(new HashMap<String, String>(metadata));
	}

	/**
	 * Converts attributes in a {@link Map} to an instance of {@link Dictionary}
	 * , validating attribute values.
	 */
	static DictionaryMetadata fromMap(Properties properties) throws IOException {
		String separator = properties.getProperty(ATTR_NAME_SEPARATOR);
		if (separator == null || separator.length() != 1) {
            throw new IOException("Attribute " + ATTR_NAME_SEPARATOR
                    + " must be " + "a single character.");
        }
		
        if (separator.charAt(0) > 0xff) {
            throw new IllegalArgumentException("Field separator not within byte range: " + (int) separator.charAt(0));
        }
                		
		final String encoding = properties.getProperty(ATTR_NAME_ENCODING);
		if (encoding == null || encoding.length() == 0) {
			throw new IOException("Attribute " + ATTR_NAME_ENCODING
			        + " must be " + "present and non-empty.");
		}

		final boolean usesPrefixes = Boolean.valueOf(
		        properties.getProperty(ATTR_NAME_USES_PREFIXES, "false"))
		        .booleanValue();

		final boolean usesInfixes = Boolean.valueOf(
		        properties.getProperty(ATTR_NAME_USES_INFIXES, "false"))
		        .booleanValue();

		final boolean ignoreNumbers = Boolean.valueOf(
                properties.getProperty(ATTR_NAME_IGNORE_NUMBERS, "true"))
                .booleanValue();

		final boolean ignorePunctuation = Boolean.valueOf(
	                properties.getProperty(ATTR_NAME_IGNORE_PUNCTUATION, "true"))
	                .booleanValue();
		
		final boolean ignoreMixedCase = Boolean.valueOf(
                properties.getProperty(ATTR_NAME_IGNORE_MIXED_CASE, "true"))
                .booleanValue();

		final boolean ignoreDiacritics = Boolean.valueOf(
                properties.getProperty(ATTR_NAME_IGNORE_DIACRITICS, "true"))
                .booleanValue();
		
		final boolean runOnWords = Boolean.valueOf(
	                properties.getProperty(ATTR_NAME_RUN_ON_WORDS, "true"))
	                .booleanValue();
	    
		final boolean convertCase = Boolean.valueOf(
                properties.getProperty(ATTR_NAME_CONVERT_CASE, "true"))
                .booleanValue();
	      

		
		Locale dLocale = Locale.getDefault();
		
		if (properties.containsKey(ATTR_NAME_LOCALE)) {		
		dLocale = 
		        Locale.forLanguageTag(properties.getProperty(ATTR_NAME_LOCALE));
		} 
		
		final HashMap<String, String> metadata = new HashMap<String, String>();
		for (Map.Entry<Object, Object> e : properties.entrySet()) {
			metadata.put(e.getKey().toString(), e.getValue().toString());
		}		
						
		return new DictionaryMetadata(separator.charAt(0), encoding,
		        usesPrefixes, usesInfixes, ignoreNumbers, ignorePunctuation, 
		        ignoreMixedCase, ignoreDiacritics, convertCase, runOnWords, dLocale, metadata);
	}
}
