package net.twentyytwo.cleanertooltips.util;

public enum Comparison {
    NONE,
    HIGHER,
    LOWER;

    public static Comparison getComparison(double entryValue, double comparedValue, boolean isPositiveSentiment) {
        if (isPositiveSentiment ? (entryValue > comparedValue) : (entryValue < comparedValue)) {
            return HIGHER;
        } else if (isPositiveSentiment ? (entryValue < comparedValue) : (entryValue > comparedValue)) {
            return LOWER;
        } else {
            return NONE;
        }
    }
}
