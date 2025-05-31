package com.example.costtracker.model;

import java.time.YearMonth;

public class Budget {
    private int budgetId;
    private int userId;
    private double budgetAmount;
    private String monthYear;

    public Budget(int budgetId, int userId, double budgetAmount, String monthYear) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.budgetAmount = budgetAmount;
        this.monthYear = monthYear;
    }

    public int getBudgetId() {
        return budgetId;
    }

    public int getUserId() {
        return userId;
    }

    public double getBudgetAmount() {
        return budgetAmount;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public boolean isCurrentMonth() {
        YearMonth current = YearMonth.now();
        return monthYear.equals(current.toString());
    }
}