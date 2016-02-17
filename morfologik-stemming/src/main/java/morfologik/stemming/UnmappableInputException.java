package morfologik.stemming;

import java.nio.charset.CharacterCodingException;

/**
 * Thrown when some input cannot be mapped using the declared charset (bytes
 * to characters or the other way around).
 */
@SuppressWarnings("serial")
public final class UnmappableInputException extends Exception {
  UnmappableInputException(String message, CharacterCodingException cause) {
    super(message, cause);
  }
}
