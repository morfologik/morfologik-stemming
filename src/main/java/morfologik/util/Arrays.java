package morfologik.util;

/**
 * Compatibility layer for JVM 1.5.
 */
public final class Arrays {
	private Arrays() {
		// No instances.
	}

	/**
	 * Compare two lists of objects for reference-equality.
	 */
	public static boolean referenceEquals(Object[] a1, int a1s, Object[] a2, int a2s, int length) {
		for (int i = 0; i < length; i++)
            if (a1[a1s++] != a2[a2s++])
				return false;

		return true;
	}

	/**
     * Compare two arrays for equality.
     */
    public static boolean equals(byte[] a1, int a1s, byte [] a2, int a2s, int length) {
        for (int i = 0; i < length; i++)
            if (a1[a1s++] != a2[a2s++])
                return false;

        return true;
    }

    /**
     * Compare two arrays for equality.
     */
    public static boolean equals(boolean[] a1, int a1s, boolean[] a2, int a2s, int length) {
        for (int i = 0; i < length; i++)
            if (a1[a1s++] != a2[a2s++])
                return false;

        return true;
    }

    /**
     * Compare two arrays for equality.
     */
    public static boolean equals(int[] a1, int a1s, int[] a2, int a2s, int length) {
        for (int i = 0; i < length; i++)
            if (a1[a1s++] != a2[a2s++])
                return false;

        return true;
    }

	/**
	 * Convert an array of strings to bytes.
	 */
	public static String toString(byte [] bytes, int start, int length)
	{
		if (bytes.length != length)
		{
			final byte [] sub = new byte [length];
			System.arraycopy(bytes, start, sub, 0, length);
			bytes = sub;
		}
		return java.util.Arrays.toString(bytes);
	}
}
