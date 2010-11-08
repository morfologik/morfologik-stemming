package morfologik.util;

import java.lang.reflect.Array;

/**
 * Compatibility layer for JVM 1.5.
 */
public final class Arrays {
	private Arrays() {
		// No instances.
	}

	public static byte[] copyOf(byte[] original, int newLength) {
		byte[] copy = new byte[newLength];
		System.arraycopy(original, 0, copy, 0, Math.min(original.length,
		        newLength));
		return copy;
	}

	public static boolean[] copyOf(boolean[] original, int newLength) {
		boolean[] copy = new boolean[newLength];
		System.arraycopy(original, 0, copy, 0, Math.min(original.length,
		        newLength));
		return copy;
	}

	public static char[] copyOf(char[] original, int newLength) {
		char[] copy = new char[newLength];
		System.arraycopy(original, 0, copy, 0, Math.min(original.length,
		        newLength));
		return copy;
	}

	public static int[] copyOf(int[] original, int newLength) {
		int[] copy = new int[newLength];
		System.arraycopy(original, 0, copy, 0, Math.min(original.length,
		        newLength));
		return copy;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] copyOf(T[] original, int newLength) {
		return (T[]) copyOf(original, newLength, original.getClass());
	}

	@SuppressWarnings("unchecked")
	public static <T, U> T[] copyOf(U[] original, int newLength,
	        Class<? extends T[]> newType) {
		T[] copy = ((Object) newType == (Object) Object[].class) ? (T[]) new Object[newLength]
		        : (T[]) Array
		                .newInstance(newType.getComponentType(), newLength);
		System.arraycopy(original, 0, copy, 0, Math.min(original.length,
		        newLength));
		return copy;
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
	 * Convert an array of strings to bytes. JDK1.5-equivalent of {@link Arrays#copyOf(byte[], int)}
	 * and then {@link java.util.Arrays#toString(byte[])}.
	 */
	public static String toString(byte [] bytes, int length)
	{
		if (bytes.length != length)
		{
			final byte [] sub = new byte [length];
			System.arraycopy(bytes, 0, sub, 0, length);
			bytes = sub;
		}
		return java.util.Arrays.toString(bytes);
	}
}
