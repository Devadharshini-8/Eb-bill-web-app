package com.example.costtracker.servlet;

import com.example.costtracker.model.User;
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
import java.util.ArrayList;
import java.util.List;

@WebServlet("/budget")
public class BudgetServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("BudgetServlet: doGet called");

        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            System.out.println("BudgetServlet: User not logged in, redirecting to login.html");
            resp.sendRedirect("login.html");
            return;
        }
        System.out.println("BudgetServlet: User authenticated, userId: " + user.getUserId());

        resp.setContentType("application/json");
        List<BudgetResponse> budgets = new ArrayList<>();
        try (Connection conn = com.example.costtracker.util.DatabaseConnection.getConnection()) {
            System.out.println("BudgetServlet: Database connection established");

            String sql = "SELECT budget_amount, month_year FROM budgets WHERE user_id = ? ORDER BY month_year DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, user.getUserId());
                System.out.println("BudgetServlet: Executing query for userId: " + user.getUserId());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    budgets.add(new BudgetResponse(
                            rs.getDouble("budget_amount"),
                            rs.getString("month_year")
                    ));
                }
                System.out.println("BudgetServlet: Fetched " + budgets.size() + " budgets");
            }
        } catch (SQLException e) {
            System.err.println("BudgetServlet: SQLException: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\":\"Database error\"}");
            return;
        }

        System.out.println("BudgetServlet: Sending response: " + new Gson().toJson(budgets));
        new Gson().toJson(budgets, resp.getWriter());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("BudgetServlet: doPost called");

        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            System.out.println("BudgetServlet: User not logged in, redirecting to login.html");
            resp.sendRedirect("login.html");
            return;
        }
        System.out.println("BudgetServlet: User authenticated, userId: " + user.getUserId());

        String monthYear = req.getParameter("month_year");
        String budgetAmountStr = req.getParameter("budget");
        System.out.println("BudgetServlet: Received month_year: " + monthYear + ", budget: " + budgetAmountStr);

        if (monthYear == null || monthYear.trim().isEmpty()) {
            System.out.println("BudgetServlet: Invalid month_year: null or empty");
            resp.sendRedirect("budget.html?error=Please select a valid month");
            return;
        }

        double budgetAmount;
        try {
            budgetAmount = Double.parseDouble(budgetAmountStr);
            if (budgetAmount <= 0) {
                throw new NumberFormatException("Budget amount must be positive");
            }
            System.out.println("BudgetServlet: Parsed budgetAmount: " + budgetAmount);
        } catch (NumberFormatException e) {
            System.err.println("BudgetServlet: Invalid budget amount: " + budgetAmountStr);
            resp.sendRedirect("budget.html?error=Please enter a valid positive budget amount");
            return;
        }

        try (Connection conn = com.example.costtracker.util.DatabaseConnection.getConnection()) {
            System.out.println("BudgetServlet: Database connection established");

            String checkSql = "SELECT budget_id FROM budgets WHERE user_id = ? AND month_year = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, user.getUserId());
                checkStmt.setString(2, monthYear);
                if (checkStmt.executeQuery().next()) {
                    String updateSql = "UPDATE budgets SET budget_amount = ? WHERE user_id = ? AND month_year = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setDouble(1, budgetAmount);
                        updateStmt.setInt(2, user.getUserId());
                        updateStmt.setString(3, monthYear);
                        int rows = updateStmt.executeUpdate();
                        System.out.println("BudgetServlet: Updated budget, rows affected: " + rows);
                    }
                } else {
                    String insertSql = "INSERT INTO budgets (user_id, budget_amount, month_year) VALUES (?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, user.getUserId());
                        insertStmt.setDouble(2, budgetAmount);
                        insertStmt.setString(3, monthYear);
                        int rows = insertStmt.executeUpdate();
                        System.out.println("BudgetServlet: Inserted new budget, rows affected: " + rows);
                    }
                }
            }

            System.out.println("BudgetServlet: Budget set successfully for " + monthYear);
            resp.sendRedirect("budget.html?success=Budget set successfully for " + monthYear);
        } catch (SQLException e) {
            System.err.println("BudgetServlet: SQLException: " + e.getMessage());
            e.printStackTrace();
            resp.sendRedirect("budget.html?error=Database error");
        }
    }

    private static class BudgetResponse {
        double budgetAmount;
        String monthYear;

        BudgetResponse(double budgetAmount, String monthYear) {
            this.budgetAmount = budgetAmount;
            this.monthYear = monthYear;
        }
    }
}