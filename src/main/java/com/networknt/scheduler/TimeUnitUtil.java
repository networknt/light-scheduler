package com.networknt.scheduler;

public class TimeUnitUtil {
    public static long nextStartTimestamp(TimeUnit timeUnit, long start) {
        long nextTimestamp = start;
        switch (timeUnit) {
            case MILLISECONDS:
                break;

            case SECONDS:
                nextTimestamp = 1000 + 1000 * (start / 1000); // the next second is the start timestamp.
                break;

            case MINUTES:
                nextTimestamp = 60000 + 60000 * (start / 60000); // next minute is the start timestamp
                break;

            case HOURS:
                nextTimestamp = 3600000 + 3600000 * (start / 3600000); // next hour is the start timestamp
                break;

            case DAYS:
                nextTimestamp = 86400000 + 86400000 * (start / 86400000); // next day is the start timestamp
                break;
        }
        return nextTimestamp;
    }

    public static long oneTimeUnitMillisecond(java.util.concurrent.TimeUnit timeUnit) {
        long millisecond = 0;
        switch (timeUnit) {
            case MILLISECONDS:
                millisecond = 1;
                break;

            case SECONDS:
                millisecond = 1000;
                break;

            case MINUTES:
                millisecond = 60000;
                break;

            case HOURS:
                millisecond = 3600000;
                break;

            case DAYS:
                millisecond = 86400000;
                break;
        }
        return millisecond;
    }
}
