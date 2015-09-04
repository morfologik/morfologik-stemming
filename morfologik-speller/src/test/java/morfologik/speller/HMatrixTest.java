package morfologik.speller;

import static org.junit.Assert.*;

import morfologik.speller.HMatrix;

import org.junit.Test;

public class HMatrixTest {
  private static final int MAX_WORD_LENGTH = 120;

  @Test
  public void stressTestInit() {
    for (int i = 0; i < 10; i++) { // test if we don't get beyond array limits etc.
      HMatrix H = new HMatrix(i, MAX_WORD_LENGTH);
      assertEquals(0, H.get(1, 1));
    }
  }
}
