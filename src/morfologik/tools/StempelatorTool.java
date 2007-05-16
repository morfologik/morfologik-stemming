package morfologik.tools;

import java.io.IOException;

import morfologik.stemmers.Stempelator;

/**
 * Command-line utility for stemming text using {@link Stempelator}.
 * 
 * @see TextStemmingTool
 * @author Dawid Weiss
 */
public final class StempelatorTool extends TextStemmingTool {
    /**
     * 
     */
    public StempelatorTool() throws IOException {
        super(new Stempelator());
    }

    /** 
     * Program's entry point.
     */
    public static void main(String[] args) throws Exception {
        final StempelatorTool tool = new StempelatorTool();
        tool.go(args);
    }
}