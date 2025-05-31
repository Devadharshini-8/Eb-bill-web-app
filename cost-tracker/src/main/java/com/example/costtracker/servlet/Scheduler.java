package com.example.costtracker.servlet;

import com.example.costtracker.model.Usage;
import com.example.costtracker.util.CostCalculator;
import com.example.costtracker.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Scheduler {
    private static Timer timer = new Timer();

    public static void start() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try (Connection conn = DatabaseConnection.getConnection()) {
                    String sql = "SELECT user_id FROM users";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        ResultSet rs = stmt.executeQuery();
                        while (rs.next()) {
                            int userId = rs.getInt("user_id");
                            double hours = new Random().nextDouble() * 1.0;
                            Usage usage = new Usage(0, userId, hours, 0, LocalDateTime.now());
                            usage.calculateCost();

                            sql = "INSERT INTO usage_data (user_id, hours, cost, timestamp) VALUES (?, ?, ?, ?)";
                            try (PreparedStatement insertStmt = conn.prepareStatement(sql)) {
                                insertStmt.setInt(1, userId);
                                insertStmt.setDouble(2, usage.getHours());
                                insertStmt.setDouble(3, usage.getCost());
                                insertStmt.setTimestamp(4, java.sql.Timestamp.valueOf(usage.getTimestamp()));
                                insertStmt.executeUpdate();

                                sql = "SELECT budget_amount FROM budgets WHERE user_id = ? AND month_year = ?";
                                try (PreparedStatement budgetStmt = conn.prepareStatement(sql)) {
                                    budgetStmt.setInt(1, userId);
                                    budgetStmt.setString(2, LocalDateTime.now().toString().substring(0, 7));
                                    ResultSet budgetRs = budgetStmt.executeQuery();
                                    if (budgetRs.next()) {
                                        double budget = budgetRs.getDouble("budget_amount");
                                        sql = "SELECT SUM(cost) as total_cost FROM usage_data WHERE user_id = ? AND MONTH(timestamp) = ? AND YEAR(timestamp) = ?";
                                        try (PreparedStatement costStmt = conn.prepareStatement(sql)) {
                                            costStmt.setInt(1, userId);
                                            costStmt.setInt(2, LocalDateTime.now().getMonthValue());
                                            costStmt.setInt(3, LocalDateTime.now().getYear());
                                            ResultSet costRs = costStmt.executeQuery();
                                            if (costRs.next()) {
                                                double totalCost = costRs.getDouble("total_cost");
                                                CostCalculator.checkBudget(userId, totalCost, budget, conn);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 30 * 60 * 1000); // Every 30 minutes

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (LocalDateTime.now().getDayOfMonth() == 1) {
                    try (Connection conn = DatabaseConnection.getConnection()) {
                        String sql = "SELECT user_id FROM users";
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            ResultSet rs = stmt.executeQuery();
                            while (rs.next()) {
                                int userId = rs.getInt("user_id");
                                sql = "SELECT SUM(cost) as total_cost FROM usage_data WHERE user_id = ? AND MONTH(timestamp) = ? AND YEAR(timestamp) = ?";
                                try (PreparedStatement costStmt = conn.prepareStatement(sql)) {
                                    costStmt.setInt(1, userId);
                                    costStmt.setInt(2, LocalDateTime.now().getMonthValue() - 1);
                                    costStmt.setInt(3, LocalDateTime.now().getYear());
                                    ResultSet costRs = costStmt.executeQuery();
                                    double totalCost = costRs.next() ? costRs.getDouble("total_cost") : 0.0;

                                    String monthYear = LocalDateTime.now().minusMonths(1).toString().substring(0, 7);
                                    sql = "INSERT INTO payments (user_id, total_amount, due_date, is_paid, fine, month_year) VALUES (?, ?, ?, ?, ?, ?) " +
                                          "ON DUPLICATE KEY UPDATE total_amount = ?, due_date = ?, is_paid = ?, fine = ?";
                                    try (PreparedStatement insertStmt = conn.prepareStatement(sql)) {
                                        insertStmt.setInt(1, userId);
                                        insertStmt.setDouble(2, totalCost);
                                        insertStmt.setDate(3, java.sql.Date.valueOf(LocalDateTime.now().withDayOfMonth(1).plusMonths(1).toLocalDate()));
                                        insertStmt.setBoolean(4, false);
                                        insertStmt.setDouble(5, 0.0);
                                        insertStmt.setString(6, monthYear);
                                        insertStmt.setDouble(7, totalCost);
                                        insertStmt.setDate(8, java.sql.Date.valueOf(LocalDateTime.now().withDayOfMonth(1).plusMonths(1).toLocalDate()));
                                        insertStmt.setBoolean(9, false);
                                        insertStmt.setDouble(10, 0.0);
                                        insertStmt.executeUpdate();
                                    }
                                }
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, 24 * 60 * 60 * 1000); // Check daily
    }
}