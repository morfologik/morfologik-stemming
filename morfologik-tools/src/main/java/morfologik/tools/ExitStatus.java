package morfologik.tools;

public enum ExitStatus {
  /**
   * The command was successful. 
   */
  SUCCESS                     (0),
  
  /**
   * Unknown error cause.
   */
  ERROR_OTHER               (1),

  /**
   * Invalid input arguments or their combination.
   */
  ERROR_INVALID_ARGUMENTS     (2),
  
  /**
   * A potentially destructive command requires explicit confirmation that was
   * not present.
   */
  ERROR_CONFIRMATION_REQUIRED (3);

  public final int code;

  private ExitStatus(int systemExitCode) {
    this.code = systemExitCode;
  }
}
