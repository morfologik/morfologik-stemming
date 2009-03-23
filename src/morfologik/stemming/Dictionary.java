package morfologik.stemming;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import morfologik.fsa.FSA;
import morfologik.util.FileUtils;
import morfologik.util.ResourceUtils;

/**
 * <p>
 * A dictionary combines {@link FSA} automaton and metadata describing the
 * internals of dictionary entries' coding ({@link DictionaryFeatures}.
 * 
 * <p>
 * A dictionary consists of two files:
 * <ul>
 * <li>an actual compressed FSA file,
 * <li>a metadata file, describing the dictionary.
 * </ul>
 * Use static methods in this class to read dictionaries and their metadata.
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
    public final DictionaryFeatures metadata;

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
     *            see {@link DictionaryFeatures}.
     */
    public Dictionary(FSA fsa, DictionaryFeatures metadata) {
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

	FileUtils.assertExists(featuresFile, true, false);

	return readAndClose(new FileInputStream(fsaFile), new FileInputStream(
		featuresFile));
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

	return readAndClose(ResourceUtils.openInputStream(fsa), ResourceUtils
		.openInputStream(features));
    }

    /**
     * Attempts to load a dictionary from opened streams of FSA dictionary data
     * and associated metadata.
     */
    public static Dictionary readAndClose(InputStream fsaData,
	    InputStream featuresData) throws IOException {
	try {
	    final Properties properties = new Properties();
	    properties.load(featuresData);

	    final DictionaryFeatures features = DictionaryFeatures
		    .fromMap(properties);
	    final FSA fsa = FSA.getInstance(fsaData, features.encoding);

	    return new Dictionary(fsa, features);
	} finally {
	    FileUtils.close(fsaData, featuresData);
	}
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
}
