package morfologik.stemming;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

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
   * 
   * @param location The location of the dictionary file (<code>*.dict</code>).
   * @return An instantiated dictionary.
   * @throws IOException if an I/O error occurs.
   */
  public static Dictionary read(Path location) throws IOException {
    final Path metadata = DictionaryMetadata.getExpectedMetadataLocation(location);

    try (InputStream fsaStream = Files.newInputStream(location);
         InputStream metadataStream = Files.newInputStream(metadata)) {
      return read(fsaStream, metadataStream);
    }
  }

  /**
   * Attempts to load a dictionary using the URL to the FSA file and the
   * expected metadata extension.
   * 
   * @param dictURL The URL pointing to the dictionary file (<code>*.dict</code>).
   * @return An instantiated dictionary.
   * @throws IOException if an I/O error occurs.
   */
  public static Dictionary read(URL dictURL) throws IOException {
    final URL expectedMetadataURL;
    try {
      String external = dictURL.toExternalForm();
      expectedMetadataURL = new URL(DictionaryMetadata.getExpectedMetadataFileName(external));
    } catch (MalformedURLException e) {
      throw new IOException("Couldn't construct relative feature map URL for: " + dictURL, e);
    }

    try (InputStream fsaStream = dictURL.openStream();
         InputStream metadataStream = expectedMetadataURL.openStream()) {
      return read(fsaStream, metadataStream);
    }
  }

  /**
   * Attempts to load a dictionary from opened streams of FSA dictionary data
   * and associated metadata. Input streams are not closed automatically.
   * 
   * @param fsaStream The stream with FSA data
   * @param metadataStream The stream with metadata
   * @return Returns an instantiated {@link Dictionary}.
   * @throws IOException if an I/O error occurs.
   */
  public static Dictionary read(InputStream fsaStream, InputStream metadataStream) throws IOException {
    return new Dictionary(FSA.read(fsaStream), DictionaryMetadata.read(metadataStream));    
  }
}
