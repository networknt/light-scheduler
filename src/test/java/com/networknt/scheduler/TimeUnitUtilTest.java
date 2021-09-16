package com.networknt.scheduler;

import org.junit.Test;

public class TimeUnitUtilTest {
    @Test
    public void testNextSecond() {
        long start = System.currentTimeMillis();
        System.out.println("start = " + start);
        long nextTimestamp = TimeUnitUtil.nextStartTimestamp(TimeUnit.SECONDS, start);
        System.out.println("next second = " + nextTimestamp);
    }

    @Test
    public void testNextMinute() {
        long start = System.currentTimeMillis();
        System.out.println("start = " + start);
        long nextTimestamp = TimeUnitUtil.nextStartTimestamp(TimeUnit.MINUTES, start);
        System.out.println("next minute = " + nextTimestamp);
    }

    @Test
    public void testNextHour() {
        long start = System.currentTimeMillis();
        System.out.println("start = " + start);
        long nextTimestamp = TimeUnitUtil.nextStartTimestamp(TimeUnit.HOURS, start);
        System.out.println("next hour = " + nextTimestamp);
    }

    @Test
    public void testNextDay() {
        long start = System.currentTimeMillis();
        System.out.println("start = " + start);
        long nextTimestamp = TimeUnitUtil.nextStartTimestamp(TimeUnit.DAYS, start);
        System.out.println("next day = " + nextTimestamp);
    }

}
