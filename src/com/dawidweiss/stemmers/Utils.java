package com.dawidweiss.stemmers;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.io.FileInputStream;
import java.io.File;

/**
 * Utility methods.
 * 
 * @author Dawid Weiss
 */
final class Utils {
    
    /**
     * No instances.
     */
    private Utils() {
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
    public static InputStream getInputStream(String resourcePath) throws IOException {
        try {
            // try the URL first.
            URL url = new URL(resourcePath);
            // success, load the resource.
            InputStream is = url.openStream();
            return is;
        } catch (MalformedURLException e) {
            // no luck. Fallback to class loader paths.
        }
        
        // try current thread's class loader first.
        ClassLoader ldr = Thread.currentThread().getContextClassLoader();
        InputStream is = null;
        if ( (is = ldr.getResourceAsStream(resourcePath)) != null) { 
            return is;
        } else if ( (is = Utils.class.getResourceAsStream(resourcePath)) != null) {
            return is;
        } else if ( (is = ClassLoader.getSystemResourceAsStream(resourcePath)) != null) {
            return is;
        } 
        
        // try file path
        File f = new File(resourcePath);
        if (f.exists() && f.isFile() && f.canRead()) {
	        return new FileInputStream(f);
        } else
        	throw new IOException("Could not open input stream from URL/ resource/ file: " + resourcePath);
    }
    
}
