package morfologik.util;

import java.io.*;
import java.net.*;

/**
 * Resource management utilities.
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
	 * @param resource
	 *            The path leading to the resource. Can be an URL, a path
	 *            leading to a class resource or a {@link File}.
	 * 
	 * @return InputStream instance.
	 * @throws IOException
	 *             If the resource could not be found or opened.
	 */
	public static InputStream openInputStream(String resource)
	        throws IOException {
		try {
			// See if the resource is an URL first.
			final URL url = new URL(resource);
			// success, load the resource.
			return url.openStream();
		} catch (MalformedURLException e) {
			// No luck. Fallback to class loader paths.
		}

		// Try current thread's class loader first.
		final ClassLoader ldr = Thread.currentThread().getContextClassLoader();

		InputStream is;
		if (ldr != null && (is = ldr.getResourceAsStream(resource)) != null) {
			return is;
		} else if ((is = ResourceUtils.class.getResourceAsStream(resource)) != null) {
			return is;
		} else if ((is = ClassLoader.getSystemResourceAsStream(resource)) != null) {
			return is;
		}

		// Try file path
		final File f = new File(resource);
		if (f.exists() && f.isFile() && f.canRead()) {
			return new FileInputStream(f);
		}

		throw new IOException("Could not locate resource: " + resource);
	}
}
