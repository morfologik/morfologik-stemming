package morfologik.tools;

import java.util.Map;

import morfologik.tools.Launcher.ToolInfo;

import org.junit.Assert;
import org.junit.Test;

/*
 *
 */
public class LauncherTest {
	/* */
	@Test
	public void testTools() throws Exception {
		for (Map.Entry<String, ToolInfo> e : Launcher.initTools().entrySet()) {
			try {
				e.getValue().invoke(new String[] {"--help"});
			} catch (Throwable t) {
				Assert.fail("Unable to launch " + e.getKey() + ": "
				        + t.getMessage());
			}
		}
	}
}
