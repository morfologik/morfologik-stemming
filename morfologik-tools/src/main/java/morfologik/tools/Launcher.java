package morfologik.tools;


/**
 * JAR entry point.
 */
public final class Launcher {
  private Launcher() {}

  public static void main(String[] args) {
    CliTool.main(args, new FSABuild(),
                       new FSADump(),
                       new FSAInfo(),
                       new DictCompile(),
                       new DictDecompile());
  }
}
