package morfologik.tools;

import com.carrotsearch.hppc.ByteArrayList;

public final class MorphEncoder2 {
    private MorphEncoder2() {}

    /**
     * Maximum encodable single-byte code.
     */
    private static final int REMOVE_EVERYTHING = 255;

    public static ByteArrayList encodeSuffix(ByteArrayList src, ByteArrayList dst, ByteArrayList encoded)
    {
        int sharedPrefix = sharedPrefixLength(src, dst);
        int truncateBytes = src.size() - sharedPrefix;
        if (truncateBytes >= REMOVE_EVERYTHING) {
            truncateBytes = REMOVE_EVERYTHING;
            sharedPrefix = 0;
        }

        final byte suffixTrimCode = (byte) (truncateBytes + 'A');
        encoded.add(suffixTrimCode);
        encoded.add(dst.buffer, sharedPrefix, dst.size() - sharedPrefix);

        return encoded;
    }

    public static ByteArrayList decodeSuffix(ByteArrayList src, ByteArrayList encoded, ByteArrayList dst)
    {
        int suffixTrimCode = encoded.get(0);
        int truncateBytes = (suffixTrimCode - 'A') & 0xFF;
        if (truncateBytes == REMOVE_EVERYTHING) {
            truncateBytes = src.size();
        }

        dst.add(src.buffer, 0, src.size() - truncateBytes);
        dst.add(encoded.buffer, 1, encoded.size() - 1);

        return dst;
    }

    /**
     * Compute the length of the shared prefix between two byte sequences.
     */
    private static int sharedPrefixLength(ByteArrayList a, ByteArrayList b) {
        final int max = Math.min(a.size(), b.size());
        int i = 0;
        while (i < max && a.get(i) == b.get(i)) {
            i++;
        }
        return i;
    }
}
