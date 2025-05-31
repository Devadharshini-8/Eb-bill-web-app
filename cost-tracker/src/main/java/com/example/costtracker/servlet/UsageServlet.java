package com.example.costtracker.servlet;

import com.example.costtracker.model.User;
import com.example.costtracker.model.Usage;
import com.example.costtracker.util.CostCalculator;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@WebServlet(urlPatterns = {"/cost-tracker/usage", "/cost-tracker/check-login"})
public class UsageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Log the incoming request for debugging
        System.out.println("POST /cost-tracker/usage received");
        System.out.println("Request URL: " + req.getRequestURL());
        System.out.println("Context path: " + req.getContextPath());
        System.out.println("Servlet path: " + req.getServletPath());
        System.out.println("Path info: " + req.getPathInfo());
        req.getParameterMap().forEach((key, value) -> 
            System.out.println("Parameter: " + key + " = " + String.join(", ", value))
        );

        // Check if user is logged in
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            System.out.println("User not logged in, redirecting to login.html");
            resp.sendRedirect("login.html");
            return;
        }

        // Get the usageHours parameter (matches the form's input name="usageHours")
        String hoursParam = req.getParameter("usageHours");
        System.out.println("Received usageHours: " + hoursParam);

        // Validate usageHours
        if (hoursParam == null || hoursParam.trim().isEmpty()) {
            System.out.println("Validation failed: usageHours is null or empty");
            resp.sendRedirect("tracking.html?error=Please%20enter%20a%20valid%20number%20of%20hours");
            return;
        }

        double hours;
        try {
            hours = Double.parseDouble(hoursParam.trim());
            System.out.println("Parsed usageHours: " + hours);
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException: " + e.getMessage());
            resp.sendRedirect("tracking.html?error=Invalid%20number%20format%20for%20hours");
            return;
        }

        // Validate hours in 0.5 increments
        if (hours % 0.5 != 0) {
            System.out.println("Validation failed: hours not in 0.5 increments");
            resp.sendRedirect("tracking.html?error=Please%20enter%20hours%20in%200.5%20increments");
            return;
        }

        // Additional validation: Ensure hours is positive
        if (hours <= 0) {
            System.out.println("Validation failed: hours must be positive");
            resp.sendRedirect("tracking.html?error=Usage%20hours%20must%20be%20a%20positive%20number");
            return;
        }

        // Create a new Usage object and calculate cost
        Usage usage = new Usage(0, user.getUserId(), hours, 0, LocalDateTime.now());
        usage.calculateCost();
        System.out.println("Usage created: hours=" + usage.getHours() + ", cost=" + usage.getCost());

        // Save to database and check budget
        try (Connection conn = com.example.costtracker.util.DatabaseConnection.getConnection()) {
            // Insert usage data
            String sql = "INSERT INTO usage_data (user_id, hours, cost, timestamp) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, user.getUserId());
                stmt.setDouble(2, usage.getHours());
                stmt.setDouble(3, usage.getCost());
                stmt.setTimestamp(4, java.sql.Timestamp.valueOf(usage.getTimestamp()));
                stmt.executeUpdate();
                System.out.println("Usage data inserted into database");
            }

            // Check budget
            sql = "SELECT budget_amount FROM budgets WHERE user_id = ? AND month_year = ?";
            try (PreparedStatement budgetStmt = conn.prepareStatement(sql)) {
                budgetStmt.setInt(1, user.getUserId());
                budgetStmt.setString(2, LocalDate.now().toString().substring(0, 7));
                ResultSet rs = budgetStmt.executeQuery();
                if (rs.next()) {
                    double budget = rs.getDouble("budget_amount");
                    System.out.println("Budget found: " + budget);
                    sql = "SELECT SUM(cost) as total_cost FROM usage_data WHERE user_id = ? AND MONTH(timestamp) = ? AND YEAR(timestamp) = ?";
                    try (PreparedStatement costStmt = conn.prepareStatement(sql)) {
                        costStmt.setInt(1, user.getUserId());
                        costStmt.setInt(2, LocalDate.now().getMonthValue());
                        costStmt.setInt(3, LocalDate.now().getYear());
                        ResultSet costRs = costStmt.executeQuery();
                        if (costRs.next()) {
                            double totalCost = costRs.getDouble("total_cost");
                            System.out.println("Total cost for month: " + totalCost);
                            CostCalculator.checkBudget(user.getUserId(), totalCost, budget, conn);
                        } else {
                            System.out.println("No usage data found for budget check");
                        }
                    }
                } else {
                    System.out.println("No budget found for user");
                }
            }

            // Redirect to tracking.html on success
            System.out.println("Redirecting to tracking.html after successful submission");
            resp.sendRedirect("tracking.html");
        } catch (SQLException e) {
            System.err.println("SQLException: " + e.getMessage());
            e.printStackTrace();
            resp.sendRedirect("tracking.html?error=Database%20error");
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            resp.sendRedirect("tracking.html?error=An%20unexpected%20error%20occurred");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Log the request details
        String path = req.getServletPath() + (req.getPathInfo() != null ? req.getPathInfo() : "");
        System.out.println("GET request received for path: " + path);
        System.out.println("Context path: " + req.getContextPath());
        System.out.println("Servlet path: " + req.getServletPath());
        System.out.println("Path info: " + req.getPathInfo());

        if (path.equals("/cost-tracker/check-login")) {
            // Handle login check endpoint
            System.out.println("Handling /cost-tracker/check-login endpoint");
            User user = (User) req.getSession().getAttribute("user");
            System.out.println("User in session: " + (user != null ? user.getUserId() : "null"));
            resp.setContentType("application/json");
            Gson gson = new Gson();
            resp.getWriter().write(gson.toJson(new LoginStatus(user != null)));
            System.out.println("Sent login status response: " + (user != null));
            return;
        }

        // Handle /cost-tracker/usage endpoint
        // Check if user is logged in
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            System.out.println("User not logged in, redirecting to login.html");
            resp.sendRedirect("login.html");
            return;
        }

        // Fetch usage data from database
        try (Connection conn = com.example.costtracker.util.DatabaseConnection.getConnection()) {
            String sql = "SELECT usage_id, hours, cost, timestamp FROM usage_data WHERE user_id = ? ORDER BY timestamp DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, user.getUserId());
                ResultSet rs = stmt.executeQuery();
                List<Usage> usageList = new ArrayList<>();
                while (rs.next()) {
                    usageList.add(new Usage(
                            rs.getInt("usage_id"),
                            user.getUserId(),
                            rs.getDouble("hours"),
                            rs.getDouble("cost"),
                            rs.getTimestamp("timestamp").toLocalDateTime()
                    ));
                }
                System.out.println("Fetched " + usageList.size() + " usage records for user " + user.getUserId());
                resp.setContentType("application/json");
                new Gson().toJson(usageList, resp.getWriter());
            }
        } catch (SQLException e) {
            System.err.println("SQLException in doGet: " + e.getMessage());
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        } catch (Exception e) {
            System.err.println("Unexpected error in doGet: " + e.getMessage());
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected error");
        }
    }

    // Simple DTO for login status response
    private static class LoginStatus {
        private boolean loggedIn;

        public LoginStatus(boolean loggedIn) {
            this.loggedIn = loggedIn;
        }

        public boolean isLoggedIn() {
            return loggedIn;
        }
    }
}