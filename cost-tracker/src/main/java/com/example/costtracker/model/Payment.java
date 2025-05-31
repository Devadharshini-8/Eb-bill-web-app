package com.example.costtracker.model;

import java.time.LocalDate;

public class Payment {
    private int paymentId;
    private int userId;
    private double totalAmount;
    private LocalDate dueDate;
    private boolean isPaid;
    private double fine;

    public Payment(int paymentId, int userId, double totalAmount, LocalDate dueDate, boolean isPaid, double fine) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.dueDate = dueDate;
        this.isPaid = isPaid;
        this.fine = fine;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public int getUserId() {
        return userId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public double getFine() {
        return fine;
    }

    public void setFine(double fine) {
        this.fine = fine;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }
}