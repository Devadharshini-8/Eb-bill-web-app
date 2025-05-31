package com.example.costtracker.servlet;

import com.example.costtracker.model.User;
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

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
	private static final long serialVersionUID = 1L; // Added for serializability
    private static boolean schedulerStarted = false;

    @Override
    public void init() throws ServletException {
        if (!schedulerStarted) {
            Scheduler.start();
            schedulerStarted = true;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        try (Connection conn = com.example.costtracker.util.DatabaseConnection.getConnection()) {
            String sql = "SELECT user_id, username, password FROM users WHERE username = ? AND password = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    User user = new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("password"));
                    req.getSession().setAttribute("user", user);
                    resp.sendRedirect("budget.html");
                } else {
                    resp.sendRedirect("login.html?error=Invalid username or password");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendRedirect("login.html?error=Database error");
        }
    }
}