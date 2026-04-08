package morfologik.fsa.builders;

import java.util.function.Predicate;

import com.carrotsearch.randomizedtesting.jupiter.DetectThreadLeaks;
import com.carrotsearch.randomizedtesting.jupiter.Randomized;

@Randomized
@DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
@DetectThreadLeaks.LingerTime(millis = 5 * 1000)
@DetectThreadLeaks.ExcludeThreads(TestBase.CustomThreadFilter.class)
public abstract class TestBase {
  /**
   * Any custom thread filters we should ignore.
   */
  public static class CustomThreadFilter implements Predicate<Thread> {
    @Override
    public boolean test(Thread t) {
      // IBM J9 bogus threads.
      String threadName = t.getName();
      if ("Attach API wait loop".equals(threadName) ||
          "file lock watchdog".equals(threadName)   ||
          "ClassCache Reaper".equals(threadName)) {
        return true;
      }

      return false;
    }
  }
}
