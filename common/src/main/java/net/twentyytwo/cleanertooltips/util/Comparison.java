package net.twentyytwo.cleanertooltips.util;

public enum Comparison {
    NONE,
    HIGHER,
    LOWER;

    public static Comparison getComparison(double entryValue, double comparedValue) {
        if (entryValue > comparedValue) {
            return HIGHER;
        } else if (entryValue < comparedValue) {
            return LOWER;
        } else {
            return NONE;
        }
    }
}
