package morfologik.tools;

import java.util.Locale;

@SuppressWarnings("serial")
class ExitStatusException extends RuntimeException {
  final ExitStatus exitStatus;
  
  public ExitStatusException(ExitStatus status, String message, Object... args) {
    this(status, null, message, args);
  }

  public ExitStatusException(ExitStatus status, Throwable t, String message, Object... args) {
    super(String.format(Locale.ROOT, message, args), t);
    this.exitStatus = status;
  }
}
