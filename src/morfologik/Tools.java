package morfologik;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.TreeMap;

import morfologik.tools.FSADumpTool;
import morfologik.tools.LametyzatorTool;

/**
 * A single class for launching command-line tools
 * (for placing in the <code>Main-Class</code> attribute
 * of the distribution JAR file).
 */
public final class Tools {
    /**
     * Tool description.
     */
    private final static class ToolInfo {
        public final Class clazz;
        public final String info;
        
        public ToolInfo(Class clazz, String info) {
            this.clazz = clazz;
            this.info = info;
        }
    }

    /**
     * Returns the known tools.
     */
    private static TreeMap getTools() {
        final TreeMap tools = new TreeMap();

        tools.put("dump",
                new ToolInfo(
                        FSADumpTool.class, 
                        "Dumps the content of FSA dictionaries."));

        tools.put("lametyzator",
                new ToolInfo(
                        LametyzatorTool.class, 
                        "Process text files with Lametyzator stemmer."));

        return tools;
    }

    /**
     * Command line entry point.
     */
    public static void main(String [] args) throws Exception {
        final TreeMap tools = getTools();

        if (args.length == 0) {
            System.out.println(
                    "Provide tool name and its command-line options.\n"
                    + "Available tools:");
            final Iterator i = tools.keySet().iterator();
            while (i.hasNext()) {
                final String toolAlias = (String) i.next();
                final ToolInfo toolInfo = (ToolInfo) tools.get(toolAlias);
                System.out.println(
                        "  " + toolAlias + "\t- " + toolInfo.info + "\n");
            }
        } else {
            final String toolName = args[0];
            if (!tools.containsKey(toolName)) {
                System.out.println("Tool unknown: " + toolName);
                return;
            }

            final String [] subArgs = new String [args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, subArgs.length);

            final ToolInfo toolInfo = (ToolInfo) tools.get(toolName);
            final Method m = toolInfo.clazz.getMethod("main", new Class [] {String[].class});
            m.invoke(null, new Object [] {subArgs});
        }
    }
}
