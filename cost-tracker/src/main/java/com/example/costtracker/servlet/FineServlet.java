package com.example.costtracker.servlet;

import com.example.costtracker.model.User;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/fine")
public class FineServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("FineServlet: doGet called");

        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            System.out.println("FineServlet: User not logged in, redirecting to login.html");
            resp.sendRedirect("login.html");
            return;
        }
        System.out.println("FineServlet: User authenticated, userId: " + user.getUserId());

        resp.setContentType("application/json");
        List<FineResponse> fines = new ArrayList<>();
        try (Connection conn = com.example.costtracker.util.DatabaseConnection.getConnection()) {
            System.out.println("FineServlet: Database connection established");

            String sql = "SELECT f.fine_id, f.fine_amount, f.is_paid " +
                        "FROM fines f JOIN payments p ON f.payment_id = p.payment_id " +
                        "WHERE p.user_id = ? ORDER BY f.fine_id DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, user.getUserId());
                System.out.println("FineServlet: Executing query for userId: " + user.getUserId());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    fines.add(new FineResponse(
                            rs.getInt("fine_id"),
                            rs.getDouble("fine_amount"),
                            rs.getBoolean("is_paid")
                    ));
                }
                System.out.println("FineServlet: Fetched " + fines.size() + " fines");
            }
        } catch (SQLException e) {
            System.err.println("FineServlet: SQLException: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Database error\"}");
            return;
        }

        System.out.println("FineServlet: Sending response: " + new Gson().toJson(fines));
        new Gson().toJson(fines, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("FineServlet: doPost called");

        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            System.out.println("FineServlet: User not logged in, redirecting to login.html");
            resp.sendRedirect("login.html");
            return;
        }
        System.out.println("FineServlet: User authenticated, userId: " + user.getUserId());

        // Parse JSON request body
        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader reader = req.getReader();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        FinePaymentRequest request = new Gson().fromJson(sb.toString(), FinePaymentRequest.class);
        System.out.println("FineServlet: Received fineId: " + request.getFineId() + ", amount: " + request.getAmount());

        resp.setContentType("application/json");
        try (Connection conn = com.example.costtracker.util.DatabaseConnection.getConnection()) {
            System.out.println("FineServlet: Database connection established");

            // Verify fine exists and belongs to user
            String sql = "SELECT f.fine_id, f.fine_amount, f.is_paid " +
                        "FROM fines f JOIN payments p ON f.payment_id = p.payment_id " +
                        "WHERE f.fine_id = ? AND p.user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, request.getFineId());
                stmt.setInt(2, user.getUserId());
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    System.out.println("FineServlet: Fine not found for fineId: " + request.getFineId());
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\":\"Fine not found\"}");
                    return;
                }

                double expectedAmount = rs.getDouble("fine_amount");
                boolean isPaid = rs.getBoolean("is_paid");
                if (isPaid) {
                    System.out.println("FineServlet: Fine already paid for fineId: " + request.getFineId());
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\":\"Fine already paid\"}");
                    return;
                }
                if (Math.abs(expectedAmount - request.getAmount()) > 0.01) {
                    System.out.println("FineServlet: Amount mismatch: expected " + expectedAmount + ", received " + request.getAmount());
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().write("{\"error\":\"Amount does not match fine of ₹" + expectedAmount + "\"}");
                    return;
                }
            }

            // Update fine status
            String updateSql = "UPDATE fines SET is_paid = TRUE WHERE fine_id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, request.getFineId());
                int rows = updateStmt.executeUpdate();
                System.out.println("FineServlet: Rows updated: " + rows);
                if (rows > 0) {
                    // Insert notification
                    String notifySql = "INSERT INTO notifications (user_id, message, created_at) VALUES (?, ?, NOW())";
                    try (PreparedStatement notifyStmt = conn.prepareStatement(notifySql)) {
                        notifyStmt.setInt(1, user.getUserId());
                        notifyStmt.setString(2, "Fine payment of ₹" + request.getAmount() + " processed for April 2025.");
                        notifyStmt.executeUpdate();
                        System.out.println("FineServlet: Notification inserted for fine payment");
                    }
                    resp.setStatus(HttpServletResponse.SC_OK);
                    resp.getWriter().write("{\"success\":\"Fine payment of ₹" + request.getAmount() + " processed\"}");
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().write("{\"error\":\"Fine not found or already paid\"}");
                }
            }
        } catch (SQLException e) {
            System.err.println("FineServlet: SQLException: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Database error\"}");
        }
    }

    @SuppressWarnings("unused")
    private static class FineResponse {
        int fineId;
        double fineAmount;
        boolean isPaid;

        FineResponse(int fineId, double fineAmount, boolean isPaid) {
            this.fineId = fineId;
            this.fineAmount = fineAmount;
            this.isPaid = isPaid;
        }
    }

    private static class FinePaymentRequest {
        private int fineId;
        private double amount;

        public int getFineId() { return fineId; }
        public double getAmount() { return amount; }
    }
}