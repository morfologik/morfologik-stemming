package morfologik.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Resource management utilities.
 * 
 * @author Dawid Weiss
 */
public final class ResourceUtils {
    /**
     * No instances.
     */
    private ResourceUtils() {
    }

    /**
     * Returns an input stream to the resource.
     * 
     * @param resourcePath The path can be an URL, or a path
     * leading to a resource looked up using class loader and
     * system class loader.
     * 
     * @return InputStream instance.
     * @throws IOException If the resource could not be found
     * or opened.
     */
    public static InputStream openInputStream(String resourcePath) throws IOException {
        return getResourceURL(resourcePath).openStream();
    }

    /**
     * Returns an URL to the matching resource or throws an IOException if
     * no resource matches the path.
     */
    public static URL getResourceURL(String resourcePath)
        throws IOException
    {
        try {
            // try the URL first.
            final URL url = new URL(resourcePath);
            // success, load the resource.
            return url;
        } catch (MalformedURLException e) {
            // No luck. Fallback to class loader paths.
        }

        // try current thread's class loader first.
        final ClassLoader ldr = Thread.currentThread().getContextClassLoader();
        URL url = null;
        if ( (url = ldr.getResource(resourcePath)) != null) { 
            return url;
        } else if ((url = ResourceUtils.class.getResource(resourcePath)) != null) {
            return url;
        } else if ((url = ClassLoader.getSystemResource(resourcePath)) != null) {
            return url;
        } 

        // Try file path
        final File f = new File(resourcePath);
        if (f.exists() && f.isFile() && f.canRead()) {
            return f.toURI().toURL();
        } else {
            throw new IOException("Could not locate resource: " + resourcePath);
        }
    }    
}
