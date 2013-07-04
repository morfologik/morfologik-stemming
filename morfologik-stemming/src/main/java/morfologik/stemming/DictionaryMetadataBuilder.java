package morfologik.stemming;

import java.nio.charset.Charset;
import java.util.EnumMap;

/**
 * Helper class to build {@link DictionaryMetadata} instances.
 */
public final class DictionaryMetadataBuilder {
    private final EnumMap<DictionaryAttribute, String> attrs 
        = new EnumMap<DictionaryAttribute, String>(DictionaryAttribute.class);

    public DictionaryMetadataBuilder separator(char c) {
        this.attrs.put(DictionaryAttribute.SEPARATOR, Character.toString(c));
        return this;
    }

    public DictionaryMetadataBuilder encoding(Charset charset) {
        return encoding(charset.name());
    }

    public DictionaryMetadataBuilder encoding(String charsetName) {
        this.attrs.put(DictionaryAttribute.ENCODING, charsetName);
        return this;
    }

    public DictionaryMetadataBuilder usesPrefixes() {
        this.attrs.put(DictionaryAttribute.USES_PREFIXES, Boolean.TRUE.toString());
        return this;
    }

    public DictionaryMetadataBuilder usesInfixes() {
        this.attrs.put(DictionaryAttribute.USES_INFIXES, Boolean.TRUE.toString());
        return this;
    }

    public DictionaryMetadata build() {
        return new DictionaryMetadata(attrs);
    }
}
