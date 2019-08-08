package others;

public class CheckHeapSize {
    private static long heapSize;
    private static long heapMaxSize;
    private static long heapFreeSize;

    public static String getHeapSize() {
        heapSize = Runtime.getRuntime().totalMemory();
        return formatSize(heapSize);
    }
    public static String getHeapMaxSize() {
        heapMaxSize = Runtime.getRuntime().maxMemory();
        return formatSize(heapMaxSize);
    }
    public static String getHeapFreeSize() {
        heapFreeSize = Runtime.getRuntime().freeMemory();
        return formatSize(heapFreeSize);
    }

    private static String formatSize(long v) {
        if (v < 1024) return v + " B";
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format("%.1f %sB", (double)v / (1L << (z*10)), " KMGTPE".charAt(z));
    }
}

