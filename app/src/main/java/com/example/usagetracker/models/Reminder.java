package com.example.usagetracker.models;

public class Reminder {
    private String message;
    private String frequency;
    private int hour;
    private int minute;

    public Reminder() {}

    public Reminder(String message, String frequency, int hour, int minute) {
        this.message = message;
        this.frequency = frequency;
        this.hour = hour;
        this.minute = minute;
    }

    public String getMessage() { return message; }
    public String getFrequency() { return frequency; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }
}