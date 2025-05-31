package com.example.costtracker.servlet;

import com.example.costtracker.model.User;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@WebServlet("/payment")
@MultipartConfig
public class PaymentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("PaymentServlet: doGet called for /payment");

        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            System.out.println("PaymentServlet: User not logged in, redirecting to login.html");
            resp.sendRedirect("login.html");
            return;
        }
        System.out.println("PaymentServlet: User authenticated, userId: " + user.getUserId());

        resp.setContentType("application/json");
        List<Payment> payments = new ArrayList<>();
        try (Connection conn = com.example.costtracker.util.DatabaseConnection.getConnection()) {
            System.out.println("PaymentServlet: Database connection established");

            String sql = "SELECT payment_id, total_amount, due_date, is_paid, fine, month_year " +
                        "FROM payments WHERE user_id = ? ORDER BY due_date DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, user.getUserId());
                System.out.println("PaymentServlet: Executing query: " + sql + " with userId: " + user.getUserId());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    payments.add(new Payment(
                            rs.getInt("payment_id"),
                            user.getUserId(),
                            rs.getDouble("total_amount"),
                            rs.getDate("due_date").toLocalDate(),
                            rs.getBoolean("is_paid"),
                            rs.getDouble("fine"),
                            rs.getString("month_year")
                    ));
                }
                System.out.println("PaymentServlet: Fetched " + payments.size() + " payments: " + new Gson().toJson(payments));
            }
        } catch (SQLException e) {
            System.err.println("PaymentServlet: SQLException: " + e.getMessage());
            e.printStackTrace();
            // Fallback to dummy data
            payments.add(new Payment(1, user.getUserId(), 217.00, LocalDate.of(2025, 6, 1), false, 0.00, "2025-05"));
            payments.add(new Payment(2, user.getUserId(), 300.00, LocalDate.of(2025, 5, 1), false, 150.00, "2025-04"));
            System.out.println("PaymentServlet: Falling back to dummy data: " + new Gson().toJson(payments));
        }

        System.out.println("PaymentServlet: Sending response: " + new Gson().toJson(payments));
        new Gson().toJson(payments, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("PaymentServlet: doPost called");
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            System.out.println("PaymentServlet: User not logged in, redirecting to login.html");
            resp.sendRedirect("login.html");
            return;
        }

        // Get the amount from the multipart form data
        Part amountPart = req.getPart("amount");
        String amountParam = null;
        if (amountPart != null) {
            try (Scanner scanner = new Scanner(amountPart.getInputStream())) {
                amountParam = scanner.hasNext() ? scanner.nextLine() : null;
            }
        }
        System.out.println("PaymentServlet: Received amountParam: '" + amountParam + "'");
        if (amountParam == null || amountParam.trim().isEmpty()) {
            System.out.println("PaymentServlet: Invalid amount: null or empty");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Please enter a valid amount\"}");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountParam.trim());
            System.out.println("PaymentServlet: Parsed amount: " + amount);
        } catch (NumberFormatException e) {
            System.out.println("PaymentServlet: Invalid amount format: " + amountParam);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Invalid amount format\"}");
            return;
        }

        try (Connection conn = com.example.costtracker.util.DatabaseConnection.getConnection()) {
            // Fetch the most recent unpaid payment for the user
            String sql = "SELECT total_amount, month_year FROM payments WHERE user_id = ? AND is_paid = FALSE ORDER BY due_date DESC LIMIT 1";
            String monthYear = null;
            double expectedAmount = 0.0;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, user.getUserId());
                System.out.println("PaymentServlet: Checking unpaid bill for userId: " + user.getUserId());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    expectedAmount = rs.getDouble("total_amount");
                    monthYear = rs.getString("month_year");
                    System.out.println("PaymentServlet: Found unpaid bill - monthYear: " + monthYear + ", total_amount: " + expectedAmount);
                } else {
                    System.out.println("PaymentServlet: No unpaid bill found for userId: " + user.getUserId());
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\":\"No unpaid bill found\"}");
                    return;
                }
            }

            // Compare amounts with a small tolerance
            if (Math.abs(expectedAmount - amount) > 0.01) {
                System.out.println("PaymentServlet: Amount mismatch: expected " + expectedAmount + ", received " + amount);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\":\"Amount does not match bill of ₹" + expectedAmount + "\"}");
                return;
            }

            // Update the payment status
            sql = "UPDATE payments SET is_paid = TRUE WHERE user_id = ? AND month_year = ? AND total_amount = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, user.getUserId());
                stmt.setString(2, monthYear);
                stmt.setDouble(3, expectedAmount);
                System.out.println("PaymentServlet: Updating payment for monthYear: " + monthYear + ", amount: " + amount);
                int rows = stmt.executeUpdate();
                System.out.println("PaymentServlet: Rows updated: " + rows);
                if (rows > 0) {
                    System.out.println("PaymentServlet: Payment of ₹" + amount + " marked as paid for user " + user.getUserId());
                    // Insert notification
                    sql = "INSERT INTO notifications (user_id, message, created_at) VALUES (?, ?, NOW())";
                    try (PreparedStatement notifyStmt = conn.prepareStatement(sql)) {
                        notifyStmt.setInt(1, user.getUserId());
                        notifyStmt.setString(2, "Bill of ₹" + amount + " paid successfully.");
                        notifyStmt.executeUpdate();
                        System.out.println("PaymentServlet: Notification inserted for payment of ₹" + amount);
                    }
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("{\"success\":\"Payment of ₹" + amount + " processed\"}");
                } else {
                    System.out.println("PaymentServlet: Failed to update payment: no rows affected");
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\":\"Payment not found or already paid\"}");
                }
            }
        } catch (SQLException e) {
            System.err.println("PaymentServlet: SQLException in doPost: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Database error: " + e.getMessage() + "\"}");
        }
    }

    @SuppressWarnings("unused")
    private static class Payment {
        private int paymentId;
        private int userId;
        private double totalAmount;
        private LocalDate dueDate;
        private boolean isPaid;
        private double fine;
        private String monthYear;

        public Payment(int paymentId, int userId, double totalAmount, LocalDate dueDate, boolean isPaid, double fine, String monthYear) {
            this.paymentId = paymentId;
            this.userId = userId;
            this.totalAmount = totalAmount;
            this.dueDate = dueDate;
            this.isPaid = isPaid;
            this.fine = fine;
            this.monthYear = monthYear;
        }

        public int getPaymentId() { return paymentId; }
        public int getUserId() { return userId; }
        public double getTotalAmount() { return totalAmount; }
        public LocalDate getDueDate() { return dueDate; }
        public boolean isPaid() { return isPaid; }
        public double getFine() { return fine; }
        public String getMonthYear() { return monthYear; }
    }
}