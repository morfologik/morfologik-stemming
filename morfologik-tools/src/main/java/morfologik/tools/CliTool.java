package morfologik.tools;

import java.io.PrintStream;
import java.util.List;
import java.util.Locale;
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
  protected final static String ARG_OVERWRITE = "--overwrite";
  protected final static String ARG_VALIDATE = "--validate";

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

  /**
   * Call {@link System#exit(int)} at the end of command processing.
   * 
   * @param flag Call {@link System#exit(int)} if <code>true</code>.
   */
  public void setCallSystemExit(boolean flag) {
    this.callSystemExit = flag;
  }

  /**
   * Parse and execute one of the commands.
   * 
   * @param args Command line arguments (command and options).
   * @param commands A list of commands.
   */
  protected static void main(String[] args, CliTool... commands) {
    if (commands.length == 1) {
      main(args, commands[0]);
    } else {
      JCommander jc = new JCommander();
      for (CliTool command : commands) {
        jc.addCommand(command);
      }
      jc.addConverterFactory(new CustomParameterConverters());
      jc.setProgramName("");
  
      ExitStatus exitStatus = ExitStatus.SUCCESS;
      try {
        jc.parse(args);

        final String commandName = jc.getParsedCommand();
        if (commandName == null) {
          helpDisplayCommandOptions(System.err, jc);
        } else {
          List<Object> objects = jc.getCommands().get(commandName).getObjects();
          if (objects.size() != 1) {
            throw new RuntimeException();
          }

          CliTool command = CliTool.class.cast(objects.get(0));
          exitStatus = command.call();
          if (command.callSystemExit) {
            System.exit(exitStatus.code);
          }
        }
      } catch (ExitStatusException e) {
        System.err.println(e.getMessage());
        if (e.getCause() != null) {
          e.getCause().printStackTrace(System.err);
        }
        exitStatus = e.exitStatus;
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
    }
  }

  /**
   * Parse and execute a single command.
   *  
   * @param args Command line arguments (command and options).
   * @param command The command to execute.
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
    } catch (ExitStatusException e) {
      System.err.println(e.getMessage());
      if (e.getCause() != null) {
        e.getCause().printStackTrace(System.err);
      }
      exitStatus = e.exitStatus;
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

  protected static void printf(String msg, Object... args) {
    System.out.println(String.format(Locale.ROOT, msg, args));
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
