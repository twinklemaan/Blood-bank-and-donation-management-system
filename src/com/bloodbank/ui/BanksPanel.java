package com.bloodbank.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * Tab panel for managing Blood Banks (Admin).
 */
public class BanksPanel extends BasePanel {
    public BanksPanel() {
        add(createHeaderPanel("Blood Banks Management", "View, register, and update blood bank organizations"), BorderLayout.NORTH);
        JLabel placeholder = new JLabel("Blood Banks module initializing...", SwingConstants.CENTER);
        placeholder.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        add(placeholder, BorderLayout.CENTER);
    }
}
