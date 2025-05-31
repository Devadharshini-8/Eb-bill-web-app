package com.example.costtracker.model;

import java.time.LocalDateTime;

public class Fine {
    private int fineId;
    private int paymentId;
    private double fineAmount;
    private boolean isPaid;
    private LocalDateTime createdAt;

    public Fine(int fineId, int paymentId, double fineAmount, boolean isPaid, LocalDateTime createdAt) {
        this.fineId = fineId;
        this.paymentId = paymentId;
        this.fineAmount = fineAmount;
        this.isPaid = isPaid;
        this.createdAt = createdAt;
    }

    public int getFineId() {
        return fineId;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }
}