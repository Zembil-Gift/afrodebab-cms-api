package com.afrodebab.cms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@ConfigurationProperties(prefix = "app.attendance")
public class AttendancePolicyProperties {
    private LocalTime entryTime = LocalTime.of(9, 0);
    private LocalTime exitTime = LocalTime.of(17, 0);
    private LocalTime lunchStartTime = LocalTime.of(12, 30);
    private LocalTime lunchEndTime = LocalTime.of(13, 30);
    private int graceMinutes = 30;
    private int maxLunchBreakMinutes = 70;

    public LocalTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalTime exitTime) {
        this.exitTime = exitTime;
    }

    public LocalTime getLunchStartTime() {
        return lunchStartTime;
    }

    public void setLunchStartTime(LocalTime lunchStartTime) {
        this.lunchStartTime = lunchStartTime;
    }

    public LocalTime getLunchEndTime() {
        return lunchEndTime;
    }

    public void setLunchEndTime(LocalTime lunchEndTime) {
        this.lunchEndTime = lunchEndTime;
    }

    public int getGraceMinutes() {
        return graceMinutes;
    }

    public void setGraceMinutes(int graceMinutes) {
        this.graceMinutes = graceMinutes;
    }

    public int getMaxLunchBreakMinutes() {
        return maxLunchBreakMinutes;
    }

    public void setMaxLunchBreakMinutes(int maxLunchBreakMinutes) {
        this.maxLunchBreakMinutes = maxLunchBreakMinutes;
    }
}
