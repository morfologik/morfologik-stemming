package morfologik.tools;

import java.lang.reflect.Method;
import java.util.TreeMap;


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
     * Known tools.
     */
    static TreeMap<String, ToolInfo> tools;
    static {
	tools = new TreeMap<String, ToolInfo>();
	tools.put("dump", new ToolInfo(DumpTool.class,
		"Dump an FSA dictionary."));
	tools.put("plstem", new ToolInfo(PolishStemmingTool.class,
		"Apply Polish stemming to the input."));
    }

    /**
     * Command line entry point.
     */
    public static void main(String[] args) throws Exception {
	if (args.length == 0) {
	    System.out.println("Provide tool name and its command-line options. "
			    + "Available tools:");
	    for (String key : tools.keySet()) {
		final ToolInfo toolInfo = tools.get(key);
		System.out.println(String.format("  %-10s - %s", key, toolInfo.info));
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
}
