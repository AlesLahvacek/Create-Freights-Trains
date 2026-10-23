package com.lahvacek.freight_trains.util;

public final class StationCandidateTracker {
    private static final ThreadLocal<Boolean> STATION_CANDIDATE =
            ThreadLocal.withInitial(() -> false);

    private StationCandidateTracker() {
    }

    public static void setStationCandidate(boolean stationCandidate) {
        STATION_CANDIDATE.set(stationCandidate);
    }

    public static boolean isStationCandidate() {
        return STATION_CANDIDATE.get();
    }
}
