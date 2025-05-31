package com.example.costtracker.model;

import java.time.LocalDateTime;

public class Usage {
    private int usageId;
    private int userId;
    private double hours;
    private double cost;
    private LocalDateTime timestamp;

    public Usage(int usageId, int userId, double hours, double cost, LocalDateTime timestamp) {
        this.usageId = usageId;
        this.userId = userId;
        this.hours = hours;
        this.cost = cost;
        this.timestamp = timestamp;
    }

    public int getUsageId() {
        return usageId;
    }

    public int getUserId() {
        return userId;
    }

    public double getHours() {
        return hours;
    }

    public double getCost() {
        return cost;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void calculateCost() {
        this.cost = hours * 14;
    }
}