package morfologik.tools;


/**
 * JAR entry point.
 */
public final class Launcher {
  private Launcher() {}

  @SuppressWarnings("deprecation")
  public static void main(String[] args) {
    CliTool.main(args, new FSACompile(),
                       new FSADump(),
                       new FSADecompile(),
                       new FSABuild(),
                       new FSAInfo(),
                       new DictCompile(),
                       new DictDecompile(),
                       new DictApply());
  }
}
