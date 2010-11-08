package morfologik.tools;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.jar.Manifest;

import morfologik.util.FileUtils;

/**
 * A launcher for other command-line tools.
 */
public final class Launcher {
	/**
	 * Tool description.
	 */
	final static class ToolInfo {
		public final Class<? extends Tool> clazz;
		public final String info;

		public ToolInfo(Class<? extends Tool> clazz, String info) {
			this.clazz = clazz;
			this.info = info;
		}

		public void invoke(String[] subArgs) throws Exception {
			final Method m = clazz.getMethod("main",
			        new Class[] { String[].class });
			m.invoke(null, new Object[] { subArgs });
		}
	}

	/**
	 * Command line entry point.
	 */
	public static void main(String[] args) throws Exception {
		// If so, tools are unavailable and a classpath error has been logged.
		final TreeMap<String, ToolInfo> tools = initTools();

		if (tools == null)
		{
			return;
		}

		if (args.length == 0) {
			System.out
			        .println("Provide tool name and its command-line options. "
			                + "Available tools:");
			for (String key : tools.keySet()) {
				final ToolInfo toolInfo = tools.get(key);
				System.out.println(String.format("  %-10s - %s", key,
				        toolInfo.info));
			}
		} else {
			final String toolName = args[0];
			if (!tools.containsKey(toolName)) {
				System.out.println("Unknown tool: " + toolName);
				return;
			}

			final String[] subArgs = new String[args.length - 1];
			System.arraycopy(args, 1, subArgs, 0, subArgs.length);

			final ToolInfo toolInfo = (ToolInfo) tools.get(toolName);
			toolInfo.invoke(subArgs);
		}
	}

	/**
	 * Initialize and check tools' availability.
	 */
	static TreeMap<String, ToolInfo> initTools() {
		TreeMap<String, ToolInfo> tools = new TreeMap<String, ToolInfo>();

		tools.put("fsa_build", new ToolInfo(FSABuildTool.class,
		        "Create an automaton from plain text files."));

		tools.put("fsa_dump", new ToolInfo(FSADumpTool.class,
		        "Dump an FSA dictionary."));

		tools.put("tab2morph", new ToolInfo(MorphEncodingTool.class,
		        "Convert tabbed dictionary to fsa encoding format."));

		tools.put("plstem", new ToolInfo(PolishStemmingTool.class,
		        "Apply Polish dictionary stemming to the input."));

		// Prune unavailable tools.
		for (Iterator<ToolInfo> i = tools.values().iterator(); i.hasNext();) {
			ToolInfo ti = i.next();
			try {
				ti.clazz.newInstance().isAvailable();
			} catch (NoClassDefFoundError e) {
				logJarWarning();
				return null;
			} catch (Throwable e) {
				System.out.println("Tools could not be initialized because" +
						" of an exception during initialization: "
						+ e.getClass().getName() + ", " + e.getMessage());
				return null;
			}
		}
		
		return tools;
	}
	
	/**
	 * Log a warning about missing JAR dependencies.
	 */
	private static void logJarWarning() {
		System.out.println("Tools are unavailable, at least one JAR dependency missing.");

		try {
			final Class<Launcher> clazz = Launcher.class;
			final ClassLoader classLoader = clazz.getClassLoader();

			final String clazzName = clazz.getName().replace('.', '/') + ".class";
			// Figure out our own class path location.
			final URL launcherLocation = classLoader.getResource(clazzName);
			if (launcherLocation == null)
				return;
			
			String launcherPrefix = launcherLocation.toString()
				.replace(clazzName, "");

			// Figure our our location's MANIFEST.MF (class loader may be hitting a few).
			URL manifestResource = null;
    		Enumeration<URL> manifests = classLoader.getResources("META-INF/MANIFEST.MF");
    		while (manifests.hasMoreElements())
    		{
    			URL candidate = manifests.nextElement();
    			if (candidate.toString().startsWith(launcherPrefix))
    			{
    				manifestResource = candidate;
    				break;
    			}
    		}
    		
    		if (manifestResource == null)
    			return;

			InputStream stream = null;
			try {
				stream = manifestResource.openStream();
				Manifest manifest = new Manifest(stream);
				
				System.out.println("Required JARs: "
						+ manifest.getMainAttributes().getValue("Class-Path"));
			} catch (IOException e) {
				FileUtils.close(stream);
			}
		} catch (IOException e) {
			// Ignore.
		}
    }
}
