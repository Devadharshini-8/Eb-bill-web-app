package com.example.costtracker.servlet;

import com.example.costtracker.model.Notification;
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

@WebServlet("/notifications")
public class NotificationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L; // Added for serializability
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");
        if (user == null) {
            resp.sendRedirect("login.html");
            return;
        }

        try (Connection conn = com.example.costtracker.util.DatabaseConnection.getConnection()) {
            String sql = "SELECT notification_id, message, created_at FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, user.getUserId());
                ResultSet rs = stmt.executeQuery();
                List<Notification> notifications = new ArrayList<>();
                while (rs.next()) {
                    notifications.add(new Notification(
                            rs.getInt("notification_id"),
                            user.getUserId(),
                            rs.getString("message"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
                resp.setContentType("application/json");
                new Gson().toJson(notifications, resp.getWriter());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }
}