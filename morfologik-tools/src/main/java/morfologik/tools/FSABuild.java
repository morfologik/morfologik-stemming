package morfologik.tools;

import com.beust.jcommander.Parameters;

@Parameters(
    hidden = true,
    commandNames = "fsa_build",
    commandDescription = "Builds finite state automaton from \\n-delimited input.")
@Deprecated
public class FSABuild extends FSACompile {
}
