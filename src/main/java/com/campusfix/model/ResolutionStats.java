package com.campusfix.model;

/** Time-to-resolution figures (minutes) over all resolved complaints. */
public record ResolutionStats(int resolvedCount, double averageMinutes, long minMinutes, long maxMinutes) {
    public static ResolutionStats none() {
        return new ResolutionStats(0, 0, 0, 0);
    }
}
