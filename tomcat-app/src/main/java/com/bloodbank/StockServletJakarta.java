package com.bloodbank;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * StockServlet - Tomcat 10 / Jakarta EE version
 */
@WebServlet(name = "StockServletJakarta", urlPatterns = {"/stock"})
public class StockServletJakarta extends HttpServlet {

    public static class StockItem {
        private String bloodGroup;
        private int units;

        public String getBloodGroup() { return bloodGroup; }
        public int getUnits() { return units; }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        String apiBase = System.getenv("API_BASE");
        if (apiBase == null || apiBase.trim().isEmpty()) {
            apiBase = System.getProperty("api.base", "http://localhost:5000");
        }
        String endpoint = apiBase + "/api/public/stock";

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Tomcat Blood Stock Portal</title>");
        out.println("<style>");
        out.println("body { font-family: -apple-system, Segoe UI, Roboto, sans-serif; background-color: #f7fafc; padding: 2rem; color: #2d3748; }");
        out.println(".container { max-width: 600px; margin: 0 auto; background: #fff; padding: 1.5rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }");
        out.println("h2 { color: #c53030; margin-bottom: 0.5rem; }");
        out.println("p.subtitle { color: #718096; font-size: 0.9rem; margin-bottom: 1.25rem; }");
        out.println("table { width: 100%; border-collapse: collapse; margin-top: 1rem; }");
        out.println("th, td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #e2e8f0; }");
        out.println("th { background-color: #edf2f7; color: #4a5568; }");
        out.println(".critical { color: #e53e3e; font-weight: bold; }");
        out.println(".normal { color: #2f855a; font-weight: bold; }");
        out.println(".error-box { background-color: #fff5f5; border: 1px solid #feb2b2; color: #c53030; padding: 1rem; border-radius: 4px; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<div class='container'>");
        out.println("<h2>🩸 Apache Tomcat Blood Inventory</h2>");
        out.println("<p class='subtitle'>Served by Java Servlet via Node.js REST API (<code>" + endpoint + "</code>)</p>");

        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("HTTP error code: " + responseCode);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();
            conn.disconnect();

            Gson gson = new Gson();
            Type listType = new TypeToken<List<StockItem>>() {}.getType();
            List<StockItem> stockList = gson.fromJson(jsonBuilder.toString(), listType);

            out.println("<table>");
            out.println("<thead><tr><th>Blood Group</th><th>Available Units</th><th>Status</th></tr></thead>");
            out.println("<tbody>");
            for (StockItem item : stockList) {
                boolean isLow = item.getUnits() <= 5;
                out.println("<tr>");
                out.println("<td><strong>" + item.getBloodGroup() + "</strong></td>");
                out.println("<td>" + item.getUnits() + " units</td>");
                out.println("<td class='" + (isLow ? "critical" : "normal") + "'>" + (isLow ? "Low Stock (≤5)" : "Adequate") + "</td>");
                out.println("</tr>");
            }
            out.println("</tbody>");
            out.println("</table>");

        } catch (Exception e) {
            out.println("<div class='error-box'>");
            out.println("<strong>API not reachable:</strong> " + e.getMessage() + "<br/>");
            out.println("<small>Please ensure the backend API server is running on <code>" + apiBase + "</code></small>");
            out.println("</div>");
        }

        out.println("<p style='margin-top: 1.5rem; text-align: center; font-size: 0.8rem; color: #a0aec0;'>Apache Tomcat 10.1 • Jakarta Servlet 6.0 • Gson 2.14</p>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
    }
}
