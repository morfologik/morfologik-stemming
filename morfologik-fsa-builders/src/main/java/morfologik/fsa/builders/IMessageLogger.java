package morfologik.fsa.builders;

public interface IMessageLogger {
  /**
   * Start a larger computation part.
   * 
   * @param name The name of the computation part. 
   */
  public void startPart(String name);

  /**
   * End a larger computation part.
   */
  public void endPart();

  /**
   * Log a message with arguments.
   * 
   * @param msg The message.
   * @param args Arguments to be passed to {@link String#format(java.util.Locale, String, Object...)}.
   */
  public void log(String msg, Object... args);
}