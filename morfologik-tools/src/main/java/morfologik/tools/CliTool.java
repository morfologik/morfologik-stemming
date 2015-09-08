package morfologik.tools;

import java.io.PrintStream;
import java.util.concurrent.Callable;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;


/**
 * Base class for command-line applications.
 */
public abstract class CliTool implements Callable<ExitStatus> {
  @Parameter(
      names = {"--exit"},
      hidden = true,
      arity = 1,
      description = "Call System.exit() at the end of command processing.")
  private boolean callSystemExit = true;

  @Parameter(
      names = {"-h", "--help"},
      help = true,
      hidden = true,
      description = "Help about options and switches.")
  private boolean help;

  public CliTool() {
    if (!getClass().isAnnotationPresent(Parameters.class)) {
      throw new RuntimeException();
    }
  }

  /** Call {@link System#exit(int)} at the end of command processing. */
  public void setCallSystemExit(boolean flag) {
    this.callSystemExit = flag;
  }

  /**
   * Parse and execute command. 
   */
  protected static void main(String[] args, CliTool command) {
    JCommander jc = new JCommander(command);
    jc.addConverterFactory(new CustomParameterConverters());
    jc.setProgramName(command.getClass().getAnnotation(Parameters.class).commandNames()[0]);

    ExitStatus exitStatus = ExitStatus.SUCCESS;
    try {
      jc.parse(args);
      if (command.help) {
        helpDisplayCommandOptions(System.err, jc);
      } else {
        exitStatus = command.call();
      }
    } catch (MissingCommandException e) {
      System.err.println("Invalid argument: " + e);
      System.err.println();
      helpDisplayCommandOptions(System.err, jc);
      exitStatus = ExitStatus.ERROR_INVALID_ARGUMENTS;
    } catch (ParameterException e) {
      System.err.println("Invalid argument: " + e.getMessage());
      System.err.println();

      if (jc.getParsedCommand() == null) {
        helpDisplayCommandOptions(System.err, jc);
      } else {
        helpDisplayCommandOptions(System.err, jc.getParsedCommand(), jc);
      }
      exitStatus = ExitStatus.ERROR_INVALID_ARGUMENTS;
    } catch (Throwable t) {
      System.err.println("An unhandled exception occurred. Stack trace below.");
      t.printStackTrace(System.err);
      exitStatus = ExitStatus.ERROR_OTHER;
    }

    if (command.callSystemExit) {
      System.exit(exitStatus.code);
    }
  }

  protected static <T> T checkNotNull(T arg) {
    if (arg == null) {
      throw new IllegalArgumentException("Argument must not be null.");
    }
    return arg;
  }

  private static void helpDisplayCommandOptions(PrintStream pw, String command, JCommander jc) {
    StringBuilder sb = new StringBuilder();
    jc = jc.getCommands().get(command);
    jc.usage(sb, "");
    pw.print(sb);
  }

  private static void helpDisplayCommandOptions(PrintStream pw, JCommander jc) {
    StringBuilder sb = new StringBuilder();
    jc.usage(sb, "");
    pw.print(sb);
  }  
}
