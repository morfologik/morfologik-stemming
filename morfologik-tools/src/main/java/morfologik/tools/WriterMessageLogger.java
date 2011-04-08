package morfologik.tools;

import java.io.PrintWriter;
import java.util.*;

import morfologik.fsa.IMessageLogger;

/**
 * A logger dumping info to <code>System.err</code>.
 */
public class WriterMessageLogger implements IMessageLogger {
    /**
     * Start of the world timestamp.
     */
    private final static long world = System.currentTimeMillis();

    /**
     * A single part: name, start timestamp.
     */
    private static class Part {
        final String name;
        final long start;

        Part(String name, long start) {
            this.name = name;
            this.start = start;
        }
    }

    /**
     * Is the output currently indented?
     */
    private boolean indent;

    /**
     * Active parts.
     */
    private ArrayDeque<Part> parts = new ArrayDeque<Part>();

    /**
     * Output writer.
     */
    private final PrintWriter writer;
    
    /**
     * 
     */
    public WriterMessageLogger(PrintWriter w) {
        this.writer = w;
    }
    
    /* 
     *
     */
    @Override
    public void log(String msg) {
        cancelIndent();

        writer.println(msg);
        writer.flush();
    }

    /* 
     *
     */
    @Override
    public void log(String header, Object v) {
        cancelIndent();
    
        if (v instanceof Integer || v instanceof Long) {
            writer.println(String.format(Locale.ENGLISH, "%-30s  %,11d", header, v));
        } else {
            writer.println(String.format(Locale.ENGLISH, "%-30s  %11s", header, v.toString()));
        }
        writer.flush();
    }

    /* 
     *
     */
    @Override
    public void startPart(String header) {
        cancelIndent();

        Part p = new Part(header, System.currentTimeMillis());
        parts.addLast(p);

        writer.print(String.format(Locale.ENGLISH, "%-30s", p.name + "..."));
        writer.flush();

        indent = true;
    }

    /* 
     *
     */
    @Override
    public void endPart() {
        long now = System.currentTimeMillis();
        Part p = parts.removeLast();

        if (!indent) {
            writer.print(String.format(Locale.ENGLISH, "%-30s", p.name + "..."));
        }

        writer.println(
                String.format(Locale.ENGLISH, "%13.2f sec.  [%6.2f sec.]", 
                (now - p.start) / 1000.0,
                (now - world) / 1000.0));
        writer.flush();

        indent = false;
    }

    /*
     * 
     */
    private void cancelIndent() {
        if (indent) {
            System.err.println();
        }

        indent = false;
    }
}
