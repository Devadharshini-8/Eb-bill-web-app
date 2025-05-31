package com.example.costtracker.model;

import java.time.LocalDateTime;

public class Notification {
    private int notificationId;
    private int userId;
    private String message;
    private LocalDateTime createdAt;

    public Notification(int notificationId, int userId, String message, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.message = message;
        this.createdAt = createdAt;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public int getUserId() {
        return userId;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}