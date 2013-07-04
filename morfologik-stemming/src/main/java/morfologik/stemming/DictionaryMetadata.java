package morfologik.stemming;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

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
     * Attribute name for {@link #ignoreCamelCase}.
     */
    public final static String ATTR_NAME_IGNORE_CAMEL_CASE = "fsa.dict.speller.ignore-camel-case";
    
    /**
     * Attribute name for {@link #ignoreAllUppercase}.
     */
    public final static String ATTR_NAME_IGNORE_ALL_UPPERCASE = "fsa.dict.speller.ignore-all-uppercase";
    
    
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
     * Attribute name for {@link #replacementPairs}.
     */        
    public final static String ATTR_NAME_REPLACEMENT_PAIRS = "fsa.dict.speller.replacement-pairs";
    
    /**
     * Attribute name for {@link #equivalentChars}.
     */        
    public final static String ATTR_NAME_EQUIVALENT_CHARS = "fsa.dict.speller.equivalent-chars";
    
    
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
     * True if the spelling dictionary is supposed to ignore CamelCase words. 
     */
    public final boolean ignoreCamelCase;
    
    /**
     * True if the spelling dictionary is supposed to ignore ALL UPPERCASE words. 
     */
    public final boolean ignoreAllUppercase;
    
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
     * Replacement pairs for non-obvious candidate search in a speller dictionary.
     */
    public final Map<String, List<String>> replacementPairs;
    
    /**
     * Equivalent characters (treated similarly as equivalent chars with and without
     * diacritics). For example, Polish <tt>ł</tt> can be specified as equivalent to <tt>l</tt>.
     * 
     * This implements a feature similar to hunspell MAP in the affix file.
     */
    public final Map<Character, List<Character>> equivalentChars;
    
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
	        ignoreNumbers, boolean ignorePunctuation, boolean ignoreCamelCase,
	        boolean ignoreAllUppercase,
	        boolean ignoreDiacritics, boolean convertCase, boolean runOnWords, 
	        Map<String, List<String>> replacementPairs, Map<Character, List<Character>>
	        equivalentChars,
	        Locale locale, Map<String, String> metadata) {
		this.encoding = encoding;
		this.usesPrefixes = usesPrefixes;
		this.usesInfixes = usesInfixes;
		this.ignoreNumbers = ignoreNumbers;
		this.dictionaryLocale = locale;
		this.ignorePunctuation = ignorePunctuation;
		this.ignoreCamelCase = ignoreCamelCase;
		this.ignoreAllUppercase = ignoreAllUppercase;
		this.convertCase = convertCase;
		this.runOnWords = runOnWords;
		this.ignoreDiacritics = ignoreDiacritics;
		this.replacementPairs = replacementPairs;
		this.equivalentChars = equivalentChars;
		
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
	 * Returns a new {@link CharsetDecoder} for the {@link #encoding}.
	 */
	public CharsetDecoder getDecoder() {
        try {
            Charset charset = Charset.forName(encoding);
            return charset.newDecoder().onMalformedInput(
                    CodingErrorAction.REPORT).onUnmappableCharacter(
                    CodingErrorAction.REPORT);
        } catch (UnsupportedCharsetException e) {
            throw new RuntimeException(
                    "FSA's encoding charset is not supported: " + encoding);
        }
	}

	/**
     * Returns a new {@link CharsetEncoder} for the {@link #encoding}.
     */
    public CharsetEncoder getEncoder() {
        try {
            Charset charset = Charset.forName(encoding);
            return charset.newEncoder();
        } catch (UnsupportedCharsetException e) {
            throw new RuntimeException(
                    "FSA's encoding charset is not supported: " + encoding);
        }
    }

	/**
	 * Returns the {@link #separator} byte converted to a single <code>char</code>. Throws
	 * a {@link RuntimeException} if this conversion is for some reason impossible
	 * (the byte is a surrogate pair, FSA's {@link #encoding} is not available). 
	 */
    public char getFsaSeparatorAsChar() {
        try {
            CharsetDecoder decoder = getDecoder();
            CharBuffer decoded = decoder.decode(ByteBuffer.wrap(new byte[] { separator }));
            if (decoded.remaining() != 1) {
                throw new RuntimeException(
                        "FSA's separator byte takes more than one character after conversion "
                                + " of byte 0x"
                                + Integer.toHexString(separator)
                                + " using encoding " + encoding);
            }
            return decoded.get();
        } catch (CharacterCodingException e) {
            throw new RuntimeException(
                    "FSA's separator character cannot be decoded from byte value 0x"
                            + Integer.toHexString(separator)
                            + " using encoding " + encoding, e);
        }
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
		
		final boolean ignoreCamelCase = Boolean.valueOf(
                properties.getProperty(ATTR_NAME_IGNORE_CAMEL_CASE, "true"))
                .booleanValue();

		final boolean ignoreAllUppercase = Boolean.valueOf(
                properties.getProperty(ATTR_NAME_IGNORE_ALL_UPPERCASE, "true"))
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
	      
		final Map<String, List<String>> replacementPairs = getReplacementPairs(
		        properties.getProperty(ATTR_NAME_REPLACEMENT_PAIRS));
		
		final Map<Character, List<Character>> equivalentChars = getEquivalentChars(
                properties.getProperty(ATTR_NAME_EQUIVALENT_CHARS));
		
		Locale dLocale = Locale.getDefault();
		if (properties.containsKey(ATTR_NAME_LOCALE)) {
		    dLocale = new Locale(properties.getProperty(ATTR_NAME_LOCALE));
		}
		
		final HashMap<String, String> metadata = new HashMap<String, String>();
		for (Map.Entry<Object, Object> e : properties.entrySet()) {
			metadata.put(e.getKey().toString(), e.getValue().toString());
		}		
						
		return new DictionaryMetadata(separator.charAt(0), encoding,
		        usesPrefixes, usesInfixes, ignoreNumbers, ignorePunctuation, 
		        ignoreCamelCase, ignoreAllUppercase, ignoreDiacritics, 
		        convertCase, runOnWords, replacementPairs, equivalentChars, dLocale, metadata);
	}

	
    private static Map<Character, List<Character>> getEquivalentChars(
            String property) throws IOException {
        Map<Character, List<Character>> equivalentCharacters = 
                new HashMap<Character, List<Character>>();
        if (property != null && property.length() != 0) {
            final String[] eqChars = property.split(", ?");
            for (final String characterPair : eqChars) {
                final String[] twoChars = characterPair.trim().split(" ");
                if (twoChars.length == 2 
                        && twoChars[0].length() == 1
                        && twoChars[1].length() == 1) { // proper format
                    if (!equivalentCharacters.containsKey(twoChars[0].charAt(0))) {
                        List<Character> chList = new ArrayList<Character>();
                        chList.add(twoChars[1].charAt(0));
                        equivalentCharacters.put(twoChars[0].charAt(0),
                                chList);
                    } else {
                        equivalentCharacters.get(twoChars[0].charAt(0)).
                            add(twoChars[1].charAt(0));
                    }
                } else {
                    throw new IOException("Attribute " + ATTR_NAME_EQUIVALENT_CHARS
                            + " is not in the proper format. The map is " + twoChars.length +
                            " characters long.");
                }
            }
        }
        return equivalentCharacters;
    }

    private static Map<String, List<String>> getReplacementPairs(String property) throws IOException {
        Map<String, List<String>> replacementPairs = 
                new HashMap<String, List<String>>();
        if (property != null && property.length() != 0) {
            final String[] replacements = property.split(", ?");
            for (final String stringPair : replacements) {
                final String[] twoStrings = stringPair.trim().split(" ");
                if (twoStrings.length == 2) { // check format
                    if (!replacementPairs.containsKey(twoStrings[0])) {
                        List<String> strList = new ArrayList<String>();
                        strList.add(twoStrings[1]);
                        replacementPairs.put(twoStrings[0],
                                strList);
                    } else {
                        replacementPairs.get(twoStrings[0]).
                        add(twoStrings[1]);
                    }
                } else {
                    throw new IOException("Attribute " + ATTR_NAME_REPLACEMENT_PAIRS
                            + " is not in the proper format. The equivalence has " + 
                            twoStrings.length +
                            " strings.");
                }
            }
        }
        return replacementPairs;
    }
}
