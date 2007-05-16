package morfologik.fsa.dictionary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import morfologik.fsa.core.FSA;
import morfologik.util.FileUtils;

/**
 * <p>A dictionary combines {@link FSA} automaton and metadata
 * describing the internals of dictionary entries' coding
 * ({@link DictionaryFeatures}.
 * 
 * <p>A dictionary consists of two files: 
 *   <ul>
 *      <li>an actual compressed FSA file,
 *      <li>a metadata file, describing the dictionary.
 *   </ul>
 * Use static methods in this class to read dictionaries and
 * their metadata.
 * 
 * @author Dawid Weiss
 */
public class Dictionary {
    /**
     * Features file suffix.
     */
    private final static String FEATURES_SUFFIX = "info";

    /**
     * {@link FSA} automaton with the compiled dictionary data.
     */
    public final FSA fsa;

    /**
     * Features of the dictionary.
     */
    public final DictionaryFeatures features;

    /**
     * It is strongly recommended to use static methods in this class
     * for reading dictionaries.
     * 
     * @param fsa An instantiated {@link FSA} instance.
     * 
     * @param features A map of attributes describing the compression format
     *      and other settings not contained in the FSA automaton.
     *      For an explanation of available attributes and their possible values, see
     *      {@link DictionaryFeatures}.
     */
    public Dictionary(FSA fsa, DictionaryFeatures features) {
        this.fsa = fsa;
        this.features = features;
    }

    /**
     * Attempts to load a dictionary based on the path to the fsa file (the
     * expected suffix of the FSA file is <code>.dict</code>.
     */
    public static Dictionary read(File fsaFile) throws IOException {
        final File featuresFile = new File(fsaFile.getParent(), 
                getExpectedFeaturesName(fsaFile.getName()));
        FileUtils.assertExists(featuresFile, true, false);

        final InputStream featuresData = new FileInputStream(featuresFile);
        final InputStream fsaData = new FileInputStream(fsaFile);
        try {
            return read(fsaData, featuresData);
        } finally {
            featuresData.close();
            fsaData.close();
        }
    }

    /**
     * <p>Attempts to load a dictionary based on the URL to the FSA file (the
     * expected suffix of the FSA file is <code>.dict</code>.
     * 
     * <p>This method can be used to load resource-based dictionaries as well.
     */
    public static Dictionary read(URL fsaURL) throws IOException {
        final URL featuresURL = new URL(getExpectedFeaturesName(fsaURL.toExternalForm()));

        final InputStream featuresData;
        try {
            featuresData = featuresURL.openStream();
        } catch (IOException e) {
            throw new IOException("Could not read dictionary features file: "
                    + e.getMessage());
        }

        final InputStream fsaData;
        try {
            fsaData = fsaURL.openStream();
        } catch (IOException e) {
            throw new IOException("Could not read FSA dictionry file: " 
                    + e.getMessage());
        }

        try {
            return read(fsaData, featuresData);
        } finally {
            featuresData.close();
            fsaData.close();
        }
    }
    
    /**
     * Attempts to load a dictionary based on the stream of the fsa dictionary 
     * and the metadata (features) file.
     */
    public static Dictionary read(InputStream fsaData, InputStream featuresData) throws IOException {
        final Properties properties = new Properties();
        properties.load(featuresData);

        final DictionaryFeatures features = DictionaryFeatures.fromMap(properties);
        final FSA fsa = FSA.getInstance(fsaData, features.encoding);
        return new Dictionary(fsa, features);
    }

    /**
     * Returns the expected name of the features file, based on the name
     * of the FSA dictionary file. The expected name is resolved by truncating
     * any suffix of <code>name</code> and appending {@link #FEATURES_SUFFIX}.
     */
    private static String getExpectedFeaturesName(String name) {
        final int dotIndex = name.lastIndexOf('.');
        final String featuresName;
        if (dotIndex >= 0) {
            featuresName = name.substring(0, dotIndex) + "." + FEATURES_SUFFIX;
        } else {
            featuresName = name + "." + FEATURES_SUFFIX;
        }

        return featuresName;
    }
}
