package morfologik.tools;

import com.beust.jcommander.Parameters;

@Parameters(
    hidden = true,
    commandNames = "fsa_dump",
    commandDescription = "Dumps all sequences encoded in an automaton.")
@Deprecated
public class FSADump extends FSADecompile {
}
