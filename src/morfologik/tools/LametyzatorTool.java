package morfologik.tools;

import java.io.IOException;

import morfologik.stemming.Lametyzator;

/**
 * Command-line utility for stemming text using {@link Lametyzator}.
 * 
 * @see TextStemmingTool
 */
public final class LametyzatorTool extends TextStemmingTool {
    /**
     * 
     */
    public LametyzatorTool() throws IOException {
        super(new Lametyzator());
    }

    /** 
     * Program's entry point.
     */
    public static void main(String[] args) throws Exception {
        final LametyzatorTool tool = new LametyzatorTool();
        tool.go(args);
    }
}