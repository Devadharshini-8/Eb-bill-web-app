package com.example.costtracker.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CostCalculator {
    public static double calculateCost(double hours) {
        return hours * 14; // 1 hour = 14 rupees (0.5 hours = 7 rupees)
    }

    public static void checkBudget(int userId, double totalCost, double budget, Connection conn) throws SQLException {
        if (budget > 0) {
            if (totalCost >= budget) {
                notifyOverdue(userId, "You have exceeded your budget of ₹" + budget + "!", conn);
            } else if (totalCost >= budget * 0.9) {
                notifyOverdue(userId, "You are nearing your budget of ₹" + budget + "!", conn);
            }
        }
    }

    public static void notifyOverdue(int userId, String message, Connection conn) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, message, created_at) VALUES (?, ?, NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, message);
            stmt.executeUpdate();
        }
    }

    public static void notifyPayment(int userId, String message, Connection conn) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, message, created_at) VALUES (?, ?, NOW())";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, message);
            stmt.executeUpdate();
        }
    }
}