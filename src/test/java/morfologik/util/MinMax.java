package morfologik.util;

/**
 * Minimum/maximum and range.
 */
public final class MinMax
{
    public final int min;
    public final int max;
    
    public MinMax(int min, int max)
    {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
    }

    public int range()
    {
        return max - min;
    }
}